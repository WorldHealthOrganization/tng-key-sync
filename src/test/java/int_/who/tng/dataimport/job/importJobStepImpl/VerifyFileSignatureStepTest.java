package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.signing.SignedByteArrayMessageBuilder;
import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStepException;
import int_.who.tng.dataimport.testdata.CertificateTestUtils;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class VerifyFileSignatureStepTest {

    @Autowired
    VerifyFileSignatureStep verifyFileSignatureStep;

    @Autowired
    CertificateUtils certificateUtils;

    private static final String TEST_COUNTRY_CODE = "DE";
    private static final String TEST_FILE_NAME = "file.txt";
    private static final String TEST_FILE_SIGNATURE_NAME = "file.txt.sig";

    @Test
    void testVerifyFileSignature() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();
        X509Certificate signerCert =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Signer Certificate");

        byte[] file = new byte[2048];
        new Random().nextBytes(file);

        byte[] signature = Base64.getEncoder().encode(new SignedByteArrayMessageBuilder()
            .withSigningCertificate(certificateUtils.convertCertificate(signerCert), keyPair.getPrivate())
            .withPayload(file)
            .build(true));

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_FILE_NAME, file);
        context.getFiles().put(TEST_FILE_SIGNATURE_NAME, signature);

        verifyFileSignatureStep.exec(context, TEST_FILE_NAME, TEST_FILE_SIGNATURE_NAME,
            certificateUtils.getCertThumbprint(signerCert));
    }

    @Test
    void testItShouldThrowAnExceptionIfSignatureFileIsInvalid() {
        byte[] file = new byte[2048];
        new Random().nextBytes(file);

        byte[] signature = new byte[256];
        new Random().nextBytes(signature);

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_FILE_NAME, file);
        context.getFiles().put(TEST_FILE_SIGNATURE_NAME, signature);

        Assertions.assertThrows(ImportJobStepException.class,
            () -> verifyFileSignatureStep.exec(context, TEST_FILE_NAME, TEST_FILE_SIGNATURE_NAME, ""));
    }

    @Test
    void testItShouldThrowAnExceptionWhenSignatureDigestIsNotCorrect() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();
        X509Certificate signerCert =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Signer Certificate");

        byte[] file = new byte[2048];
        new Random().nextBytes(file);

        byte[] anotherFile = new byte[2048];
        new Random().nextBytes(file);

        byte[] signature = Base64.getEncoder().encode(new SignedByteArrayMessageBuilder()
            .withSigningCertificate(certificateUtils.convertCertificate(signerCert), keyPair.getPrivate())
            .withPayload(anotherFile)
            .build(true));

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_FILE_NAME, file);
        context.getFiles().put(TEST_FILE_SIGNATURE_NAME, signature);

        Assertions.assertThrows(ImportJobStepException.class,
            () -> verifyFileSignatureStep.exec(context, TEST_FILE_NAME, TEST_FILE_SIGNATURE_NAME,
                certificateUtils.getCertThumbprint(signerCert)));
    }

    @Test
    void testItShouldThrowAnUncriticalExceptionWhenCertificateAllowListIsEmpty() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();
        X509Certificate signerCert =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Signer Certificate");

        byte[] file = new byte[2048];
        new Random().nextBytes(file);

        byte[] signature = Base64.getEncoder().encode(new SignedByteArrayMessageBuilder()
            .withSigningCertificate(certificateUtils.convertCertificate(signerCert), keyPair.getPrivate())
            .withPayload(file)
            .build(true));

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_FILE_NAME, file);
        context.getFiles().put(TEST_FILE_SIGNATURE_NAME, signature);

        ImportJobStepException thrownException = Assertions.assertThrows(ImportJobStepException.class,
            () -> verifyFileSignatureStep.exec(context, TEST_FILE_NAME, TEST_FILE_SIGNATURE_NAME));

        Assertions.assertFalse(thrownException.isCritical());
    }

    @Test
    void testItShouldThrowAnExceptionWhenSigningCertificateIsNotOnAllowList() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();
        X509Certificate signerCert =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Signer Certificate");

        byte[] file = new byte[2048];
        new Random().nextBytes(file);

        byte[] signature = Base64.getEncoder().encode(new SignedByteArrayMessageBuilder()
            .withSigningCertificate(certificateUtils.convertCertificate(signerCert), keyPair.getPrivate())
            .withPayload(file)
            .build(true));

        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_FILE_NAME, file);
        context.getFiles().put(TEST_FILE_SIGNATURE_NAME, signature);

        Assertions.assertThrows(ImportJobStepException.class,
            () -> verifyFileSignatureStep.exec(context, TEST_FILE_NAME, TEST_FILE_SIGNATURE_NAME,
                "another-certificate-thumbprint"));
    }


}
