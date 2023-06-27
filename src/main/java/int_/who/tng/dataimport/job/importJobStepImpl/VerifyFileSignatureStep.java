package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.signing.SignedByteArrayMessageParser;
import eu.europa.ec.dgc.signing.SignedMessageParser;
import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("VerifyFileSignature")
@RequiredArgsConstructor
@Slf4j
public class VerifyFileSignatureStep implements ImportJobStep {

    private final CertificateUtils certificateUtils;

    @Override
    public void exec(ImportJobContext context, String... args) {
        String fileName = args[0];
        String signatureFileName = args[1];
        List<String> expectedSignerCertThumbprints = Arrays.stream(args)
            .skip(2)
            .toList();

        log.info("Verifying Signature of File {} with {}", fileName, signatureFileName);

        verifySignature(
            context.getFiles().get(fileName),
            context.getFiles().get(signatureFileName),
            expectedSignerCertThumbprints);

        log.info("Verification of Signature of {} was successful.", fileName);
    }


    private void verifySignature(byte[] file, byte[] signature, List<String> allowedThumbprints) {
        byte[] base64EncodedFile = Base64.getEncoder().encode(file);
        SignedByteArrayMessageParser messageParser = new SignedByteArrayMessageParser(signature, base64EncodedFile);

        if (messageParser.getParserState() != SignedMessageParser.ParserState.SUCCESS) {
            log.error("Failed to parse Signature Message: {}", messageParser.getParserState());
            System.exit(1);
            return;
        }

        if (!messageParser.isSignatureVerified()) {
            log.error("Signature digest value is not matching provided file.");
            System.exit(1);
            return;
        }

        if (allowedThumbprints.isEmpty()) {
            log.warn(
                "Allow-List of Signing Certificates is empty. Signing Certificate check is disabled.");
        } else {
            String signingCertHash = certificateUtils.getCertThumbprint(messageParser.getSigningCertificate());
            if (allowedThumbprints
                .stream()
                .noneMatch(thumbprint -> thumbprint.equalsIgnoreCase(signingCertHash))
            ) {
                log.error("Signing Certificate of File is not on Allow-List. SigningCertHash: {}",
                    signingCertHash);
                System.exit(1);
            }
        }
    }
}
