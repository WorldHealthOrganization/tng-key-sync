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
            importJobStepImpl.exec(context, step.getArgs());
        });

        log.info("Finished DCC Key Import Job");
    }
}
