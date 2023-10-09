package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStepException;
import int_.who.tng.dataimport.testdata.CertificateTestUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ParseCertificatesStepTest {

    @Autowired
    ParseCertificatesStep parseCertificatesStep;

    @Autowired
    CertificateUtils certificateUtils;

    private final static String TEST_COUNTRY_CODE = "DE";

    private final static String TEST_TRUSTANCHOR_SIGNATURE = "dummy-ta-signature";
    private final static String TEST_CERTIFICATE_1_FILENAME_PEM = "abc/def/ghi/cert1.pem";
    private final static String TEST_CERTIFICATE_1_FILENAME_JSON = "abc/def/ghi/cert1.json";
    private final static String TEST_CERTIFICATE_2_FILENAME_PEM = "abc/def/ghi/cert2.pem";
    private final static String TEST_CERTIFICATE_2_FILENAME_JSON = "abc/def/ghi/cert2.json";
    private final static String TEST_CERTIFICATE_3_FILENAME_PEM = "abc/def/ghi/cert3.pem";
    private final static String TEST_CERTIFICATE_3_FILENAME_JSON = "abc/def/ghi/cert3.json";

    @Test
    void testParsingCertificatesFromPEM() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 3");

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_CERTIFICATE_1_FILENAME_PEM, CertificateTestUtils.toPem(certificate1).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_2_FILENAME_PEM, CertificateTestUtils.toPem(certificate2).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_3_FILENAME_PEM, CertificateTestUtils.toPem(certificate3).getBytes(
            StandardCharsets.UTF_8));

        parseCertificatesStep.exec(context, "^abc/def/ghi/.*\\.pem",
            ImportJobContext.CertificateType.UPLOAD.toString(), "PEM");

        Assertions.assertEquals(3, context.getParsedCertificates().size());
        for (ImportJobContext.CertificateEntry certificateEntry : context.getParsedCertificates()) {
            if (certificateEntry.getParsedCertificate().equals(certificate1)) {
                checkCertEntry(certificateEntry, certificate1, null);
            } else if (certificateEntry.getParsedCertificate().equals(certificate2)) {
                checkCertEntry(certificateEntry, certificate2, null);
            } else if (certificateEntry.getParsedCertificate().equals(certificate3)) {
                checkCertEntry(certificateEntry, certificate3, null);
            } else {
                Assertions.fail("Unexpected cert in context");
            }
        }
    }

    @Test
    void testParsingCertificatesFromJSON() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 3");

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_CERTIFICATE_1_FILENAME_JSON, getJson(certificate1).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_2_FILENAME_JSON, getJson(certificate2).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_3_FILENAME_JSON, getJson(certificate3).getBytes(
            StandardCharsets.UTF_8));

        parseCertificatesStep.exec(context, "^abc/def/ghi/.*\\.json$",
            ImportJobContext.CertificateType.UPLOAD.toString(), "JSON");

        Assertions.assertEquals(3, context.getParsedCertificates().size());
        for (ImportJobContext.CertificateEntry certificateEntry : context.getParsedCertificates()) {
            if (certificateEntry.getParsedCertificate().equals(certificate1)) {
                checkCertEntry(certificateEntry, certificate1, TEST_TRUSTANCHOR_SIGNATURE);
            } else if (certificateEntry.getParsedCertificate().equals(certificate2)) {
                checkCertEntry(certificateEntry, certificate2, TEST_TRUSTANCHOR_SIGNATURE);
            } else if (certificateEntry.getParsedCertificate().equals(certificate3)) {
                checkCertEntry(certificateEntry, certificate3, TEST_TRUSTANCHOR_SIGNATURE);
            } else {
                Assertions.fail("Unexpected cert in context");
            }
        }
    }

    @Test
    void testParsingCertificatesWithDomainExtractedByRegex() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
                CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 1");

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_CERTIFICATE_1_FILENAME_JSON, getJson(certificate1).getBytes(
                StandardCharsets.UTF_8));


        parseCertificatesStep.exec(context, "^abc\\/def\\/(?<DOMAIN>[\\w-]+)\\/.*\\.json$",
                ImportJobContext.CertificateType.UPLOAD.toString(), "JSON");

        Assertions.assertEquals(1, context.getParsedCertificates().size());
        for (ImportJobContext.CertificateEntry certificateEntry : context.getParsedCertificates()) {
            if (certificateEntry.getParsedCertificate().equals(certificate1)) {
                checkCertEntry(certificateEntry, certificate1, TEST_TRUSTANCHOR_SIGNATURE);
                Assertions.assertEquals("ghi", certificateEntry.getDomain());
            } else {
                Assertions.fail("Unexpected cert in context");
            }
        }
    }

    @Test
    void testOnlyRegExMatchingCertsShouldBeProcessed() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 3");

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_CERTIFICATE_1_FILENAME_PEM, CertificateTestUtils.toPem(certificate1).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_2_FILENAME_PEM, CertificateTestUtils.toPem(certificate2).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_3_FILENAME_PEM, CertificateTestUtils.toPem(certificate3).getBytes(
            StandardCharsets.UTF_8));

        parseCertificatesStep.exec(context, "^abc/def/ghi/cert(1|3)\\.pem",
            ImportJobContext.CertificateType.UPLOAD.toString(), "PEM");

        Assertions.assertEquals(2, context.getParsedCertificates().size());
        for (ImportJobContext.CertificateEntry certificateEntry : context.getParsedCertificates()) {
            if (certificateEntry.getParsedCertificate().equals(certificate1)) {
                checkCertEntry(certificateEntry, certificate1, null);
            } else if (certificateEntry.getParsedCertificate().equals(certificate2)) {
                Assertions.fail("Certificate 2 should not be imported!");
            } else if (certificateEntry.getParsedCertificate().equals(certificate3)) {
                checkCertEntry(certificateEntry, certificate3, null);
            } else {
                Assertions.fail("Unexpected cert in context");
            }
        }
    }

    @Test
    void testShouldThrowAnExceptionOnWrongInputType() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 1");

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_CERTIFICATE_1_FILENAME_PEM, CertificateTestUtils.toPem(certificate1).getBytes(
            StandardCharsets.UTF_8));

        context.getFiles().put(TEST_CERTIFICATE_1_FILENAME_JSON, getJson(certificate1).getBytes(
            StandardCharsets.UTF_8));

        Assertions.assertThrows(ImportJobStepException.class, () -> parseCertificatesStep.exec(
            context, "^abc/def/ghi/.*\\.pem$", ImportJobContext.CertificateType.UPLOAD.toString(), "JSON"));

        Assertions.assertThrows(ImportJobStepException.class, () -> parseCertificatesStep.exec(
            context, "^abc/def/ghi/.*\\.json$", ImportJobContext.CertificateType.UPLOAD.toString(), "PEM"));
    }

    @Test
    void testShouldThrowAnExceptionOnUnknownInputType() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 1");

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_CERTIFICATE_1_FILENAME_PEM, CertificateTestUtils.toPem(certificate1).getBytes(
            StandardCharsets.UTF_8));

        Assertions.assertThrows(ImportJobStepException.class, () -> parseCertificatesStep.exec(
            context, "^abc/def/ghi/.*\\.pem$", ImportJobContext.CertificateType.UPLOAD.toString(), "XXX"));
    }

    @Test
    void testShouldThrowAnExceptionIfCertificateDoesNotContainsCountryAttribute_JSON() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, null, "Certificate 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 3");

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_CERTIFICATE_1_FILENAME_JSON, getJson(certificate1).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_2_FILENAME_JSON, getJson(certificate2).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_3_FILENAME_JSON, getJson(certificate3).getBytes(
            StandardCharsets.UTF_8));

        Assertions.assertThrows(ImportJobStepException.class, () -> parseCertificatesStep.exec(
            context, "^abc/def/ghi/.*\\.json$", ImportJobContext.CertificateType.UPLOAD.toString(), "JSON"));
    }

    @Test
    void testShouldThrowAnExceptionIfCertificateDoesNotContainsCountryAttribute_PEM() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, null, "Certificate 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Certificate 3");

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_CERTIFICATE_1_FILENAME_PEM, CertificateTestUtils.toPem(certificate1).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_2_FILENAME_PEM, CertificateTestUtils.toPem(certificate2).getBytes(
            StandardCharsets.UTF_8));
        context.getFiles().put(TEST_CERTIFICATE_3_FILENAME_PEM, CertificateTestUtils.toPem(certificate3).getBytes(
            StandardCharsets.UTF_8));

        Assertions.assertThrows(ImportJobStepException.class, () -> parseCertificatesStep.exec(
            context, "^abc/def/ghi/.*\\.pem$", ImportJobContext.CertificateType.UPLOAD.toString(), "PEM"));
    }

    @Test
    void testShouldThrowExceptionOnInvalidJson() {
        ImportJobContext context = new ImportJobContext();
        context.getFiles()
            .put(TEST_CERTIFICATE_1_FILENAME_JSON, "this is no valid json!!!".getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(ImportJobStepException.class, () -> parseCertificatesStep.exec(
            context, "^abc/def/ghi/.*\\.json$", ImportJobContext.CertificateType.UPLOAD.toString(), "JSON"));

        Assertions.assertEquals(0, context.getParsedCertificates().size());
    }

    @Test
    void testShouldThrowExceptionOnJsonWithInvalidValues() {
        ImportJobContext context = new ImportJobContext();
        context.getFiles()
            .put(TEST_CERTIFICATE_1_FILENAME_JSON, """
            {
                "trustAnchorSignature": [1,2,3],
                "certificateRawData": true,
                "country": "DE"
            }
            """.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(ImportJobStepException.class, () -> parseCertificatesStep.exec(
            context, "^abc/def/ghi/.*\\.json$", ImportJobContext.CertificateType.UPLOAD.toString(), "JSON"));

        Assertions.assertEquals(0, context.getParsedCertificates().size());
    }

    private String getJson(X509Certificate certificate) throws CertificateEncodingException {
        return """
            {
                "trustAnchorSignature": "%s",
                "certificateRawData": "%s",
                "country": "%s"
            }
            """.formatted(
            TEST_TRUSTANCHOR_SIGNATURE,
            Base64.getEncoder().encodeToString(certificate.getEncoded()),
            TEST_COUNTRY_CODE
        );
    }

    void checkCertEntry(ImportJobContext.CertificateEntry certificateEntry, X509Certificate certificate,
                        String signature)
        throws CertificateEncodingException, IOException {
        Assertions.assertEquals(certificateUtils.getCertThumbprint(certificate), certificateEntry.getThumbprint());
        Assertions.assertEquals(TEST_COUNTRY_CODE, certificateEntry.getCountry());
        Assertions.assertEquals(ImportJobContext.CertificateType.UPLOAD, certificateEntry.getCertificateType());
        Assertions.assertEquals(certificate, certificateEntry.getParsedCertificate());
        Assertions.assertEquals(certificateUtils.convertCertificate(certificate),
            certificateEntry.getParsedCertificateHolder());
        Assertions.assertArrayEquals(certificate.getEncoded(), certificateEntry.getRawCertificate());
        Assertions.assertEquals(signature, certificateEntry.getSignature());
        Assertions.assertNull(certificateEntry.getPrivateKey());
    }


}
