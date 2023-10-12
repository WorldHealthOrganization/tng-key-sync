package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.entity.TrustedIssuerEntity;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import int_.who.tng.dataimport.repository.TrustedIssuerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("SaveTrustedIssuersInDb")
@RequiredArgsConstructor
@Slf4j
public class SaveTrustedIssuersInDbStep implements ImportJobStep {

    private final TrustedIssuerRepository trustedIssuerRepository;

    @Override
    public void exec(ImportJobContext context, String... args) {

        log.debug("Persisting Trusted Issuers in Database");

        context.getParsedIssuers().stream()
            .filter(this::checkTrustedIssuerEntry)
            .forEach(this::persistTrustedIssuer);
    }

    private boolean checkTrustedIssuerEntry(ImportJobContext.TrustedIssuerEntry trustedIssuerEntry) {

        if (trustedIssuerEntry.getSignature() == null) {
            log.warn("Trusted issuer with hash {} has no signature. Skipping persistence.",
                    trustedIssuerEntry.getHash());
            return false;
        }

        if (trustedIssuerEntry.getCountry() == null || trustedIssuerEntry.getCountry().length() != 2) {
            log.warn("Trusted issuer with hash {} has no valid country code. Skipping persistence.",
                    trustedIssuerEntry.getHash());
            return false;
        }

        return true;
    }

    private void persistTrustedIssuer(ImportJobContext.TrustedIssuerEntry trustedIssuerEntry) {
        TrustedIssuerEntity trustedIssuerEntity = new TrustedIssuerEntity();

        trustedIssuerEntity.setName(trustedIssuerEntry.getName());
        trustedIssuerEntity.setUrl(trustedIssuerEntry.getUrl());
        trustedIssuerEntity.setUrlType(TrustedIssuerEntity.UrlType.valueOf(trustedIssuerEntry.getUrlType()));
        trustedIssuerEntity.setThumbprint(trustedIssuerEntry.getHash());
        trustedIssuerEntity.setSslPublicKey(trustedIssuerEntry.getSslPublicKeys().get(0));//TODO: support multiple keys, requires TNG DB schema change
        trustedIssuerEntity.setSignature(trustedIssuerEntry.getSignature());
        trustedIssuerEntity.setCountry(trustedIssuerEntry.getCountry());
        trustedIssuerEntity.setDomain(trustedIssuerEntry.getDomain());

        trustedIssuerRepository.save(trustedIssuerEntity);

        log.debug("Inserted trustedIssuer with thumbprint {} for country {}",
            trustedIssuerEntity.getThumbprint(), trustedIssuerEntity.getCountry());

    }

}
