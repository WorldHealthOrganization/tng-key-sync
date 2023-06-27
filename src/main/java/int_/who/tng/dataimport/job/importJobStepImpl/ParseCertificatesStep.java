package int_.who.tng.dataimport.job.importJobStepImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.stereotype.Service;

@Service("ParseCertificates")
@RequiredArgsConstructor
@Slf4j
public class ParseCertificatesStep implements ImportJobStep {

    private final CertificateUtils certificateUtils;

    private final ObjectMapper objectMapper;

    @Override
    public void exec(ImportJobContext context, String... args) {
        Pattern fileNamePattern = Pattern.compile(args[0]);
        ImportJobContext.CertificateType certificateType = ImportJobContext.CertificateType.valueOf(args[1]);
        String format = args[2];

        log.info("Parsing Files matching {} as Certificates of type {}", fileNamePattern, certificateType);

        List<ImportJobContext.CertificateEntry> parsedCerts = context.getFiles().entrySet().stream()
            .filter(entry -> fileNamePattern.matcher(entry.getKey()).matches())
            .map(Map.Entry::getValue)
            .map(file -> {
                if (format.equalsIgnoreCase("JSON")) {
                    return parseJson(file, certificateType);
                } else if (format.equalsIgnoreCase("PEM")) {
                    return parsePem(file, certificateType);
                } else {
                    log.error("{} is not a known format for certificate files.", format);
                    System.exit(1);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();

        context.getParsedCertificates().addAll(parsedCerts);

        log.info("Finished parsing {} File into Certificate Objects.", parsedCerts.size());
    }

    private ImportJobContext.CertificateEntry parsePem(byte[] file, ImportJobContext.CertificateType certificateType) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");

            Certificate cert = certificateFactory.generateCertificate(new ByteArrayInputStream(file));

            if (cert instanceof X509Certificate x509Certificate) {
                X509CertificateHolder x509CertificateHolder = certificateUtils.convertCertificate(x509Certificate);

                RDN[] countryRdns = x509CertificateHolder.getSubject().getRDNs(BCStyle.C);

                if (countryRdns.length != 1) {
                    log.error("No Country Attribute in Cert with Subject {}. Skipping File",
                        x509CertificateHolder.getSubject());
                    return null;
                }

                String countryCode = IETFUtils.valueToString(countryRdns[0].getFirst().getValue());

                return new ImportJobContext.CertificateEntry(
                    x509CertificateHolder,
                    null,
                    x509Certificate.getEncoded(),
                    certificateUtils.getCertThumbprint(x509Certificate),
                    null,
                    countryCode,
                    certificateType);
            } else {
                log.error("Failed to parse File as X509Certificate. Skipping File");
                return null;
            }
        } catch (CertificateException | IOException e) {
            log.error("Unable to initialize PEM Parser.", e);
            System.exit(1);
            return null;
        }
    }

    private ImportJobContext.CertificateEntry parseJson(byte[] file, ImportJobContext.CertificateType certificateType) {
        try {
            JsonStructure json = objectMapper.readValue(file, JsonStructure.class);

            X509CertificateHolder x509CertificateHolder =
                new X509CertificateHolder(Base64.getDecoder().decode(json.getCertificateRawData()));
            RDN[] countryRdns = x509CertificateHolder.getSubject().getRDNs(BCStyle.C);

            if (countryRdns.length != 1) {
                log.error("No Country Attribute in Cert with Subject {}. Skipping File",
                    x509CertificateHolder.getSubject());
                return null;
            }

            String countryCode = IETFUtils.valueToString(countryRdns[0].getFirst().getValue());

            return new ImportJobContext.CertificateEntry(
                x509CertificateHolder,
                null,
                x509CertificateHolder.getEncoded(),
                certificateUtils.getCertThumbprint(x509CertificateHolder),
                json.trustAnchorSignature,
                countryCode,
                certificateType);

        } catch (IOException e) {
            log.error("Unable to Parse Certificate.", e);
            System.exit(1);
            return null;
        }
    }

    @NoArgsConstructor
    @Getter
    @Setter
    static class JsonStructure {
        private String trustAnchorSignature;

        private String certificateRawData;

        private String certificateThumbprint;

        private String country;
    }
}
