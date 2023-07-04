package int_.who.tng.dataimport.job;

public interface ImportJobStep {
    void exec(ImportJobContext context, String... args) throws ImportJobStepException;

}
