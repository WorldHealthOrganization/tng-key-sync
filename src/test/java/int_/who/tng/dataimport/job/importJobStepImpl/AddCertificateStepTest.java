package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStepException;
import int_.who.tng.dataimport.testdata.CertificateTestUtils;
import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AddCertificateStepTest {

    @Autowired
    private AddCertificateStep addCertificateStep;

    @Autowired
    private CertificateUtils certificateUtils;

    private static final String TEST_CERT_COUNTRY_CODE = "DE";

    @Test
    void testCertificatesAreAdded() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();
        X509Certificate certificateUpload =
            CertificateTestUtils.generateCertificate(keyPair, TEST_CERT_COUNTRY_CODE, "Test Cert UPLOAD");
        X509Certificate certificateAuth =
            CertificateTestUtils.generateCertificate(keyPair, TEST_CERT_COUNTRY_CODE, "Test Cert AUTH");
        X509Certificate certificateCsca =
            CertificateTestUtils.generateCertificate(keyPair, TEST_CERT_COUNTRY_CODE, "Test Cert CSCA");
        X509Certificate certificateDsc =
            CertificateTestUtils.generateCertificate(keyPair, TEST_CERT_COUNTRY_CODE, "Test Cert DSC");

        ImportJobContext context = new ImportJobContext();

        addCertificateStep.exec(context, Base64.getEncoder().encodeToString(certificateUpload.getEncoded()), "UPLOAD");
        addCertificateStep.exec(context, Base64.getEncoder().encodeToString(certificateAuth.getEncoded()),
            "AUTHENTICATION");
        addCertificateStep.exec(context, Base64.getEncoder().encodeToString(certificateCsca.getEncoded()), "CSCA");
        addCertificateStep.exec(context, Base64.getEncoder().encodeToString(certificateDsc.getEncoded()), "DSC");

        Assertions.assertEquals(4, context.getParsedCertificates().size());
        for (ImportJobContext.CertificateEntry certificateEntry : context.getParsedCertificates()) {
            switch (certificateEntry.getCertificateType()) {
                case DSC ->
                    checkParsedCertificateEntry(certificateEntry, certificateDsc, ImportJobContext.CertificateType.DSC);
                case AUTHENTICATION -> checkParsedCertificateEntry(certificateEntry, certificateAuth,
                    ImportJobContext.CertificateType.AUTHENTICATION);
                case CSCA -> checkParsedCertificateEntry(certificateEntry, certificateCsca,
                    ImportJobContext.CertificateType.CSCA);
                case UPLOAD -> checkParsedCertificateEntry(certificateEntry, certificateUpload,
                    ImportJobContext.CertificateType.UPLOAD);
            }
        }
    }

    @Test
    void testCertificateWithoutCountryShouldThrowException() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();
        X509Certificate certificateUpload = CertificateTestUtils.generateCertificate(keyPair, null, "Test Cert UPLOAD");

        ImportJobContext context = new ImportJobContext();

        Assertions.assertThrows(ImportJobStepException.class, () -> addCertificateStep.exec(
            context, Base64.getEncoder().encodeToString(certificateUpload.getEncoded()),
            "UPLOAD"));

        Assertions.assertEquals(0, context.getParsedCertificates().size());
    }

    void checkParsedCertificateEntry(ImportJobContext.CertificateEntry certificateEntry,
                                     X509Certificate x509Certificate,
                                     ImportJobContext.CertificateType certificateType)
        throws CertificateEncodingException, IOException {
        Assertions.assertEquals(certificateType, certificateEntry.getCertificateType());
        Assertions.assertEquals(x509Certificate, certificateEntry.getParsedCertificate());
        Assertions.assertEquals(certificateUtils.convertCertificate(x509Certificate),
            certificateEntry.getParsedCertificateHolder());
        Assertions.assertEquals(TEST_CERT_COUNTRY_CODE, certificateEntry.getCountry());
        Assertions.assertEquals(certificateUtils.getCertThumbprint(x509Certificate), certificateEntry.getThumbprint());
        Assertions.assertArrayEquals(x509Certificate.getEncoded(), certificateEntry.getRawCertificate());
        Assertions.assertNull(certificateEntry.getSignature());
        Assertions.assertNull(certificateEntry.getPrivateKey());

    }
}
