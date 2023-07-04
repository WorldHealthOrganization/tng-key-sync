package int_.who.tng.dataimport.job;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImportJobStepException extends RuntimeException {

    /**
     * Criticality of the Exception. Critical advises to cancel the whole import process.
     */
    private final boolean critical;

    /**
     * Description of the exception.
     */
    private final String message;

}
