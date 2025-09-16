package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.signing.SignedCertificateMessageParser;
import eu.europa.ec.dgc.signing.SignedMessageParser;
import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.testdata.CertificateTestUtils;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {SignCertificatesStep.class, CertificateUtils.class})
public class SignCertificatesStepTest {

    @Autowired
    SignCertificatesStep signCertificatesStep;

    @Autowired
    CertificateUtils certificateUtils;

    private static final String TEST_COUNTRY_CODE_1 = "DE";
    private static final String TEST_COUNTRY_CODE_2 = "AA";
    private static final String TEST_DUMMY_SIGNATURE = "dummy-ta-signature";

    @Test
    void testDscIsSignedByUploadCert() throws Exception {
        KeyPair dscKeyPair = CertificateTestUtils.generateKeyPair();
        KeyPair uploadKeyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate dscCertificateCountry1 =
            CertificateTestUtils.generateCertificate(dscKeyPair, TEST_COUNTRY_CODE_1, "DSC");
        X509Certificate uploadCertificateCountry1 =
            CertificateTestUtils.generateCertificate(uploadKeyPair, TEST_COUNTRY_CODE_1, "Upload");

        X509Certificate dscCertificateCountry2 =
            CertificateTestUtils.generateCertificate(dscKeyPair, TEST_COUNTRY_CODE_2, "DSC");
        X509Certificate uploadCertificateCountry2 =
            CertificateTestUtils.generateCertificate(uploadKeyPair, TEST_COUNTRY_CODE_2, "Upload");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, dscCertificateCountry1, ImportJobContext.CertificateType.DSC, null,
            TEST_COUNTRY_CODE_1, null);
        addCertificate(context, uploadCertificateCountry1, ImportJobContext.CertificateType.UPLOAD,
            TEST_DUMMY_SIGNATURE, TEST_COUNTRY_CODE_1, uploadKeyPair.getPrivate());
        addCertificate(context, dscCertificateCountry2, ImportJobContext.CertificateType.DSC, null,
            TEST_COUNTRY_CODE_2, null);
        addCertificate(context, uploadCertificateCountry2, ImportJobContext.CertificateType.UPLOAD,
            TEST_DUMMY_SIGNATURE, TEST_COUNTRY_CODE_2, uploadKeyPair.getPrivate());

        signCertificatesStep.exec(context,
            ImportJobContext.CertificateType.UPLOAD.toString(),
            ImportJobContext.CertificateType.DSC.toString());

        // Check that DSC were signed with correct Upload certificate
        for (ImportJobContext.CertificateEntry parsedCertificate : context.getParsedCertificates()) {
            if (parsedCertificate.getParsedCertificate().equals(dscCertificateCountry1)) {
                SignedCertificateMessageParser signatureParser = new SignedCertificateMessageParser(
                    parsedCertificate.getSignature(),
                    Base64.getEncoder().encode(parsedCertificate.getParsedCertificate().getEncoded()));

                Assertions.assertEquals(SignedMessageParser.ParserState.SUCCESS, signatureParser.getParserState());
                Assertions.assertTrue(signatureParser.isSignatureVerified());
                Assertions.assertEquals(certificateUtils.convertCertificate(uploadCertificateCountry1),
                    signatureParser.getSigningCertificate());
            } else if (parsedCertificate.getParsedCertificate().equals(dscCertificateCountry2)) {
                SignedCertificateMessageParser signatureParser = new SignedCertificateMessageParser(
                    parsedCertificate.getSignature(),
                    Base64.getEncoder().encode(parsedCertificate.getParsedCertificate().getEncoded()));

                Assertions.assertEquals(SignedMessageParser.ParserState.SUCCESS, signatureParser.getParserState());
                Assertions.assertTrue(signatureParser.isSignatureVerified());
                Assertions.assertEquals(certificateUtils.convertCertificate(uploadCertificateCountry2),
                    signatureParser.getSigningCertificate());
            }
        }
    }

    @Test
    void testDscWithSignatureShouldNotBeTouched() throws Exception {
        KeyPair dscKeyPair = CertificateTestUtils.generateKeyPair();
        KeyPair uploadKeyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate dscCertificateCountry1 =
            CertificateTestUtils.generateCertificate(dscKeyPair, TEST_COUNTRY_CODE_1, "DSC");
        X509Certificate uploadCertificateCountry1 =
            CertificateTestUtils.generateCertificate(uploadKeyPair, TEST_COUNTRY_CODE_1, "Upload");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, dscCertificateCountry1, ImportJobContext.CertificateType.DSC,
            TEST_DUMMY_SIGNATURE, TEST_COUNTRY_CODE_1, null);
        addCertificate(context, uploadCertificateCountry1, ImportJobContext.CertificateType.UPLOAD,
            TEST_DUMMY_SIGNATURE, TEST_COUNTRY_CODE_1, uploadKeyPair.getPrivate());

        signCertificatesStep.exec(context,
            ImportJobContext.CertificateType.UPLOAD.toString(),
            ImportJobContext.CertificateType.DSC.toString());

        for (ImportJobContext.CertificateEntry parsedCertificate : context.getParsedCertificates()) {
            Assertions.assertEquals(TEST_DUMMY_SIGNATURE, parsedCertificate.getSignature());
        }
    }

    @Test
    void testItShouldNotThrowAnExceptionIfNoSignerCertIsPresent() throws Exception {
        KeyPair dscKeyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate dscCertificateCountry1 =
            CertificateTestUtils.generateCertificate(dscKeyPair, TEST_COUNTRY_CODE_1, "DSC");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, dscCertificateCountry1, ImportJobContext.CertificateType.DSC,
            null, TEST_COUNTRY_CODE_1, null);

        signCertificatesStep.exec(context,
            ImportJobContext.CertificateType.UPLOAD.toString(),
            ImportJobContext.CertificateType.DSC.toString());
    }

    @Test
    void testSigningShouldNotFailIfMoreThanOneSignerCertificateForCountryIsAvailable() throws Exception {
        KeyPair dscKeyPair = CertificateTestUtils.generateKeyPair();
        KeyPair uploadKeyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate dscCertificate =
            CertificateTestUtils.generateCertificate(dscKeyPair, TEST_COUNTRY_CODE_1, "DSC");
        X509Certificate uploadCertificate1 =
            CertificateTestUtils.generateCertificate(uploadKeyPair, TEST_COUNTRY_CODE_1, "Upload 1");
        X509Certificate uploadCertificate2 =
            CertificateTestUtils.generateCertificate(uploadKeyPair, TEST_COUNTRY_CODE_1, "Upload 2");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, dscCertificate, ImportJobContext.CertificateType.DSC, null,
            TEST_COUNTRY_CODE_1, null);
        addCertificate(context, uploadCertificate1, ImportJobContext.CertificateType.UPLOAD,
            TEST_DUMMY_SIGNATURE, TEST_COUNTRY_CODE_1, uploadKeyPair.getPrivate());
        addCertificate(context, uploadCertificate2, ImportJobContext.CertificateType.UPLOAD,
            TEST_DUMMY_SIGNATURE, TEST_COUNTRY_CODE_1, uploadKeyPair.getPrivate());

        signCertificatesStep.exec(context,
            ImportJobContext.CertificateType.UPLOAD.toString(),
            ImportJobContext.CertificateType.DSC.toString());

        // Check that DSC was signed with one of the two Upload Certs
        for (ImportJobContext.CertificateEntry parsedCertificate : context.getParsedCertificates()) {
            if (parsedCertificate.getParsedCertificate().equals(dscCertificate)) {
                SignedCertificateMessageParser signatureParser = new SignedCertificateMessageParser(
                    parsedCertificate.getSignature(),
                    Base64.getEncoder().encode(parsedCertificate.getParsedCertificate().getEncoded()));

                Assertions.assertEquals(SignedMessageParser.ParserState.SUCCESS, signatureParser.getParserState());
                Assertions.assertTrue(signatureParser.isSignatureVerified());

                Assertions.assertTrue(
                    certificateUtils.convertCertificate(uploadCertificate1)
                        .equals(signatureParser.getSigningCertificate()) ||
                        certificateUtils.convertCertificate(uploadCertificate2)
                            .equals(signatureParser.getSigningCertificate()));

            }
        }
    }

    private void addCertificate(ImportJobContext context, X509Certificate certificate,
                                ImportJobContext.CertificateType certificateType, String signature, String countryCode,
                                PrivateKey privateKey)
        throws CertificateEncodingException, IOException {
        context.getParsedCertificates().add(new ImportJobContext.CertificateEntry(
            certificateUtils.convertCertificate(certificate),
            certificate,
            privateKey,
            certificate.getEncoded(),
            certificateUtils.getCertThumbprint(certificate),
            signature,
            countryCode,
            certificateType,
            null));
    }
}
