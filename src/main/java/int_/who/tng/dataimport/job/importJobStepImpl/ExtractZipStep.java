package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import int_.who.tng.dataimport.job.ImportJobStepException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("ExtractZip")
@RequiredArgsConstructor
@Slf4j
public class ExtractZipStep implements ImportJobStep {

    @Override
    public void exec(ImportJobContext context, String... args) throws ImportJobStepException {
        String archiveFileName = args[0];
        String extractPrefix = args[1];

        log.debug("Extracting ZIP Archive {}", archiveFileName);
        try {
            ZipInputStream zipInputStream = new ZipInputStream(
                new ByteArrayInputStream(
                    context.getFiles().get(archiveFileName)));

            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }

                String filename = zipEntry.getName();
                context.getFiles().put(extractPrefix + "/" + filename, zipInputStream.readAllBytes());
            }
        } catch (IOException e) {
            throw new ImportJobStepException(true, "Extraction of ZIP Archive failed: " + e.getMessage());
        }
    }
}
