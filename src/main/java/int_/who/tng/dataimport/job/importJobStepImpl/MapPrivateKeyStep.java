package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("MapPrivateKey")
@RequiredArgsConstructor
@Slf4j
public class MapPrivateKeyStep implements ImportJobStep {

    @Override
    public void exec(ImportJobContext context, String... args) {
        ImportJobContext.CertificateType certificateType = ImportJobContext.CertificateType.valueOf(args[0]);
        String keyStorePath = args[1];
        String keyStorePassword = args[2];
        String keyStoreKeyAlias = args[3];
        String keyStoreKeyPassword = args[4];

        log.info("Trying to map given PrivateKey to parsed Certificates");

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            loadKeyStore(keyStore, keyStorePath, keyStorePassword.toCharArray());
            final PrivateKey privateKey =
                (PrivateKey) keyStore.getKey(keyStoreKeyAlias, keyStoreKeyPassword.toCharArray());

            log.info("Generating Random Signature with PrivateKey");
            String signatureAlgorithm = privateKey instanceof RSAPrivateKey ? "NoneWithRSA" : "NoneWithECDSA";

            byte[] dummyData = new byte[32];
            Random random = new Random();
            random.nextBytes(dummyData);
            Signature signer =
                Signature.getInstance(signatureAlgorithm);
            signer.initSign(privateKey);
            signer.update(dummyData);
            final byte[] signature = signer.sign();


            context.getParsedCertificates().stream()
                .filter(certificateEntry -> certificateEntry.getCertificateType() == certificateType)
                .filter(certificateEntry -> {
                    // Check if Signed Data can be verified by PublicKey of Certificate
                    try {
                        PublicKey publicKey = certificateEntry.getParsedCertificate().getPublicKey();
                        Signature verifier = Signature.getInstance(signatureAlgorithm);
                        verifier.initVerify(publicKey);
                        return verifier.verify(signature);
                    } catch (NoSuchAlgorithmException e) {
                        log.error("Failed to initialize Signature Verifier", e);
                        System.exit(1);
                        return false;
                    } catch (InvalidKeyException | SignatureException e) {
                        return false;
                    }
                })
                .forEach(certificateEntry -> certificateEntry.setPrivateKey(privateKey));
        } catch (NoSuchAlgorithmException | SignatureException e) {
            log.error("Failed to create Dummy Signature.", e);
            System.exit(1);
        } catch (KeyStoreException | UnrecoverableKeyException e) {
            log.error("Failed to load PrivateKey from KeyStore.", e);
            System.exit(1);
        } catch (Exception e) {
            log.error("Failed to load KeyStore: {}", keyStorePath, e);
            System.exit(1);
        }

        log.info("Finished mapping PrivateKey to parsed Certificates");
    }

    private void loadKeyStore(KeyStore keyStore, String path, char[] password) throws Exception {
        try (InputStream fileStream = getStream(path)) {
            keyStore.load(fileStream, password);
        } catch (Exception e) {
            log.error("Could not load Keystore {}", path);
            throw e;
        }
    }

    private InputStream getStream(String path) throws FileNotFoundException {
        if (path.startsWith("classpath:")) {
            String resourcePath = path.substring(10);
            return getClass().getClassLoader().getResourceAsStream(resourcePath);
        } else {
            File file = new File(path);
            return file.exists() ? new FileInputStream(path) : null;
        }
    }
}
