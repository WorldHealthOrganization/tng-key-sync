package int_.who.tng.dataimport.job.importJobStepImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStepException;
import int_.who.tng.dataimport.testdata.CertificateTestUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {MapPrivateKeyStep.class, CertificateUtils.class})
public class MapPrivateKeyStepTest {

    @Autowired
    private MapPrivateKeyStep mapPrivateKeyStep;

    @Autowired
    private CertificateUtils certificateUtils;

    private static final String TEST_COUNTRY_CODE = "DE";

    private static final char[] TEST_KEYSTORE_PASSWORD = "s3cret".toCharArray();

    private static final String TEST_KEYSTORE_ALIAS = "cert";

    private static final String TEST_KEYSTORE_FILENAME = UUID.randomUUID().toString();

    @BeforeAll
    @AfterAll
    static void cleanUp() {
        new File(TEST_KEYSTORE_FILENAME).delete();
    }

    @Test
    void testPrivateKeyIsMappedToCertificates() throws Exception {

        KeyPair keyPair = CertificateTestUtils.generateKeyPair();
        KeyPair keyPairOtherCerts = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Upload Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Upload Testcert 2");
        X509Certificate certificateWithOtherType1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "AUTH Testcert");
        X509Certificate certificateWithOtherType2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "CSCA Testcert");
        X509Certificate certificateWithOtherKey1 =
            CertificateTestUtils.generateCertificate(keyPairOtherCerts, TEST_COUNTRY_CODE, "Upload Testcert 3");
        X509Certificate certificateWithOtherKey2 =
            CertificateTestUtils.generateCertificate(keyPairOtherCerts, TEST_COUNTRY_CODE, "Upload Testcert 4");
        X509Certificate certificateWithOtherType3 =
                CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "DECA Testcert");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.UPLOAD);
        addCertificate(context, certificate2, ImportJobContext.CertificateType.UPLOAD);
        addCertificate(context, certificateWithOtherType1, ImportJobContext.CertificateType.AUTHENTICATION);
        addCertificate(context, certificateWithOtherType2, ImportJobContext.CertificateType.CSCA);
        addCertificate(context, certificateWithOtherType3, ImportJobContext.CertificateType.DECA);
        addCertificate(context, certificateWithOtherKey1, ImportJobContext.CertificateType.UPLOAD);
        addCertificate(context, certificateWithOtherKey2, ImportJobContext.CertificateType.UPLOAD);

        X509Certificate keyWrapper =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Key Wrapper");

        FileOutputStream fileOutputStream = new FileOutputStream(TEST_KEYSTORE_FILENAME);
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(null, TEST_KEYSTORE_PASSWORD);
        keyStore.setKeyEntry(TEST_KEYSTORE_ALIAS, keyPair.getPrivate(), TEST_KEYSTORE_PASSWORD,
            new X509Certificate[] {keyWrapper});
        keyStore.store(fileOutputStream, TEST_KEYSTORE_PASSWORD);
        fileOutputStream.close();

        mapPrivateKeyStep.exec(context, ImportJobContext.CertificateType.UPLOAD.toString(), TEST_KEYSTORE_FILENAME,
            String.valueOf(TEST_KEYSTORE_PASSWORD),
            TEST_KEYSTORE_ALIAS, String.valueOf(TEST_KEYSTORE_PASSWORD));

        context.getParsedCertificates().forEach(certificateEntry -> {
            if (certificateEntry.getParsedCertificate().getPublicKey().equals(keyPair.getPublic()) &&
                certificateEntry.getCertificateType() == ImportJobContext.CertificateType.UPLOAD) {
                Assertions.assertEquals(keyPair.getPrivate(), certificateEntry.getPrivateKey());
            } else {
                Assertions.assertNull(certificateEntry.getPrivateKey());
            }
        });
    }

    @Test
    void itShouldThrowAnExceptionIfKeyEntryOnlyContainsPublicKey() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        ImportJobContext context = new ImportJobContext();

        X509Certificate keyWrapper =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Key Wrapper");

        FileOutputStream fileOutputStream = new FileOutputStream(TEST_KEYSTORE_FILENAME);
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(null, TEST_KEYSTORE_PASSWORD);
        keyStore.setCertificateEntry(TEST_KEYSTORE_ALIAS, keyWrapper);
        keyStore.store(fileOutputStream, TEST_KEYSTORE_PASSWORD);
        fileOutputStream.close();


        Assertions.assertThrows(ImportJobStepException.class, () -> mapPrivateKeyStep.exec(
            context, ImportJobContext.CertificateType.UPLOAD.toString(), TEST_KEYSTORE_FILENAME,
            String.valueOf(TEST_KEYSTORE_PASSWORD),
            TEST_KEYSTORE_ALIAS, String.valueOf(TEST_KEYSTORE_PASSWORD)));
    }

    @Test
    void itShouldThrowAnExceptionIfKeyEntryDoesNotExists() throws Exception {
        ImportJobContext context = new ImportJobContext();

        FileOutputStream fileOutputStream = new FileOutputStream(TEST_KEYSTORE_FILENAME);
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(null, TEST_KEYSTORE_PASSWORD);
        keyStore.store(fileOutputStream, TEST_KEYSTORE_PASSWORD);
        fileOutputStream.close();

        Assertions.assertThrows(ImportJobStepException.class, () -> mapPrivateKeyStep.exec(
            context, ImportJobContext.CertificateType.UPLOAD.toString(), TEST_KEYSTORE_FILENAME,
            String.valueOf(TEST_KEYSTORE_PASSWORD),
            TEST_KEYSTORE_ALIAS, String.valueOf(TEST_KEYSTORE_PASSWORD)));
    }

    @Test
    void itShouldThrowAnExceptionIfKeyStorePasswordIsWrong() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate keyWrapper =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Key Wrapper");

        FileOutputStream fileOutputStream = new FileOutputStream(TEST_KEYSTORE_FILENAME);
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(null, TEST_KEYSTORE_PASSWORD);
        keyStore.setKeyEntry(TEST_KEYSTORE_ALIAS, keyPair.getPrivate(), TEST_KEYSTORE_PASSWORD,
            new X509Certificate[] {keyWrapper});
        keyStore.store(fileOutputStream, TEST_KEYSTORE_PASSWORD);
        fileOutputStream.close();

        ImportJobContext context = new ImportJobContext();
        Assertions.assertThrows(ImportJobStepException.class, () -> mapPrivateKeyStep.exec(
            context, ImportJobContext.CertificateType.UPLOAD.toString(), TEST_KEYSTORE_FILENAME,
            "wrongPassword",
            TEST_KEYSTORE_ALIAS, String.valueOf(TEST_KEYSTORE_PASSWORD)));
    }

    @Test
    void itShouldThrowAnExceptionIfKeyPasswordIsWrong() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate keyWrapper =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Key Wrapper");

        FileOutputStream fileOutputStream = new FileOutputStream(TEST_KEYSTORE_FILENAME);
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(null, TEST_KEYSTORE_PASSWORD);
        keyStore.setKeyEntry(TEST_KEYSTORE_ALIAS, keyPair.getPrivate(), TEST_KEYSTORE_PASSWORD,
            new X509Certificate[] {keyWrapper});
        keyStore.store(fileOutputStream, TEST_KEYSTORE_PASSWORD);
        fileOutputStream.close();

        ImportJobContext context = new ImportJobContext();

        Assertions.assertThrows(ImportJobStepException.class, () -> mapPrivateKeyStep.exec(
            context, ImportJobContext.CertificateType.UPLOAD.toString(), TEST_KEYSTORE_FILENAME,
            String.valueOf(TEST_KEYSTORE_PASSWORD),
            TEST_KEYSTORE_ALIAS, "wrongPassword"));
    }

    @Test
    void itShouldThrowAnExceptionIfKeyStoreIsInvalid() throws Exception {
        ImportJobContext context = new ImportJobContext();

        Random random = new Random();
        byte[] dummyData = new byte[512];
        random.nextBytes(dummyData);

        FileOutputStream fileOutputStream = new FileOutputStream(TEST_KEYSTORE_FILENAME);
        fileOutputStream.write(dummyData);
        fileOutputStream.close();

        Assertions.assertThrows(ImportJobStepException.class, () -> mapPrivateKeyStep.exec(
            context, ImportJobContext.CertificateType.UPLOAD.toString(), TEST_KEYSTORE_FILENAME,
            String.valueOf(TEST_KEYSTORE_PASSWORD),
            TEST_KEYSTORE_ALIAS, String.valueOf(TEST_KEYSTORE_PASSWORD)));
    }

    private void addCertificate(ImportJobContext context, X509Certificate certificate,
                                ImportJobContext.CertificateType certificateType)
        throws CertificateEncodingException, IOException {
        context.getParsedCertificates().add(new ImportJobContext.CertificateEntry(
            certificateUtils.convertCertificate(certificate),
            certificate,
            null,
            certificate.getEncoded(),
            certificateUtils.getCertThumbprint(certificate),
            null,
            TEST_COUNTRY_CODE,
            certificateType,
            null));
    }

}
