package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.stereotype.Service;

@Service("DownloadFile")
@RequiredArgsConstructor
@Slf4j
public class DownloadFileStep implements ImportJobStep {

    private final CloseableHttpClient httpClient;

    @Override
    public void exec(ImportJobContext context, String... args) {
        final String downloadUri = args[0];
        final String targetFileName = args[1];

        log.info("Downloading File from {} as {}", downloadUri, targetFileName);

        try {
            CloseableHttpResponse response = httpClient.execute(RequestBuilder.get(downloadUri).build());
            byte[] downloadedFile = response.getEntity().getContent().readAllBytes();

            context.getFiles().put(targetFileName, downloadedFile);
            log.info("Downloaded File from {} as {} with {} bytes.", downloadUri, targetFileName,
                downloadedFile.length);
        } catch (IOException e) {
            log.error("Download from {} failed.", downloadUri, e);
            System.exit(1);
        }
    }
}
