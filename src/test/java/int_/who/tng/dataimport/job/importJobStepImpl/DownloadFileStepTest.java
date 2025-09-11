package int_.who.tng.dataimport.job.importJobStepImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStepException;
import java.io.ByteArrayInputStream;
import java.util.Random;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;

@SpringBootTest(classes = {DownloadFileStep.class})
public class DownloadFileStepTest {

    @Autowired
    private DownloadFileStep downloadFileStep;

    @MockBean
    private CloseableHttpClient httpClientMock;

    private final static String TEST_URL = "https://example.org/file.txt";

    private final static String TEST_FILENAME = "test.txt";

    @Test
    void testDownloadFile() throws Exception {
        // Generate Dummy Data
        byte[] dummyPayload = new byte[512];
        new Random().nextBytes(dummyPayload);

        // Prepare Dummy Http Response
        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(new ByteArrayInputStream(dummyPayload));
        httpEntity.setContentLength(dummyPayload.length);
        httpEntity.setContentType(ContentType.APPLICATION_OCTET_STREAM.toString());
        CloseableHttpResponse response =
            new TestCloseableHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, ""));
        response.setEntity(httpEntity);

        // Setup Mock
        ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        when(httpClientMock.execute(requestArgumentCaptor.capture())).thenReturn(response);

        // Execute Import Job step
        ImportJobContext context = new ImportJobContext();
        downloadFileStep.exec(context, TEST_URL, TEST_FILENAME);

        // Check Result of Job Execution
        verify(httpClientMock, times(1)).execute(any());
        HttpUriRequest request = requestArgumentCaptor.getValue();
        Assertions.assertEquals(TEST_URL, request.getURI().toString());
        Assertions.assertEquals(HttpMethod.GET.name(), request.getMethod());
        Assertions.assertTrue(context.getFiles().containsKey(TEST_FILENAME));
        Assertions.assertArrayEquals(dummyPayload, context.getFiles().get(TEST_FILENAME));
    }

    @Test
    void testDownloadFileShouldNotStoreFileOnNon200StatusCode() throws Exception {
        // Generate Dummy Data
        byte[] dummyPayload = new byte[512];
        new Random().nextBytes(dummyPayload);

        // Prepare Dummy Http Response
        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(new ByteArrayInputStream(dummyPayload));
        httpEntity.setContentLength(dummyPayload.length);
        httpEntity.setContentType(ContentType.APPLICATION_OCTET_STREAM.toString());
        CloseableHttpResponse response =
            new TestCloseableHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 401, ""));
        response.setEntity(httpEntity);

        // Setup Mock
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenReturn(response);

        // Execute Import Job step
        ImportJobContext context = new ImportJobContext();
        Assertions.assertThrows(ImportJobStepException.class, () -> downloadFileStep
            .exec(context, TEST_URL, TEST_FILENAME));

        // Check Result of Job Execution
        verify(httpClientMock, times(1)).execute(any());
        Assertions.assertFalse(context.getFiles().containsKey(TEST_FILENAME));
    }

    @Test
    void testDownloadFileShouldNotStoreFileOnNetworkError() throws Exception {
        // Setup Mock
        when(httpClientMock.execute(any(HttpUriRequest.class)))
            .thenThrow(new ClientProtocolException("Network Error"));

        // Execute Import Job step
        ImportJobContext context = new ImportJobContext();
        Assertions.assertThrows(ImportJobStepException.class, () -> downloadFileStep
            .exec(context, TEST_URL, TEST_FILENAME));

        // Check Result of Job Execution
        verify(httpClientMock, times(1)).execute(any());
        Assertions.assertFalse(context.getFiles().containsKey(TEST_FILENAME));
    }

    private static class TestCloseableHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {
        public TestCloseableHttpResponse(StatusLine statusline) {
            super(statusline);
        }

        @Override
        public void close() {
        }
    }

}
