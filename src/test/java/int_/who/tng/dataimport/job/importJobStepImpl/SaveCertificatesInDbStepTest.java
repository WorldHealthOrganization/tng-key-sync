package int_.who.tng.dataimport.job.importJobStepImpl;

import static org.mockito.ArgumentMatchers.any;
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
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class SaveCertificatesInDbStepTest {

    @Autowired
    SaveCertificatesInDbStep saveCertificatesInDbStep;

    @Autowired
    CertificateUtils certificateUtils;

    @MockBean
    SignerInformationRepository signerInformationRepositoryMock;

    @MockBean
    TrustedPartyRepository trustedPartyRepositoryMock;

    private static final String TEST_COUNTRY_CODE = "DE";
    private static final String TEST_DUMMY_SIGNATURE = "dummy-ta-signature";
    
    private static final String TEST_DCC_DOMAIN = "DCC";

    private static final String TEST_RACSEL_DDVC_DOMAIN = "RACSEL-DDVC";

    @Test
    void testSaveDSC() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 3");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.DSC, TEST_DUMMY_SIGNATURE,
            TEST_COUNTRY_CODE);
        addCertificate(context, certificate2, ImportJobContext.CertificateType.DSC, TEST_DUMMY_SIGNATURE,
            TEST_COUNTRY_CODE);
        addCertificate(context, certificate3, ImportJobContext.CertificateType.AUTHENTICATION, TEST_DUMMY_SIGNATURE,
            TEST_COUNTRY_CODE);

        ArgumentCaptor<SignerInformationEntity> signerInformationEntityArgumentCaptor =
            ArgumentCaptor.forClass(SignerInformationEntity.class);

        when(signerInformationRepositoryMock.save(signerInformationEntityArgumentCaptor.capture()))
            .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveCertificatesInDbStep.exec(context, ImportJobContext.CertificateType.DSC.toString());

        verify(signerInformationRepositoryMock, times(2)).save(any());
        verify(trustedPartyRepositoryMock, never()).save(any());

        SignerInformationEntity firstSignerInformationEntity =
            signerInformationEntityArgumentCaptor.getAllValues().get(0);
        Assertions.assertEquals(Base64.getEncoder().encodeToString(certificate1.getEncoded()),
            firstSignerInformationEntity.getRawData());
        Assertions.assertEquals(certificateUtils.getCertThumbprint(certificate1),
            firstSignerInformationEntity.getThumbprint());
        Assertions.assertEquals(TEST_COUNTRY_CODE, firstSignerInformationEntity.getCountry());
        Assertions.assertEquals(TEST_DUMMY_SIGNATURE, firstSignerInformationEntity.getSignature());
        Assertions.assertEquals(SignerInformationEntity.CertificateType.DSC,
            firstSignerInformationEntity.getCertificateType());
        Assertions.assertNull(firstSignerInformationEntity.getKid());
        Assertions.assertDoesNotThrow(() -> UUID.fromString(firstSignerInformationEntity.getUuid()));
        Assertions.assertEquals(TEST_DCC_DOMAIN, firstSignerInformationEntity.getDomain());


        SignerInformationEntity secondSignerInformationEntity =
            signerInformationEntityArgumentCaptor.getAllValues().get(1);
        Assertions.assertEquals(Base64.getEncoder().encodeToString(certificate2.getEncoded()),
            secondSignerInformationEntity.getRawData());
        Assertions.assertEquals(certificateUtils.getCertThumbprint(certificate2),
            secondSignerInformationEntity.getThumbprint());
        Assertions.assertEquals(TEST_COUNTRY_CODE, secondSignerInformationEntity.getCountry());
        Assertions.assertEquals(TEST_DUMMY_SIGNATURE, secondSignerInformationEntity.getSignature());
        Assertions.assertEquals(SignerInformationEntity.CertificateType.DSC,
            secondSignerInformationEntity.getCertificateType());
        Assertions.assertNull(secondSignerInformationEntity.getKid());
        Assertions.assertDoesNotThrow(() -> UUID.fromString(secondSignerInformationEntity.getUuid()));
        Assertions.assertEquals(TEST_DCC_DOMAIN, secondSignerInformationEntity.getDomain());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CSCA", "AUTHENTICATION", "UPLOAD", "DECA"})
    void testSaveTrustedParty(String type) throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 2");
        X509Certificate certificate3 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 3");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.valueOf(type), TEST_DUMMY_SIGNATURE,
            TEST_COUNTRY_CODE);
        addCertificate(context, certificate2, ImportJobContext.CertificateType.valueOf(type), TEST_DUMMY_SIGNATURE,
            TEST_COUNTRY_CODE);
        addCertificate(context, certificate3, ImportJobContext.CertificateType.DSC, TEST_DUMMY_SIGNATURE,
            TEST_COUNTRY_CODE);

        ArgumentCaptor<TrustedPartyEntity> trustedPartyEntityArgumentCaptor =
            ArgumentCaptor.forClass(TrustedPartyEntity.class);

        when(trustedPartyRepositoryMock.save(trustedPartyEntityArgumentCaptor.capture()))
            .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveCertificatesInDbStep.exec(context, type);

        verify(trustedPartyRepositoryMock, times(2)).save(any());
        verify(signerInformationRepositoryMock, never()).save(any());

        TrustedPartyEntity firstTrustedPartyEntity = trustedPartyEntityArgumentCaptor.getAllValues().get(0);
        Assertions.assertEquals(Base64.getEncoder().encodeToString(certificate1.getEncoded()),
            firstTrustedPartyEntity.getRawData());
        Assertions.assertEquals(certificateUtils.getCertThumbprint(certificate1),
            firstTrustedPartyEntity.getThumbprint());
        Assertions.assertEquals(TEST_COUNTRY_CODE, firstTrustedPartyEntity.getCountry());
        Assertions.assertEquals(TEST_DUMMY_SIGNATURE, firstTrustedPartyEntity.getSignature());
        Assertions.assertEquals(TrustedPartyEntity.CertificateType.valueOf(type),
            firstTrustedPartyEntity.getCertificateType());
        Assertions.assertNull(firstTrustedPartyEntity.getKid());
        Assertions.assertDoesNotThrow(() -> UUID.fromString(firstTrustedPartyEntity.getUuid()));
        Assertions.assertEquals(TEST_DCC_DOMAIN, firstTrustedPartyEntity.getDomain());

        TrustedPartyEntity secondTrustedPartyEntity = trustedPartyEntityArgumentCaptor.getAllValues().get(1);
        Assertions.assertEquals(Base64.getEncoder().encodeToString(certificate2.getEncoded()),
            secondTrustedPartyEntity.getRawData());
        Assertions.assertEquals(certificateUtils.getCertThumbprint(certificate2),
            secondTrustedPartyEntity.getThumbprint());
        Assertions.assertEquals(TEST_COUNTRY_CODE, secondTrustedPartyEntity.getCountry());
        Assertions.assertEquals(TEST_DUMMY_SIGNATURE, secondTrustedPartyEntity.getSignature());
        Assertions.assertEquals(TrustedPartyEntity.CertificateType.valueOf(type),
            secondTrustedPartyEntity.getCertificateType());
        Assertions.assertNull(secondTrustedPartyEntity.getKid());
        Assertions.assertDoesNotThrow(() -> UUID.fromString(secondTrustedPartyEntity.getUuid()));
        Assertions.assertEquals(TEST_DCC_DOMAIN, secondTrustedPartyEntity.getDomain());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CSCA", "AUTHENTICATION", "UPLOAD", "DECA"})
    void testSaveTrustedPartyWithNonDefaultDomain(String type) throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
                CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 1");

        ImportJobContext context = new ImportJobContext();
        addCertificateToContext(context, certificate1, ImportJobContext.CertificateType.valueOf(type), TEST_DUMMY_SIGNATURE,
                TEST_COUNTRY_CODE, TEST_RACSEL_DDVC_DOMAIN);

        ArgumentCaptor<TrustedPartyEntity> trustedPartyEntityArgumentCaptor =
                ArgumentCaptor.forClass(TrustedPartyEntity.class);

        when(trustedPartyRepositoryMock.save(trustedPartyEntityArgumentCaptor.capture()))
                .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveCertificatesInDbStep.exec(context, type);

        verify(trustedPartyRepositoryMock, times(1)).save(any());
        verify(signerInformationRepositoryMock, never()).save(any());

        TrustedPartyEntity firstTrustedPartyEntity = trustedPartyEntityArgumentCaptor.getAllValues().get(0);
        Assertions.assertEquals(TEST_RACSEL_DDVC_DOMAIN, firstTrustedPartyEntity.getDomain());

    }

    @ParameterizedTest
    @ValueSource(strings = {"CSCA", "AUTHENTICATION", "UPLOAD", "DECA"})
    void testItShouldNotInsertEntriesWithoutSignatureTrustedParty(String type) throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 2");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.valueOf(type),
            null, TEST_COUNTRY_CODE);
        addCertificate(context, certificate2, ImportJobContext.CertificateType.valueOf(type),
            TEST_DUMMY_SIGNATURE, TEST_COUNTRY_CODE);

        ArgumentCaptor<TrustedPartyEntity> trustedPartyEntityArgumentCaptor =
            ArgumentCaptor.forClass(TrustedPartyEntity.class);

        when(trustedPartyRepositoryMock.save(trustedPartyEntityArgumentCaptor.capture()))
            .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveCertificatesInDbStep.exec(context, type);

        verify(trustedPartyRepositoryMock, times(1)).save(any());
        verify(signerInformationRepositoryMock, never()).save(any());

        Assertions.assertEquals(Base64.getEncoder().encodeToString(certificate2.getEncoded()),
            trustedPartyEntityArgumentCaptor.getValue().getRawData());
        Assertions.assertEquals(certificateUtils.getCertThumbprint(certificate2),
            trustedPartyEntityArgumentCaptor.getValue().getThumbprint());
        Assertions.assertEquals(TEST_COUNTRY_CODE,
            trustedPartyEntityArgumentCaptor.getValue().getCountry());
        Assertions.assertEquals(TEST_DUMMY_SIGNATURE,
            trustedPartyEntityArgumentCaptor.getValue().getSignature());
        Assertions.assertEquals(TrustedPartyEntity.CertificateType.valueOf(type),
            trustedPartyEntityArgumentCaptor.getValue().getCertificateType());
    }

    @Test
    void testItShouldNotSaveDscWithoutSignature() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 2");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.DSC, null,
            TEST_COUNTRY_CODE);
        addCertificate(context, certificate2, ImportJobContext.CertificateType.DSC, TEST_DUMMY_SIGNATURE,
            TEST_COUNTRY_CODE);

        ArgumentCaptor<SignerInformationEntity> signerInformationEntityArgumentCaptor =
            ArgumentCaptor.forClass(SignerInformationEntity.class);

        when(signerInformationRepositoryMock.save(signerInformationEntityArgumentCaptor.capture()))
            .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveCertificatesInDbStep.exec(context, ImportJobContext.CertificateType.DSC.toString());

        verify(signerInformationRepositoryMock, times(1)).save(any());
        verify(trustedPartyRepositoryMock, never()).save(any());

        Assertions.assertEquals(Base64.getEncoder().encodeToString(certificate2.getEncoded()),
            signerInformationEntityArgumentCaptor.getValue().getRawData());
        Assertions.assertEquals(certificateUtils.getCertThumbprint(certificate2),
            signerInformationEntityArgumentCaptor.getValue().getThumbprint());
        Assertions.assertEquals(TEST_COUNTRY_CODE,
            signerInformationEntityArgumentCaptor.getValue().getCountry());
        Assertions.assertEquals(TEST_DUMMY_SIGNATURE,
            signerInformationEntityArgumentCaptor.getValue().getSignature());
        Assertions.assertEquals(SignerInformationEntity.CertificateType.DSC,
            signerInformationEntityArgumentCaptor.getValue().getCertificateType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CSCA", "AUTHENTICATION", "UPLOAD", "DECA"})
    void testItShouldNotInsertEntriesWithoutCountryAttributeTrustedParty(String type) throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, null, "Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 2");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.valueOf(type),
            TEST_DUMMY_SIGNATURE, null);
        addCertificate(context, certificate2, ImportJobContext.CertificateType.valueOf(type),
            TEST_DUMMY_SIGNATURE, TEST_COUNTRY_CODE);

        ArgumentCaptor<TrustedPartyEntity> trustedPartyEntityArgumentCaptor =
            ArgumentCaptor.forClass(TrustedPartyEntity.class);

        when(trustedPartyRepositoryMock.save(trustedPartyEntityArgumentCaptor.capture()))
            .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveCertificatesInDbStep.exec(context, type);

        verify(trustedPartyRepositoryMock, times(1)).save(any());
        verify(signerInformationRepositoryMock, never()).save(any());

        Assertions.assertEquals(Base64.getEncoder().encodeToString(certificate2.getEncoded()),
            trustedPartyEntityArgumentCaptor.getValue().getRawData());
        Assertions.assertEquals(certificateUtils.getCertThumbprint(certificate2),
            trustedPartyEntityArgumentCaptor.getValue().getThumbprint());
        Assertions.assertEquals(TEST_COUNTRY_CODE,
            trustedPartyEntityArgumentCaptor.getValue().getCountry());
        Assertions.assertEquals(TEST_DUMMY_SIGNATURE,
            trustedPartyEntityArgumentCaptor.getValue().getSignature());
        Assertions.assertEquals(TrustedPartyEntity.CertificateType.valueOf(type),
            trustedPartyEntityArgumentCaptor.getValue().getCertificateType());
    }

    @Test
    void testItShouldNotSaveDscWithoutCountryAttribute() throws Exception {
        KeyPair keyPair = CertificateTestUtils.generateKeyPair();

        X509Certificate certificate1 =
            CertificateTestUtils.generateCertificate(keyPair, null, "Testcert 1");
        X509Certificate certificate2 =
            CertificateTestUtils.generateCertificate(keyPair, TEST_COUNTRY_CODE, "Testcert 2");

        ImportJobContext context = new ImportJobContext();
        addCertificate(context, certificate1, ImportJobContext.CertificateType.DSC, TEST_DUMMY_SIGNATURE,
            null);
        addCertificate(context, certificate2, ImportJobContext.CertificateType.DSC, TEST_DUMMY_SIGNATURE,
            TEST_COUNTRY_CODE);

        ArgumentCaptor<SignerInformationEntity> signerInformationEntityArgumentCaptor =
            ArgumentCaptor.forClass(SignerInformationEntity.class);

        when(signerInformationRepositoryMock.save(signerInformationEntityArgumentCaptor.capture()))
            .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveCertificatesInDbStep.exec(context, ImportJobContext.CertificateType.DSC.toString());

        verify(signerInformationRepositoryMock, times(1)).save(any());
        verify(trustedPartyRepositoryMock, never()).save(any());

        Assertions.assertEquals(Base64.getEncoder().encodeToString(certificate2.getEncoded()),
            signerInformationEntityArgumentCaptor.getValue().getRawData());
        Assertions.assertEquals(certificateUtils.getCertThumbprint(certificate2),
            signerInformationEntityArgumentCaptor.getValue().getThumbprint());
        Assertions.assertEquals(TEST_COUNTRY_CODE,
            signerInformationEntityArgumentCaptor.getValue().getCountry());
        Assertions.assertEquals(TEST_DUMMY_SIGNATURE,
            signerInformationEntityArgumentCaptor.getValue().getSignature());
        Assertions.assertEquals(SignerInformationEntity.CertificateType.DSC,
            signerInformationEntityArgumentCaptor.getValue().getCertificateType());
    }

    private void addCertificate(ImportJobContext context, X509Certificate certificate,
                                ImportJobContext.CertificateType certificateType, String signature, String countryCode)
        throws CertificateEncodingException, IOException {
        context.getParsedCertificates().add(new ImportJobContext.CertificateEntry(
            certificateUtils.convertCertificate(certificate),
            certificate,
            null,
            certificate.getEncoded(),
            certificateUtils.getCertThumbprint(certificate),
            signature,
            countryCode,
            certificateType, null));
    }

    private void addCertificateToContext(ImportJobContext context, X509Certificate certificate,
                                ImportJobContext.CertificateType certificateType, String signature, String countryCode, String domain)
            throws CertificateEncodingException, IOException {
        context.getParsedCertificates().add(new ImportJobContext.CertificateEntry(
                certificateUtils.convertCertificate(certificate),
                certificate,
                null,
                certificate.getEncoded(),
                certificateUtils.getCertThumbprint(certificate),
                signature,
                countryCode,
                certificateType,
                domain));
    }
}
