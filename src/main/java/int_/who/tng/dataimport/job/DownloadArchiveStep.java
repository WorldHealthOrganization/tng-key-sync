package int_.who.tng.dataimport.job;

import eu.europa.ec.dgc.signing.SignedByteArrayMessageParser;
import eu.europa.ec.dgc.signing.SignedMessageParser;
import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.client.DccPublicationClient;
import int_.who.tng.dataimport.config.DccConfigProperties;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadArchiveStep {

    private final DccPublicationClient dccPublicationClient;

    private final CertificateUtils certificateUtils;

    private final DccConfigProperties dccConfigProperties;

    public byte[] exec() {
        byte[] publicationArchive = downloadArchive();
        byte[] publicationArchiveSignature = downloadSignature();
        verifySignature(publicationArchive, publicationArchiveSignature);

        return publicationArchive;
    }

    private byte[] downloadArchive() {
        log.info("Downloading DCC Publication Archive");
        try {
            byte[] response = dccPublicationClient.downloadPublicationArchive();
            log.info("Downloaded DCC Publication Archive. ({} bytes)", response.length);
            return response;
        } catch (Exception e) {
            log.error("Failed to download DCC Publication Archive", e);
            System.exit(1);
            return null;
        }
    }

    private byte[] downloadSignature() {
        log.info("Downloading DCC Publication Archive Signature");
        try {
            byte[] response = dccPublicationClient.downloadPublicationSignature();
            log.info("Downloaded DCC Publication Archive Signature. ({} bytes)", response.length);
            return response;
        } catch (Exception e) {
            log.error("Failed to download DCC Publication Archive Signature", e);
            System.exit(1);
            return null;
        }
    }

    private void verifySignature(byte[] archive, byte[] signature) {
        log.info("Verifying Signature of DCC Publication Archive");
        byte[] base64EncodedArchive = Base64.getEncoder().encode(archive);
        SignedByteArrayMessageParser messageParser = new SignedByteArrayMessageParser(signature, base64EncodedArchive);

        if (messageParser.getParserState() != SignedMessageParser.ParserState.SUCCESS) {
            log.error("Failed to parse Signature Message: {}", messageParser.getParserState());
            System.exit(1);
            return;
        }

        if (!messageParser.isSignatureVerified()) {
            log.error("Signature value is not matching downloaded DCC Publication Archive");
            System.exit(1);
            return;
        }

        if (dccConfigProperties.getPublication().getAllowedSigningCertThumbprints().isEmpty()) {
            log.warn(
                "Allow-List of DCC Publication Archive Signing Certificates is empty. Signing Certificate check is disabled.");
        } else {
            String signingCertHash = certificateUtils.getCertThumbprint(messageParser.getSigningCertificate());
            if (dccConfigProperties.getPublication().getAllowedSigningCertThumbprints()
                .stream()
                .noneMatch(thumbprint -> thumbprint.equalsIgnoreCase(signingCertHash))
            ) {
                log.error("Signing Certificate of Publication Archive is not on Allow-List. SigningCertHash: {}",
                    signingCertHash);
                System.exit(1);
                return;
            }
        }

        log.info("Verification of Signature of DCC Publication Archive was successful.");
    }
}
