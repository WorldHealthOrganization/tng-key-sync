package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("RemoveIgnoredCountries")
@RequiredArgsConstructor
@Slf4j
public class RemoveIgnoredCountriesStep implements ImportJobStep {

    @Override
    public void exec(ImportJobContext context, String... args) {
        List<String> ignoredCountries = Arrays.asList(args);

        log.info("Removing Certificates of ignored countries: {}", ignoredCountries);

        int preProcessSize = context.getParsedCertificates().size();

        context.getParsedCertificates()
            .removeIf(certificateEntry -> ignoredCountries.contains(certificateEntry.getCountry()));

        log.info("Finished filtering Certificates for ignored countries. {} of {} entries were removed.",
            preProcessSize - context.getParsedCertificates().size(), preProcessSize);
    }
}
