package int_.who.tng.dataimport.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

@SpringBootTest(properties = "dgc.import-job-steps=[]")
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
            new DccConfigProperties.ImportJobStep(
                DccConfigProperties.ImportJobStep.ImportJobStepNames.VerifyFileSignature,
                new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true),
            new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.ExtractZip,
                new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)
        ));

        ArgumentCaptor<ImportJobContext> contextArgumentCaptor = ArgumentCaptor.forClass(ImportJobContext.class);

        doNothing().when(testImportJobStep1).exec(contextArgumentCaptor.capture(), any());
        doNothing().when(testImportJobStep2).exec(contextArgumentCaptor.capture(), any());
        doNothing().when(testImportJobStep3).exec(contextArgumentCaptor.capture(), any());

        importJobOrchestrator.exec();

        // ensure all steps were called with the same context
        for (ImportJobContext capturedContext : contextArgumentCaptor.getAllValues()) {
            Assertions.assertSame(contextArgumentCaptor.getAllValues().get(0), capturedContext);
        }
        ImportJobContext context = contextArgumentCaptor.getValue();

        // Ensure all steps were called in correct order with correct arguments
        InOrder inOrder = Mockito.inOrder(testImportJobStep1, testImportJobStep2, testImportJobStep3);
        inOrder.verify(testImportJobStep1).exec(same(context), eq(TEST_ARG_1), eq(TEST_ARG_2), eq(TEST_ARG_3));
        inOrder.verify(testImportJobStep2).exec(same(context), eq(TEST_ARG_1), eq(TEST_ARG_2), eq(TEST_ARG_3));
        inOrder.verify(testImportJobStep3).exec(same(context), eq(TEST_ARG_1), eq(TEST_ARG_2), eq(TEST_ARG_3));
    }

    @Test
    void testImportJobStepExceptionThrownDuringJobExecution() {
        // Critical ImportJobStepException, FailOnCriticalException true --> Runtime Exception, Exit Code 1
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
            new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)));

        doThrow(new ImportJobStepException(true, "Test-Exception"))
            .when(testImportJobStep1).exec(any(), any());

        Assertions.assertThrows(RuntimeException.class, () -> importJobOrchestrator.exec());

        // Critical ImportJobStepException, FailOnCriticalException false --> No Runtime Exception
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
            new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, false)));

        doThrow(new ImportJobStepException(true, "Test-Exception"))
            .when(testImportJobStep1).exec(any(), any());

        Assertions.assertDoesNotThrow(() -> importJobOrchestrator.exec());

        // Uncritical ImportJobStepException, FailOnCriticalException true --> No Runtime Exception
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
            new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)));

        doThrow(new ImportJobStepException(false, "Test-Exception"))
            .when(testImportJobStep1).exec(any(), any());

        Assertions.assertDoesNotThrow(() -> importJobOrchestrator.exec());

        // Uncritical ImportJobStepException, FailOnCriticalException false --> No Runtime Exception
        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
            new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, false)));

        doThrow(new ImportJobStepException(false, "Test-Exception"))
            .when(testImportJobStep1).exec(any(), any());

        Assertions.assertDoesNotThrow(() -> importJobOrchestrator.exec());

    }

    @Test
    void testExecutionShouldBeCanceledOnIndexOutOfBoundsException() {

        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
            new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)));

        doThrow(new IndexOutOfBoundsException())
            .when(testImportJobStep1).exec(any(), any());

        Assertions.assertThrows(RuntimeException.class, () -> importJobOrchestrator.exec());
    }

    @Test
    void testExecutionShouldBeCanceledOnAnyOtherUnexpectedException() {

        when(dccConfigPropertiesMock.getImportJobSteps()).thenReturn(List.of(
            new DccConfigProperties.ImportJobStep(DccConfigProperties.ImportJobStep.ImportJobStepNames.DownloadFile,
                new String[] {TEST_ARG_1, TEST_ARG_2, TEST_ARG_3}, true)));

        doThrow(new RuntimeException())
            .when(testImportJobStep1).exec(any(), any());

        Assertions.assertThrows(RuntimeException.class, () -> importJobOrchestrator.exec());
    }

}
