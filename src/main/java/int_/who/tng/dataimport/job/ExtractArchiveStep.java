package int_.who.tng.dataimport.job;

import int_.who.tng.dataimport.config.DccConfigProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractArchiveStep {

    private final Pattern dscFilenamePattern = Pattern.compile("^DSC/DCC/(..)/([0-9a-f]{64})\\.pem$");
    private final Pattern cscaFilenamePattern = Pattern.compile("^CSCA/DCC/(..)/([0-9a-f]{64})\\.pem$");

    public List<ArchivePemEntry> exec(byte[] archive) {
        List<ArchivePemEntry> archiveEntries = new ArrayList<>();

        try {
            log.info("Extracting ZIP Archive");
            ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(archive));

            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }

                String filename = zipEntry.getName();
                Matcher dscFilenameMatcher = dscFilenamePattern.matcher(filename);
                Matcher cscaFilenameMatcher = cscaFilenamePattern.matcher(filename);

                if (dscFilenameMatcher.find()) {
                    archiveEntries.add(new ArchivePemEntry(
                        dscFilenameMatcher.group(1),
                        new String(zipInputStream.readAllBytes()),
                        dscFilenameMatcher.group(2),
                        ArchivePemEntry.ArchiveEntryType.DSC
                    ));
                } else if (cscaFilenameMatcher.find()) {
                    archiveEntries.add(new ArchivePemEntry(
                        cscaFilenameMatcher.group(1),
                        new String(zipInputStream.readAllBytes()),
                        cscaFilenameMatcher.group(2),
                        ArchivePemEntry.ArchiveEntryType.CSCA
                    ));
                }
            }
        } catch (IOException e) {
            log.error("Extraction of ZIP Archive failed.", e);
            System.exit(1);
        }

        log.info("Found {} certificates in ZIP Archive", archiveEntries.size());
        return archiveEntries;
    }

    public record ArchivePemEntry(
        String country,
        String pem,
        String thumbprint,
        ArchiveEntryType type) {

        enum ArchiveEntryType {
            DSC,
            CSCA
        }
    }
}
