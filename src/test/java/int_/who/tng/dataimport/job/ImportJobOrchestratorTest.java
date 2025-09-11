package int_.who.tng.dataimport.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import int_.who.tng.dataimport.config.DccConfigProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = {ImportJobOrchestrator.class},
        properties = "dgc.import-job-steps=[]"
)
@Slf4j
public class ImportJobOrchestratorTest {

    @MockBean(name = "DownloadFile")
    ImportJobStep testImportJobStep1;

    @MockBean(name = "VerifyFileSignature")
    ImportJobStep testImportJobStep2;

    @MockBean(name = "ExtractZip")
    ImportJobStep testImportJobStep3;

    @MockBean
    DccConfigProperties dccConfigPropertiesMock;

    @Autowired
    ImportJobOrchestrator importJobOrchestrator;

    private static final String TEST_ARG_1 = "Argument 1";
    private static final String TEST_ARG_2 = "Argument 2";
    private static final String TEST_ARG_3 = "Argument 3";

    @Test
    void testJobStepsAreExecuted() {
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
                new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                        new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true),
                new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.VerifyFileSignature,
                        new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true),
                new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.ExtractZip,
                        new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)
        ));

        doNothing().when(testImportJobStep1).exec(any(ImportJobContext.class), any(String[].class));
        doNothing().when(testImportJobStep2).exec(any(ImportJobContext.class), any(String[].class));
        doNothing().when(testImportJobStep3).exec(any(ImportJobContext.class), any(String[].class));

        importJobOrchestrator.exec();

        ArgumentCaptor<ImportJobContext> captor1 = ArgumentCaptor.forClass(ImportJobContext.class);
        ArgumentCaptor<ImportJobContext> captor2 = ArgumentCaptor.forClass(ImportJobContext.class);
        ArgumentCaptor<ImportJobContext> captor3 = ArgumentCaptor.forClass(ImportJobContext.class);

        InOrder inOrder = inOrder(testImportJobStep1, testImportJobStep2, testImportJobStep3);
        inOrder.verify(testImportJobStep1).exec(captor1.capture(), eq(TEST_ARG_1), eq(TEST_ARG_2), eq(TEST_ARG_3));
        inOrder.verify(testImportJobStep2).exec(captor2.capture(), eq(TEST_ARG_1), eq(TEST_ARG_2), eq(TEST_ARG_3));
        inOrder.verify(testImportJobStep3).exec(captor3.capture(), eq(TEST_ARG_1), eq(TEST_ARG_2), eq(TEST_ARG_3));

        Assertions.assertSame(captor1.getValue(), captor2.getValue());
        Assertions.assertSame(captor1.getValue(), captor3.getValue());
    }

    @Test
    void testImportJobStepExceptionThrownDuringJobExecution() {
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
                new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                        new String[]{TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)
        ));

        // Critical ImportJobStepException, failOnCriticalException = true
        doThrow(new ImportJobStepException(true, "Test-Exception"))
                .when(testImportJobStep1)
                .exec(any(ImportJobContext.class), any(String[].class));
        Assertions.assertThrows(RuntimeException.class, () -> importJobOrchestrator.exec());

        // Critical ImportJobStepException, failOnCriticalException = false
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
                new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                        new String[]{TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, false)
        ));
        doThrow(new ImportJobStepException(true, "Test-Exception"))
                .when(testImportJobStep1)
                .exec(any(ImportJobContext.class), any(String[].class));
        Assertions.assertDoesNotThrow(() -> importJobOrchestrator.exec());

        // Uncritical ImportJobStepException, failOnCriticalException = true
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
                new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                        new String[]{TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)
        ));
        doThrow(new ImportJobStepException(false, "Test-Exception"))
                .when(testImportJobStep1)
                .exec(any(ImportJobContext.class), any(String[].class));
        Assertions.assertDoesNotThrow(() -> importJobOrchestrator.exec());

        // Uncritical ImportJobStepException, failOnCriticalException = false
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
                new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                        new String[]{TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, false)
        ));
        doThrow(new ImportJobStepException(false, "Test-Exception"))
                .when(testImportJobStep1)
                .exec(any(ImportJobContext.class), any(String[].class));
        Assertions.assertDoesNotThrow(() -> importJobOrchestrator.exec());
    }

    @Test
    void testExecutionShouldBeCanceledOnIndexOutOfBoundsException() {
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
                new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                        new String[]{TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)
        ));
        doThrow(new IndexOutOfBoundsException())
                .when(testImportJobStep1)
                .exec(any(ImportJobContext.class), any(String[].class));
        Assertions.assertThrows(RuntimeException.class, () -> importJobOrchestrator.exec());
    }

    @Test
    void testExecutionShouldBeCanceledOnAnyOtherUnexpectedException() {
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
                new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                        new String[]{TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)
        ));
        doThrow(new RuntimeException())
                .when(testImportJobStep1)
                .exec(any(ImportJobContext.class), any(String[].class));
        Assertions.assertThrows(RuntimeException.class, () -> importJobOrchestrator.exec());
    }
}
