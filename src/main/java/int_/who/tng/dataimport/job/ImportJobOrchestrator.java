package int_.who.tng.dataimport.job;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImportJobOrchestrator {

    private final DownloadArchiveStep downloadArchiveStep;

    private final ExtractArchiveStep extractArchiveStep;

    private final ParseCertificatesStep parseCertificatesStep;

    private final ImportCSCAStep importCSCAStep;

    private final ImportDSCStep importDSCStep;

    private final RemoveIgnoredCountriesStep removeIgnoredCountriesStep;

    @EventListener(ApplicationReadyEvent.class)
    public void exec() {
        log.info("Starting DCC Key Import Job");

        byte[] archive = downloadArchiveStep.exec();
        List<ExtractArchiveStep.ArchivePemEntry> pemEntries = extractArchiveStep.exec(archive);
        List<ExtractArchiveStep.ArchivePemEntry> filteredPemEntries = removeIgnoredCountriesStep.exec(pemEntries);
        List<ParseCertificatesStep.ArchiveCertificateEntry> certificateEntries = parseCertificatesStep.exec(filteredPemEntries);
        importCSCAStep.exec(certificateEntries);
        importDSCStep.exec(certificateEntries);

        log.info("Finished DCC Key Import Job");
    }

}
