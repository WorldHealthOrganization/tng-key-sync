package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import int_.who.tng.dataimport.repository.TrustedIssuerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("RemoveExistingIssuersFromContext")
@RequiredArgsConstructor
@Slf4j
public class RemoveExistingIssuersFromContextStep implements ImportJobStep {

    private final TrustedIssuerRepository trustedIssuerRepository;

    @Override
    public void exec(ImportJobContext context, String... args) {

        log.debug("Removing existing trusted issuer from context");

        int preProcessSize = context.getParsedIssuers().size();

        context.getParsedIssuers()
            .removeIf(
                trustedIssuerEntry -> trustedIssuerRepository.getFirstByThumbprint(trustedIssuerEntry.getHash())
                    .isPresent());

        log.debug("Finished filtering trusted issuers. {} of {} entries were removed.",
            preProcessSize - context.getParsedIssuers().size(), preProcessSize);
    }
}
