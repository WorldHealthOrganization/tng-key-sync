package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import int_.who.tng.dataimport.job.ImportJobStepException;
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
    public void exec(ImportJobContext context, String... args) throws ImportJobStepException {
        final String downloadUri = args[0];
        final String targetFileName = args[1];

        log.debug("Downloading File from {} as {}", downloadUri, targetFileName);

        try {
            CloseableHttpResponse response = httpClient.execute(RequestBuilder.get(downloadUri).build());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ImportJobStepException(true, "Download from " + downloadUri + " failed with HTTP Status " +
                    response.getStatusLine().getStatusCode());
            }

            byte[] downloadedFile = response.getEntity().getContent().readAllBytes();
            context.getFiles().put(targetFileName, downloadedFile);

            log.debug("Downloaded File from {} as {} with {} bytes.", downloadUri, targetFileName,
                downloadedFile.length);
        } catch (IOException e) {
            throw new ImportJobStepException(true, "Download from " + downloadUri + " failed: " + e.getMessage());
        }
    }
}
