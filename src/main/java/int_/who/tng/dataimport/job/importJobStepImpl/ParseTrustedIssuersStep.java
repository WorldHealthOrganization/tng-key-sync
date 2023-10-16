package int_.who.tng.dataimport.job.importJobStepImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStepException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import int_.who.tng.dataimport.job.ImportJobStep;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ParseTrustedIssuers")
@RequiredArgsConstructor
@Slf4j
public class ParseTrustedIssuersStep implements ImportJobStep {

    private final ObjectMapper objectMapper;

    @Override
    public void exec(ImportJobContext context, String... args) throws ImportJobStepException {
        Pattern fileNamePattern = Pattern.compile(args[0]);

        log.debug("Parsing Files matching {} as Trusted Issuer", fileNamePattern);

        List<ImportJobContext.TrustedIssuerEntry> parsedIssuers = context.getFiles().entrySet().stream()
                .filter(entry -> fileNamePattern.matcher(entry.getKey()).matches())
                .map(file -> {

                    Matcher matcher = fileNamePattern.matcher(file.getKey());
                    matcher.find();
                    String domain = null;
                    String countryAlpha3 = null;
                    try {
                        domain = matcher.group("DOMAIN");
                    } catch (IllegalArgumentException ignored) {
                        log.warn("Failed to extract domain on parsing trusted issuer files matching {}. Using default domain.", fileNamePattern);
                    }

                    try {
                        countryAlpha3 = matcher.group("COUNTRYALPHA3");
                    } catch (IllegalArgumentException ignored) {
                        log.info("Failed to extract country alpha-3 code parsing trusted issuer files matching {}", fileNamePattern);
                    }

                    return parseJson(file.getValue(), countryAlpha3, domain);

                })
                .toList();

        context.getParsedIssuers().addAll(parsedIssuers);
    }


    private ImportJobContext.TrustedIssuerEntry parseJson(byte[] file, String countryAlpha3, String domain)
            throws ImportJobStepException {
        try {
            ParseTrustedIssuersStep.JsonStructure json = objectMapper.readValue(file, ParseTrustedIssuersStep.JsonStructure.class);

            return new ImportJobContext.TrustedIssuerEntry(
                    json.name,
                    json.url,
                    json.urlType,
                    json.hash,
                    json.sslPublicKeys,
                    json.signature,
                    json.country,
                    countryAlpha3,
                    domain);

        } catch (IOException e) {
            throw new ImportJobStepException(true, "Failed to parse Certificate: " + e.getMessage());
        }
    }


    @NoArgsConstructor
    @Getter
    @Setter
    static class JsonStructure {
        private String name;

        private String url;

        private String urlType;

        private String hash;

        private List<String> sslPublicKeys;

        private String signature;

        private String country;
    }

}
