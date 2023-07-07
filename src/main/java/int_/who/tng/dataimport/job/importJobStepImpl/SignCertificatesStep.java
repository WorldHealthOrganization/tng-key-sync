package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.signing.SignedCertificateMessageBuilder;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("SignCertificates")
@RequiredArgsConstructor
@Slf4j
public class SignCertificatesStep implements ImportJobStep {

    @Override
    public void exec(ImportJobContext context, String... args) {
        final ImportJobContext.CertificateType signerCertType = ImportJobContext.CertificateType.valueOf(args[0]);
        final ImportJobContext.CertificateType toBeSignedCertType = ImportJobContext.CertificateType.valueOf(args[1]);

        log.debug("Signing Certificates of Type {} with Certificates of Type {}", toBeSignedCertType, signerCertType);

        // Find Certificates with matching Type and without Signature
        List<ImportJobContext.CertificateEntry> toBeSignedCertificates = context.getParsedCertificates().stream()
            .filter(certificateEntry -> certificateEntry.getCertificateType() == toBeSignedCertType)
            .filter(certificateEntry -> certificateEntry.getSignature() == null)
            .toList();

        // Find Certificates with PrivateKey and matching Type, Group by Country
        Map<String, ImportJobContext.CertificateEntry> signingCertificates = context.getParsedCertificates().stream()
            .filter(certificateEntry -> certificateEntry.getCertificateType() == signerCertType)
            .filter(certificateEntry -> certificateEntry.getPrivateKey() != null)
            .collect(Collectors.toMap(
                ImportJobContext.CertificateEntry::getCountry,
                certificateEntry -> certificateEntry,
                (t, t2) -> t));

        // Sign all found Certificates
        for (ImportJobContext.CertificateEntry toBeSignedCertificate : toBeSignedCertificates) {
            ImportJobContext.CertificateEntry signerCertificate =
                signingCertificates.get(toBeSignedCertificate.getCountry());

            if (signerCertificate == null) {
                log.warn("No Signer Certificate for Country {}", toBeSignedCertificate.getCountry());
                continue;
            }

            String signature = new SignedCertificateMessageBuilder()
                .withSigningCertificate(signerCertificate.getParsedCertificateHolder(),
                    signerCertificate.getPrivateKey())
                .withPayload(toBeSignedCertificate.getParsedCertificateHolder())
                .buildAsString(true);

            toBeSignedCertificate.setSignature(signature);
            log.debug("Signed Certificate. ToBeSigned: {} {} - {}, Signer: {} {} - {}",
                toBeSignedCertificate.getCertificateType(), toBeSignedCertificate.getCountry(),
                toBeSignedCertificate.getThumbprint(),
                signerCertificate.getCertificateType(), signerCertificate.getCountry(),
                toBeSignedCertificate.getThumbprint());
        }

        log.debug("Finished signing Certificates of Type {} with Certificates of Type {}", toBeSignedCertType,
            signerCertType);
    }
}
