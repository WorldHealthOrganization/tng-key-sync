package int_.who.tng.dataimport.job.importJobStepImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.entity.SignerInformationEntity;
import int_.who.tng.dataimport.entity.TrustedPartyEntity;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.repository.SignerInformationRepository;
import int_.who.tng.dataimport.repository.TrustedPartyRepository;
import int_.who.tng.dataimport.testdata.CertificateTestUtils;
import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class RemoveExistingCertificatesFromContextTest {

    @Autowired
    RemoveExistingCertificatesFromContextStep removeExistingCertificatesFromContextStep;

    @Autowired
    CertificateUtils certificateUtils;

    @MockBean
    SignerInformationRepository signerInformationRepositoryMock;

    @MockBean
    TrustedPartyRepository trustedPartyRepositoryMock;

    private static final String TEST_COUNTRY_CODE = "DE";

    @ParameterizedTest
    @ValueSource(strings = {"UPLOAD", "CSCA", "AUTHENTICATION"})
    void testExistingCertificatesAreRemovedTrustedParty(String type) throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 3");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.valueOf(type));
        addCertificate(context, certificate2, ImportJobContext.CertificateType.valueOf(type));
        addCertificate(context, certificate3, ImportJobContext.CertificateType.valueOf(type));

        when(trustedPartyRepositoryMock.getFirstByThumbprint(
            eq(certificateUtils.getCertThumbprint(certificate1))))
            .thenReturn(Optional.empty());

        when(trustedPartyRepositoryMock.getFirstByThumbprint(
            eq(certificateUtils.getCertThumbprint(certificate2))))
            .thenReturn(Optional.of(new TrustedPartyEntity()));

        when(trustedPartyRepositoryMock.getFirstByThumbprint(
            eq(certificateUtils.getCertThumbprint(certificate3))))
            .thenReturn(Optional.of(new TrustedPartyEntity()));

        removeExistingCertificatesFromContextStep.exec(context, type);

        verify(trustedPartyRepositoryMock, times(3)).getFirstByThumbprint(any());
        verify(signerInformationRepositoryMock, never()).getFirstByThumbprint(any());

        Assertions.assertEquals(1, context.getParsedCertificates().size());
        Assertions.assertEquals(certificate1, context.getParsedCertificates().get(0).getParsedCertificate());
    }

    @Test
    void testExistingCertificatesAreRemovedDSC() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 3");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.DSC);
        addCertificate(context, certificate2, ImportJobContext.CertificateType.DSC);
        addCertificate(context, certificate3, ImportJobContext.CertificateType.DSC);

        when(signerInformationRepositoryMock.getFirstByThumbprint(
            eq(certificateUtils.getCertThumbprint(certificate1))))
            .thenReturn(Optional.empty());

        when(signerInformationRepositoryMock.getFirstByThumbprint(
            eq(certificateUtils.getCertThumbprint(certificate2))))
            .thenReturn(Optional.of(new SignerInformationEntity()));

        when(signerInformationRepositoryMock.getFirstByThumbprint(
            eq(certificateUtils.getCertThumbprint(certificate3))))
            .thenReturn(Optional.of(new SignerInformationEntity()));

        removeExistingCertificatesFromContextStep.exec(context, ImportJobContext.CertificateType.DSC.toString());

        verify(signerInformationRepositoryMock, times(3)).getFirstByThumbprint(any());
        verify(trustedPartyRepositoryMock, never()).getFirstByThumbprint(any());

        Assertions.assertEquals(1, context.getParsedCertificates().size());
        Assertions.assertEquals(certificate1, context.getParsedCertificates().get(0).getParsedCertificate());
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
            certificateType, null));
    }
}
