package int_.who.tng.dataimport.job.importJobStepImpl;

import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import int_.who.tng.dataimport.job.ImportJobStepException;
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
    public void exec(ImportJobContext context, String... args) throws ImportJobStepException {
        ImportJobContext.CertificateType certificateType = ImportJobContext.CertificateType.valueOf(args[0]);
        String keyStorePath = args[1];
        String keyStorePassword = args[2];
        String keyStoreKeyAlias = args[3];
        String keyStoreKeyPassword = args[4];

        log.debug("Trying to map given PrivateKey to parsed Certificates");

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            loadKeyStore(keyStore, keyStorePath, keyStorePassword.toCharArray());
            final PrivateKey privateKey =
                (PrivateKey) keyStore.getKey(keyStoreKeyAlias, keyStoreKeyPassword.toCharArray());

            log.debug("Generating Random Signature with PrivateKey");
            String signatureAlgorithm = privateKey instanceof RSAPrivateKey ? "NoneWithRSA" : "NoneWithECDSA";

            byte[] dummyData = new byte[32];
            Random random = new Random();
            random.nextBytes(dummyData);
            Signature signer =
                Signature.getInstance(signatureAlgorithm);
            signer.initSign(privateKey);
            signer.update(dummyData);
            final byte[] signature = signer.sign();

            Signature verifier = Signature.getInstance(signatureAlgorithm);

            context.getParsedCertificates().stream()
                .filter(certificateEntry -> certificateEntry.getCertificateType() == certificateType)
                .filter(certificateEntry -> {
                    // Check if Signed Data can be verified by PublicKey of Certificate
                    try {
                        PublicKey publicKey = certificateEntry.getParsedCertificate().getPublicKey();
                        verifier.initVerify(publicKey);
                        verifier.update(dummyData);
                        return verifier.verify(signature);
                    } catch (InvalidKeyException | SignatureException e) {
                        return false;
                    }
                })
                .forEach(certificateEntry -> certificateEntry.setPrivateKey(privateKey));
        } catch (NoSuchAlgorithmException | SignatureException e) {
            throw new ImportJobStepException(true, "Failed to create initialize Verifier/Signer: " + e.getMessage());
        } catch (KeyStoreException | UnrecoverableKeyException e) {
            throw new ImportJobStepException(true, "Failed to load PrivateKey from KeyStore: " + e.getMessage());
        } catch (Exception e) {
            throw new ImportJobStepException(true, "Failed to load KeyStore " + keyStorePath + ": " + e.getMessage());
        }

        log.debug("Finished mapping PrivateKey to parsed Certificates");
    }

    private void loadKeyStore(KeyStore keyStore, String path, char[] password) throws Exception {
        try (InputStream fileStream = getStream(path)) {
            keyStore.load(fileStream, password);
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
