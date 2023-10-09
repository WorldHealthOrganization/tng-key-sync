package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.entity.SignerInformationEntity;
import int_.who.tng.dataimport.entity.TrustedPartyEntity;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import int_.who.tng.dataimport.repository.SignerInformationRepository;
import int_.who.tng.dataimport.repository.TrustedPartyRepository;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("SaveCertificatesInDb")
@RequiredArgsConstructor
@Slf4j
public class SaveCertificatesInDbStep implements ImportJobStep {

    private final TrustedPartyRepository trustedPartyRepository;

    private final SignerInformationRepository signerInformationRepository;

    @Override
    public void exec(ImportJobContext context, String... args) {
        ImportJobContext.CertificateType certificateType = ImportJobContext.CertificateType.valueOf(args[0]);

        log.debug("Persisting {} Certificates in Database", certificateType);

        context.getParsedCertificates().stream()
            .filter(certificate -> certificate.getCertificateType() == certificateType)
            .filter(this::checkCertificateEntry)
            .forEach(certificateType == ImportJobContext.CertificateType.DSC
                ? this::persistSignerInformation
                : this::persistTrustedParty);
    }

    private boolean checkCertificateEntry(ImportJobContext.CertificateEntry certificateEntry) {
        if (certificateEntry.getSignature() == null) {
            log.warn("Certificate with thumbprint {} has no signature. Skipping Certificate.",
                certificateEntry.getThumbprint());
            return false;
        }

        if (certificateEntry.getCountry() == null) {
            log.warn("Certificate with thumbprint {} has no valid country code. Skipping Certificate.",
                certificateEntry.getThumbprint());
            return false;
        }

        return true;
    }

    private void persistTrustedParty(ImportJobContext.CertificateEntry certificateEntry) {
        TrustedPartyEntity trustedPartyEntity = new TrustedPartyEntity();
        trustedPartyEntity.setCountry(certificateEntry.getCountry());
        trustedPartyEntity.setCertificateType(
            TrustedPartyEntity.CertificateType.valueOf(certificateEntry.getCertificateType().toString()));
        trustedPartyEntity.setThumbprint(certificateEntry.getThumbprint());
        trustedPartyEntity.setRawData(Base64.getEncoder().encodeToString(certificateEntry.getRawCertificate()));
        trustedPartyEntity.setSignature(certificateEntry.getSignature());
        if (certificateEntry.getDomain() != null) {
            trustedPartyEntity.setDomain(certificateEntry.getDomain());
        }
        trustedPartyRepository.save(trustedPartyEntity);

        log.debug("Inserted TrustedParty with thumbprint {} for country {}",
            certificateEntry.getThumbprint(), certificateEntry.getCountry());
    }

    private void persistSignerInformation(ImportJobContext.CertificateEntry certificateEntry) {
        SignerInformationEntity signerInformationEntity = new SignerInformationEntity();
        signerInformationEntity.setCountry(certificateEntry.getCountry());
        signerInformationEntity.setCertificateType(
            SignerInformationEntity.CertificateType.valueOf(certificateEntry.getCertificateType().toString()));
        signerInformationEntity.setThumbprint(certificateEntry.getThumbprint());
        signerInformationEntity.setRawData(Base64.getEncoder().encodeToString(certificateEntry.getRawCertificate()));
        signerInformationEntity.setSignature(certificateEntry.getSignature());
        if (certificateEntry.getDomain() != null){
            signerInformationEntity.setDomain(certificateEntry.getDomain());
        }

        signerInformationRepository.save(signerInformationEntity);

        log.debug("Inserted SignerInformation with thumbprint {} for country {}",
            certificateEntry.getThumbprint(), certificateEntry.getCountry());
    }
}
