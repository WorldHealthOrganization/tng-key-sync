package int_.who.tng.dataimport.job.importJobStepImpl;


import int_.who.tng.dataimport.entity.TrustedIssuerEntity;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.repository.TrustedIssuerRepository;
import int_.who.tng.dataimport.testdata.TrustedIssuerTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SaveTrustedIssuersInDbStep.class)
public class SaveTrustedIssuersInDbStepTest {

    @Autowired
    SaveTrustedIssuersInDbStep saveTrustedIssuersInDbStep;

    @MockBean
    TrustedIssuerRepository trustedIssuerRepositoryMock;

    private static final String TEST_COUNTRY_CODE_ALPHA_3 = "XXA";

    private static final String TEST_RACSEL_DDVC_DOMAIN = "RACSEL-DDVC";

    @Test
    void testSaveTrustedIssuer() {
        //Given we have a JSON file containing a trusted issuer in import context
        ImportJobContext context = new ImportJobContext();
        ImportJobContext.TrustedIssuerEntry trustedIssuerEntry = TrustedIssuerTestUtils.getTrustedIssuerEntry(TEST_COUNTRY_CODE_ALPHA_3, TEST_RACSEL_DDVC_DOMAIN);
        context.getParsedIssuers().add(trustedIssuerEntry);

        ArgumentCaptor<TrustedIssuerEntity> trustedIssuerEntityArgumentCaptor =
                ArgumentCaptor.forClass(TrustedIssuerEntity.class);

        when(trustedIssuerRepositoryMock.save(trustedIssuerEntityArgumentCaptor.capture()))
            .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveTrustedIssuersInDbStep.exec(context);

        verify(trustedIssuerRepositoryMock, times(1)).save(any());


        TrustedIssuerEntity firstTrustedIssuerEntity =
                trustedIssuerEntityArgumentCaptor.getValue();
        Assertions.assertEquals(trustedIssuerEntry.getName(), firstTrustedIssuerEntity.getName());
        Assertions.assertEquals(trustedIssuerEntry.getUrl(), firstTrustedIssuerEntity.getUrl());
        Assertions.assertEquals(TrustedIssuerEntity.UrlType.valueOf(trustedIssuerEntry.getUrlType()), firstTrustedIssuerEntity.getUrlType());
        Assertions.assertEquals(trustedIssuerEntry.getHash(), firstTrustedIssuerEntity.getThumbprint());
        Assertions.assertEquals(trustedIssuerEntry.getSslPublicKeys().get(0), firstTrustedIssuerEntity.getSslPublicKey());
        Assertions.assertEquals(trustedIssuerEntry.getSignature(), firstTrustedIssuerEntity.getSignature());
        Assertions.assertEquals(trustedIssuerEntry.getCountry(), firstTrustedIssuerEntity.getCountry());
        Assertions.assertNotEquals(TEST_COUNTRY_CODE_ALPHA_3, firstTrustedIssuerEntity.getCountry());
        Assertions.assertEquals(TEST_RACSEL_DDVC_DOMAIN, firstTrustedIssuerEntity.getDomain());

    }

    @Test
    void testSaveTrustedIssuerSkippedOnMissingSignature() {
        //Given we have 1 JSON file containing a trusted issuer in import context
        ImportJobContext context = new ImportJobContext();
        ImportJobContext.TrustedIssuerEntry trustedIssuerEntry = TrustedIssuerTestUtils.getTrustedIssuerEntry(TEST_COUNTRY_CODE_ALPHA_3, TEST_RACSEL_DDVC_DOMAIN);
        context.getParsedIssuers().add(trustedIssuerEntry);

        //And the trusted issuer is not compliant due to missing signature
        trustedIssuerEntry.setSignature(null);


        ArgumentCaptor<TrustedIssuerEntity> trustedIssuerEntityArgumentCaptor =
                ArgumentCaptor.forClass(TrustedIssuerEntity.class);

        when(trustedIssuerRepositoryMock.save(trustedIssuerEntityArgumentCaptor.capture()))
                .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveTrustedIssuersInDbStep.exec(context);

        //then nothing is saved
        verify(trustedIssuerRepositoryMock, times(0)).save(any());
    }

    @Test
    void testSaveTrustedIssuerSkippedOnInvalidCountryCode() {
        //Given we have 1 JSON file containing a trusted issuer in import context
        ImportJobContext context = new ImportJobContext();
        ImportJobContext.TrustedIssuerEntry trustedIssuerEntry = TrustedIssuerTestUtils.getTrustedIssuerEntry(TEST_COUNTRY_CODE_ALPHA_3, TEST_RACSEL_DDVC_DOMAIN);
        context.getParsedIssuers().add(trustedIssuerEntry);

        //And the trusted issuer is not compliant due to invalid country code
        trustedIssuerEntry.setCountry("XXA");

        ArgumentCaptor<TrustedIssuerEntity> trustedIssuerEntityArgumentCaptor =
                ArgumentCaptor.forClass(TrustedIssuerEntity.class);

        when(trustedIssuerRepositoryMock.save(trustedIssuerEntityArgumentCaptor.capture()))
                .thenAnswer(invocation -> (invocation.getArgument(0)));

        saveTrustedIssuersInDbStep.exec(context);

        //then nothing is saved
        verify(trustedIssuerRepositoryMock, times(0)).save(any());
    }

}
