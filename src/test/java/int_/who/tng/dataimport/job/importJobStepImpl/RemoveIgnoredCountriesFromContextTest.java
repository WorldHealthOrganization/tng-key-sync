package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.testdata.CertificateTestUtils;
import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RemoveIgnoredCountriesFromContextTest {

    @Autowired
    RemoveIgnoredCountriesStep removeIgnoredCountriesStep;

    @Autowired
    CertificateUtils certificateUtils;

    private static final String TEST_COUNTRY_CODE_1 = "AA";
    private static final String TEST_COUNTRY_CODE_2 = "BB";
    private static final String TEST_COUNTRY_CODE_3 = "CC";

    @Test
    void testCountriesAreRemoved() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE_1, "Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE_2, "Testcert 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE_3, "Testcert 3");
        X509Certificate certificate4 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE_3, "Testcert 4");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.DSC, TEST_COUNTRY_CODE_1);
        addCertificate(context, certificate2, ImportJobContext.CertificateType.UPLOAD, TEST_COUNTRY_CODE_2);
        addCertificate(context, certificate3, ImportJobContext.CertificateType.AUTHENTICATION, TEST_COUNTRY_CODE_3);
        addCertificate(context, certificate4, ImportJobContext.CertificateType.CSCA, TEST_COUNTRY_CODE_3);

        removeIgnoredCountriesStep.exec(context, TEST_COUNTRY_CODE_1, TEST_COUNTRY_CODE_3);

        Assertions.assertEquals(1, context.getParsedCertificates().size());
        Assertions.assertEquals(certificate2, context.getParsedCertificates().get(0).getParsedCertificate());
    }

    private void addCertificate(ImportJobContext context, X509Certificate certificate,
                                ImportJobContext.CertificateType certificateType, String countryCode)
        throws CertificateEncodingException, IOException {
        context.getParsedCertificates().add(new ImportJobContext.CertificateEntry(
            certificateUtils.convertCertificate(certificate),
            certificate,
            null,
            certificate.getEncoded(),
            certificateUtils.getCertThumbprint(certificate),
            null,
            countryCode,
            certificateType, null));
    }
}
