package int_.who.tng.dataimport.job;

import int_.who.tng.dataimport.config.DccConfigProperties;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RemoveIgnoredCountriesStep {

    private final DccConfigProperties dccConfigProperties;

    public List<ExtractArchiveStep.ArchivePemEntry> exec(List<ExtractArchiveStep.ArchivePemEntry> pemEntries) {
        log.info("Removing PEMs of ignored countries");

        List<ExtractArchiveStep.ArchivePemEntry> filteredPemEntries = pemEntries.stream()
            .filter(this::isCountryNotOnIgnoreList)
            .collect(Collectors.toList());

        log.info("Finished filtering PEMs for ignored countries. {} of {} entries were removed.",
            pemEntries.size() - filteredPemEntries.size(), pemEntries.size());

        return filteredPemEntries;
    }

    private boolean isCountryNotOnIgnoreList(ExtractArchiveStep.ArchivePemEntry pemEntry) {
        return !dccConfigProperties.getIgnoreCountries().contains(pemEntry.country());
    }
}
