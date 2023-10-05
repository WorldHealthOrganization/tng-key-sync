package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import int_.who.tng.dataimport.job.ImportJobStepException;
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
    public void exec(ImportJobContext context, String... args) throws ImportJobStepException {
        String base64EncodedCert = args[0];
        ImportJobContext.CertificateType certificateType = ImportJobContext.CertificateType.valueOf(args[1]);

        try {
            X509CertificateHolder x509CertificateHolder =
                new X509CertificateHolder(Base64.getDecoder().decode(base64EncodedCert));
            RDN[] countryRdns = x509CertificateHolder.getSubject().getRDNs(BCStyle.C);

            if (countryRdns.length != 1) {
                throw new ImportJobStepException(false,
                    "No Country Attribute in Cert with Subject " + x509CertificateHolder.getSubject() +
                        ". Skipping Certificate");
            }

            String countryCode = IETFUtils.valueToString(countryRdns[0].getFirst().getValue());

            X509Certificate x509Certificate = certificateUtils.convertCertificate(x509CertificateHolder);

            log.debug("Adding Certificate ({}, {}) to Context.", x509Certificate.getSubjectX500Principal().toString(),
                certificateType);

            context.getParsedCertificates().add(new ImportJobContext.CertificateEntry(
                x509CertificateHolder,
                x509Certificate,
                null,
                x509CertificateHolder.getEncoded(),
                certificateUtils.getCertThumbprint(x509CertificateHolder),
                null,
                countryCode,
                certificateType, null));//TODO: domain null?

        } catch (Exception e) {
            throw new ImportJobStepException(true,
                "Unexpected Exception during adding Certificate to Context: " + e.getMessage());
        }
    }
}
