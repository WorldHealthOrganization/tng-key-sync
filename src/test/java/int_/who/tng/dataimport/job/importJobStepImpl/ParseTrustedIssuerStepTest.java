package int_.who.tng.dataimport.job.importJobStepImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.testdata.TrustedIssuerTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;


@SpringBootTest(classes = {ParseTrustedIssuersStep.class, ObjectMapper.class})
public class ParseTrustedIssuerStepTest {

    @Autowired
    ParseTrustedIssuersStep parseTrustedIssuerStep;

    private final static String TEST_TRUSTED_ISSUER_FILENAME_JSON = "abc/def/ghi/Trusted_Issuer.json";
    private final static String TEST_TRUSTED_ISSUER_FILENAME_1_JSON = "abc/def/ghi/Trusted_Issuer_1.json";

    @Test
    void testParsingTrustedIssuerFromJSON() {
        //Given we have a JSON file containing a trusted issuer of test country XA in import context
        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_TRUSTED_ISSUER_FILENAME_JSON, TrustedIssuerTestUtils.getJson("XA").getBytes(
                StandardCharsets.UTF_8));

        //when we executed ParseTrustedIssuerStep
        parseTrustedIssuerStep.exec(context, "^abc\\/(?<COUNTRYALPHA3>.{3})\\/(?<DOMAIN>[\\w-]+)\\/.*\\.json$");

        //then we have a TrustedIssuerEntry in the context
        Assertions.assertEquals(1, context.getParsedIssuers().size());

        //and the TrustedIssuerEntry contains the expected values
        ImportJobContext.TrustedIssuerEntry trustedIssuerEntry = context.getParsedIssuers().get(0);
        Assertions.assertEquals("def", trustedIssuerEntry.getCountryAlpha3());
        Assertions.assertEquals("ghi", trustedIssuerEntry.getDomain());
        Assertions.assertEquals("XA", trustedIssuerEntry.getCountry());

    }

    @Test
    void testParsingMultipleTrustedIssuerFromJSON() {
        //Given we have more then 1 JSON file containing a trusted issuer of test country XA in import context
        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_TRUSTED_ISSUER_FILENAME_JSON, TrustedIssuerTestUtils.getJson("XA").getBytes(
                StandardCharsets.UTF_8));
        context.getFiles().put(TEST_TRUSTED_ISSUER_FILENAME_1_JSON, TrustedIssuerTestUtils.getJson("XA").getBytes(
                StandardCharsets.UTF_8));

        //when we executed ParseTrustedIssuerStep
        parseTrustedIssuerStep.exec(context, "^abc\\/(?<COUNTRYALPHA3>.{3})\\/(?<DOMAIN>[\\w-]+)\\/.*\\.json$");

        //then we have a TrustedIssuerEntry in the context
        Assertions.assertEquals(2, context.getParsedIssuers().size());
    }

    @Test
    void testParsingMultipleTrustedIssuerFromJSONHoldAdditionalFilepathInformation() {
        //Given we have more then 1 JSON file containing a trusted issuer of test country XA in import context
        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_TRUSTED_ISSUER_FILENAME_JSON, TrustedIssuerTestUtils.getJson("XA").getBytes(
                StandardCharsets.UTF_8));
        context.getFiles().put(TEST_TRUSTED_ISSUER_FILENAME_1_JSON, TrustedIssuerTestUtils.getJson("XA").getBytes(
                StandardCharsets.UTF_8));

        //when we executed ParseTrustedIssuerStep without a countryAlpha3 group
        parseTrustedIssuerStep.exec(context, "^abc\\/(?<COUNTRYALPHA3>.{3})\\/(?<DOMAIN>[\\w-]+)\\/.*\\.json$");

        //then we have a TrustedIssuerEntry in the context without an alpha 3 country code set
        Assertions.assertEquals(2, context.getParsedIssuers().size());
        Assertions.assertEquals("def", context.getParsedIssuers().get(0).getCountryAlpha3());
        Assertions.assertEquals("def", context.getParsedIssuers().get(1).getCountryAlpha3());
        Assertions.assertEquals("ghi", context.getParsedIssuers().get(0).getDomain());
        Assertions.assertEquals("ghi", context.getParsedIssuers().get(1).getDomain());
    }

    @Test
    void testParsingMultipleTrustedIssuerFromJSONWithMissingCountryAlpha3() {
        //Given we have more then 1 JSON file containing a trusted issuer of test country XA in import context
        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_TRUSTED_ISSUER_FILENAME_JSON, TrustedIssuerTestUtils.getJson("XA").getBytes(
                StandardCharsets.UTF_8));
        context.getFiles().put(TEST_TRUSTED_ISSUER_FILENAME_1_JSON, TrustedIssuerTestUtils.getJson("XA").getBytes(
                StandardCharsets.UTF_8));

        //when we executed ParseTrustedIssuerStep without a countryAlpha3 group
        parseTrustedIssuerStep.exec(context, "^abc\\/.{3}\\/(?<DOMAIN>[\\w-]+)\\/.*\\.json$");

        //then we have a TrustedIssuerEntry in the context without an alpha 3 country code set
        Assertions.assertEquals(2, context.getParsedIssuers().size());
        Assertions.assertNull(context.getParsedIssuers().get(0).getCountryAlpha3());
        Assertions.assertNull(context.getParsedIssuers().get(1).getCountryAlpha3());
    }


}
