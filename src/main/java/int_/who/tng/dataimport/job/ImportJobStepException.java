package int_.who.tng.dataimport.job;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImportJobStepException extends Exception {

    private final boolean critical;
    private final String message;

}
