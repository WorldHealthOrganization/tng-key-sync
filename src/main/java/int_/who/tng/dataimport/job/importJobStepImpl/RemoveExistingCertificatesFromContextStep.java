package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import int_.who.tng.dataimport.repository.SignerInformationRepository;
import int_.who.tng.dataimport.repository.TrustedPartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("RemoveExistingCertificatesFromContext")
@RequiredArgsConstructor
@Slf4j
public class RemoveExistingCertificatesFromContextStep implements ImportJobStep {

    private final SignerInformationRepository signerInformationRepository;

    private final TrustedPartyRepository trustedPartyRepository;

    @Override
    public void exec(ImportJobContext context, String... args) {
        ImportJobContext.CertificateType certificateType = ImportJobContext.CertificateType.valueOf(args[0]);

        log.debug("Removing Existing Certificates of type {}", certificateType);

        int preProcessSize = context.getParsedCertificates().size();

        if (certificateType == ImportJobContext.CertificateType.DSC) {
            context.getParsedCertificates()
                .removeIf(certificateEntry -> signerInformationRepository.getFirstByThumbprint(
                    certificateEntry.getThumbprint()).isPresent());
        } else { // CSCA, UPLOAD or AUTH
            context.getParsedCertificates()
                .removeIf(
                    certificateEntry -> trustedPartyRepository.getFirstByThumbprint(certificateEntry.getThumbprint())
                        .isPresent());
        }

        log.debug("Finished filtering for existing Certificates. {} of {} entries were removed.",
            preProcessSize - context.getParsedCertificates().size(), preProcessSize);
    }
}
