package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import java.security.cert.X509Certificate;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.stereotype.Service;

@Service("AddCertificate")
@RequiredArgsConstructor
@Slf4j
public class AddCertificateStep implements ImportJobStep {

    private final CertificateUtils certificateUtils;

    @Override
    public void exec(ImportJobContext context, String... args) {
        String base64EncodedCert = args[0];
        ImportJobContext.CertificateType certificateType = ImportJobContext.CertificateType.valueOf(args[1]);

        log.info("Adding Certificate to Context");

        try {

            X509CertificateHolder x509CertificateHolder =
                new X509CertificateHolder(Base64.getDecoder().decode(base64EncodedCert));
            RDN[] countryRdns = x509CertificateHolder.getSubject().getRDNs(BCStyle.C);

            if (countryRdns.length != 1) {
                log.error("No Country Attribute in Cert with Subject {}. Skipping Certificate",
                    x509CertificateHolder.getSubject());
                return;
            }

            String countryCode = IETFUtils.valueToString(countryRdns[0].getFirst().getValue());

            X509Certificate x509Certificate = certificateUtils.convertCertificate(x509CertificateHolder);

            context.getParsedCertificates().add(new ImportJobContext.CertificateEntry(
                x509CertificateHolder,
                x509Certificate,
                null,
                x509CertificateHolder.getEncoded(),
                certificateUtils.getCertThumbprint(x509CertificateHolder),
                null,
                countryCode,
                certificateType));

        } catch (Exception e) {
            log.error("Failed to add Certificate to Context", e);
            return;
        }

        log.info("Certificate Added to Context");
    }
}
