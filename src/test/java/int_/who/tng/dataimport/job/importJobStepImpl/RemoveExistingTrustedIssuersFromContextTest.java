package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.entity.TrustedIssuerEntity;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.repository.TrustedIssuerRepository;
import int_.who.tng.dataimport.testdata.TrustedIssuerTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RemoveExistingTrustedIssuersFromContextTest {

    @Autowired
    RemoveExistingIssuersFromContextStep removeExistingIssuersFromContextStep;

    @MockBean
    TrustedIssuerRepository trustedIssuerRepositoryMock;

    private static final String TEST_COUNTRY_CODE_ALPHA_3 = "XXA";

    private static final String TEST_RACSEL_DDVC_DOMAIN = "RACSEL-DDVC";

    @Test
    void testExistingTrustedIssuerRemovedFromContext() {
        //Given we have more then 1 JSON file containing a trusted issuer of test country XA in import context
        ImportJobContext context = new ImportJobContext();
        ImportJobContext.TrustedIssuerEntry trustedIssuerEntry = TrustedIssuerTestUtils.getTrustedIssuerEntry(TEST_COUNTRY_CODE_ALPHA_3, TEST_RACSEL_DDVC_DOMAIN);
        ImportJobContext.TrustedIssuerEntry trustedIssuerEntry2 = TrustedIssuerTestUtils.getTrustedIssuerEntry(TEST_COUNTRY_CODE_ALPHA_3, TEST_RACSEL_DDVC_DOMAIN);
        ImportJobContext.TrustedIssuerEntry trustedIssuerEntry3 = TrustedIssuerTestUtils.getTrustedIssuerEntry(TEST_COUNTRY_CODE_ALPHA_3, TEST_RACSEL_DDVC_DOMAIN);

        trustedIssuerEntry2.setHash("123");
        trustedIssuerEntry3.setHash("456");

        context.getParsedIssuers().add(trustedIssuerEntry);
        context.getParsedIssuers().add(trustedIssuerEntry2);
        context.getParsedIssuers().add(trustedIssuerEntry3);

        when(trustedIssuerRepositoryMock.getFirstByThumbprint(
                trustedIssuerEntry.getHash()))
                .thenReturn(Optional.empty());

        when(trustedIssuerRepositoryMock.getFirstByThumbprint(
                trustedIssuerEntry2.getHash()))
                .thenReturn(Optional.of(new TrustedIssuerEntity()));

        when(trustedIssuerRepositoryMock.getFirstByThumbprint(
                trustedIssuerEntry3.getHash()))
                .thenReturn(Optional.of(new TrustedIssuerEntity()));

        removeExistingIssuersFromContextStep.exec(context);

        verify(trustedIssuerRepositoryMock, times(3)).getFirstByThumbprint(any());
        Assertions.assertEquals(1, context.getParsedIssuers().size());
        Assertions.assertEquals(trustedIssuerEntry, context.getParsedIssuers().get(0));

    }


}
