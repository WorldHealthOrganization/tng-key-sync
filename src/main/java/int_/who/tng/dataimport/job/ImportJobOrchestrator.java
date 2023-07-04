package int_.who.tng.dataimport.job;

import int_.who.tng.dataimport.config.DccConfigProperties;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImportJobOrchestrator {

    private final Map<String, ImportJobStep> importJobSteps;

    private final DccConfigProperties dccConfigProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void exec() {
        log.info("Starting DCC Key Import Job");

        ImportJobContext context = new ImportJobContext();

        dccConfigProperties.getImportJobSteps().forEach((step) -> {
            ImportJobStep importJobStepImpl = importJobSteps.get(step.getName().toString());

            if (importJobStepImpl == null) {
                log.error("Could not find implementation for {}", step.getName());
                System.exit(1);
            }

            log.info("Executing {} for {} with {} args", importJobStepImpl.getClass().getName(),
                step.getName(), step.getArgs().length);
            log.debug("Args: {}", Arrays.asList(step.getArgs()));

            try {
                importJobStepImpl.exec(context, step.getArgs());
            } catch (ImportJobStepException e) {
                if (e.isCritical()) {
                    log.error("CRITICAL ERROR occurred during execution of step {} with args {}: {}",
                        step.getName(), step.getArgs(), e.getMessage());
                } else {
                    log.error("Error occurred during execution of step {} with args {}: {}",
                        step.getName(), step.getArgs(), e.getMessage());
                }

                if (e.isCritical() && step.isFailOnCriticalException()) {
                    log.error("Execution will be canceled because of critical error.");
                    System.exit(1);
                }
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                log.error(
                    "{} occurred at step {} with args {}: Please check the arguments passed to the import step.",
                    e.getClass().getSimpleName(), step.getName(), step.getArgs());
                log.error("Execution will be canceled because of critical error.");
                System.exit(1);
            } catch (Exception e) {
                log.error(
                    "Unexpected Exception of type {} occurred at step {} with args {}. See Stacktrace for details.",
                    e.getClass().getSimpleName(), step.getName(), step.getArgs(), e);
                log.error("Execution will be canceled because of critical error.");
                System.exit(1);
            }
        });

        log.info("Finished DCC Key Import Job");
    }
}
