package int_.who.tng.dataimport.config;

import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.config.DccConfigProperties;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TrustAnchorConfig {

    @Qualifier("trustAnchor")
    private final KeyStore trustAnchorKeyStore;

    private final DccConfigProperties dccConfigProperties;

    private final CertificateUtils certificateUtils;

    @Qualifier("trustAnchor")
    @Bean
    public PrivateKey trustAnchorPrivateKey() {
        final String alias = dccConfigProperties.getTrustAnchor().getCertificateAlias();
        final String password = dccConfigProperties.getTrustAnchor().getKeyStorePass();

        try {
            if (trustAnchorKeyStore.getKey(alias, password.toCharArray()) instanceof PrivateKey privateKey) {
                log.info("Successfully loaded TrustAnchor PrivateKey from KeyStore: {}", privateKey.getAlgorithm());
                return privateKey;
            } else {
                log.error("TrustAnchor PrivateKey ist not a valid PrivateKey");
                throw new BeanCreationException("Initialization failed");
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            log.error("Failed to load TrustAnchor PrivateKey from KeyStore.", e);
            throw new BeanCreationException("Initialization failed");
        }
    }

    @Qualifier("trustAnchor")
    @Bean
    public X509CertificateHolder trustAnchorCertificate() {
        final String alias = dccConfigProperties.getTrustAnchor().getCertificateAlias();
        try {
            if (trustAnchorKeyStore.getCertificate(alias) instanceof X509Certificate x509Certificate) {
                log.info("Successfully loaded TrustAnchor Certificate from KeyStore: {}",
                    x509Certificate.getSubjectX500Principal());
                return certificateUtils.convertCertificate(x509Certificate);
            } else {
                log.error("TrustAnchor Certificate ist not a valid X509Certificate");
                throw new BeanCreationException("Initialization failed");
            }
        } catch (KeyStoreException | CertificateEncodingException | IOException e) {
            log.error("Failed to load TrustAnchor Certificate from KeyStore.", e);
            throw new BeanCreationException("Initialization failed");
        }
    }
}
