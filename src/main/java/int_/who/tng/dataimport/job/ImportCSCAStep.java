package int_.who.tng.dataimport.job;

import eu.europa.ec.dgc.signing.SignedCertificateMessageBuilder;
import int_.who.tng.dataimport.entity.TrustedPartyEntity;
import int_.who.tng.dataimport.repository.TrustedPartyRepository;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportCSCAStep {

    @Qualifier("trustAnchor")
    private final PrivateKey trustAnchorPrivateKey;

    @Qualifier("trustAnchor")
    private final X509CertificateHolder trustAnchorCertificate;

    private final TrustedPartyRepository trustedPartyRepository;

    public void exec(List<ParseCertificatesStep.ArchiveCertificateEntry> certificateList) {
        log.info("Importing CSCA into Database");

        certificateList.stream()
            .filter(this::isCsca)
            .filter(this::doesNotAlreadyExists)
            .map(this::sign)
            .forEach(this::insert);
    }

    private void insert(SignedCscaCertificate signedCscaCertificate) {
        try {
            String certRawData = Base64.getEncoder().encodeToString(
                signedCscaCertificate.certificateEntry.certificate().getEncoded());

            TrustedPartyEntity trustedPartyEntity = new TrustedPartyEntity();
            trustedPartyEntity.setCountry(signedCscaCertificate.certificateEntry.country());
            trustedPartyEntity.setCertificateType(TrustedPartyEntity.CertificateType.CSCA);
            trustedPartyEntity.setThumbprint(signedCscaCertificate.certificateEntry.thumbprint());
            trustedPartyEntity.setRawData(certRawData);
            trustedPartyEntity.setSignature(signedCscaCertificate.trustAnchorSignature);

            trustedPartyRepository.save(trustedPartyEntity);

            log.info("Inserted CSCA with thumbprint {} for country {}",
                signedCscaCertificate.certificateEntry.thumbprint(), signedCscaCertificate.certificateEntry.country());

        } catch (IOException e) {
            log.error("Failed to convert Certificate");
        }
    }

    private SignedCscaCertificate sign(ParseCertificatesStep.ArchiveCertificateEntry certificateEntry) {
        log.info("Signing CSCA with thumbprint {} for country {} with TrustAnchor",
            certificateEntry.thumbprint(), certificateEntry.country());

        byte[] signature = new SignedCertificateMessageBuilder()
            .withSigningCertificate(trustAnchorCertificate, trustAnchorPrivateKey)
            .withPayload(certificateEntry.certificate())
            .build(true);

        return new SignedCscaCertificate(
            certificateEntry,
            Base64.getEncoder().encodeToString(signature));
    }

    private boolean doesNotAlreadyExists(ParseCertificatesStep.ArchiveCertificateEntry certificateEntry) {
        if (trustedPartyRepository.getFirstByThumbprintAndCertificateType(
            certificateEntry.thumbprint(), TrustedPartyEntity.CertificateType.CSCA).isPresent()) {
            log.info("CSCA with thumbprint {} for country {} already exists",
                certificateEntry.thumbprint(), certificateEntry.country());

            return false;
        } else {
            return true;
        }
    }

    /**
     * Filter Certificates to get only CSCA
     */
    private boolean isCsca(ParseCertificatesStep.ArchiveCertificateEntry certificateEntry) {
        return certificateEntry.type() == ParseCertificatesStep.ArchiveCertificateEntry.ArchiveEntryType.CSCA;
    }

    private record SignedCscaCertificate(
        ParseCertificatesStep.ArchiveCertificateEntry certificateEntry,
        String trustAnchorSignature
    ) {
    }

}
