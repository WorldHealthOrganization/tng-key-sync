package int_.who.tng.dataimport.job;

import eu.europa.ec.dgc.utils.CertificateUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParseCertificatesStep {

    private final CertificateUtils certificateUtils;

    public List<ArchiveCertificateEntry> exec(List<ExtractArchiveStep.ArchivePemEntry> pemEntries) {
        log.info("Parsing PEMs into X509Certificate Objects");

        List<ArchiveCertificateEntry> certificateEntries = pemEntries.stream()
            .map(this::convert)
            .collect(Collectors.toList());

        log.info("Finished parsing PEMs into X509Certificate Objects. {} of {} entries were converted successfuly.",
            certificateEntries.size(), pemEntries.size());

        return certificateEntries;
    }

    private ArchiveCertificateEntry convert(ExtractArchiveStep.ArchivePemEntry pemEntry) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");

            Certificate cert = certificateFactory.generateCertificate(
                new ByteArrayInputStream(pemEntry.pem().getBytes(StandardCharsets.UTF_8)));

            if (cert instanceof X509Certificate) {
                return new ArchiveCertificateEntry(
                    pemEntry.country(),
                    certificateUtils.convertCertificate((X509Certificate)  cert),
                    pemEntry.thumbprint(),
                    pemEntry.type() == ExtractArchiveStep.ArchivePemEntry.ArchiveEntryType.CSCA
                        ? ArchiveCertificateEntry.ArchiveEntryType.CSCA
                        : ArchiveCertificateEntry.ArchiveEntryType.DSC
                );
            } else {
                log.error("PEM of Country: {}, Type: {}, Thumbprint: {} could not be parsed. Skipping entry.",
                    pemEntry.country(), pemEntry.type(), pemEntry.thumbprint());
                return null;
            }
        } catch (CertificateException | IOException e) {
            log.error("Unable to initialize PEM Parser.", e);
            System.exit(1);
            return null;
        }
    }

    public record ArchiveCertificateEntry(
        String country,
        X509CertificateHolder certificate,
        String thumbprint,
        ArchiveEntryType type) {

        enum ArchiveEntryType {
            DSC,
            CSCA
        }
    }
}
