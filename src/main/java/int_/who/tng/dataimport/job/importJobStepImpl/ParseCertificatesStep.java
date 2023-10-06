package int_.who.tng.dataimport.job.importJobStepImpl;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import int_.who.tng.dataimport.job.ImportJobStep;
import int_.who.tng.dataimport.job.ImportJobStepException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
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
    public void exec(ImportJobContext context, String... args) throws ImportJobStepException {
        Pattern fileNamePattern = Pattern.compile(args[0]);
        ImportJobContext.CertificateType certificateType = ImportJobContext.CertificateType.valueOf(args[1]);
        String format = args[2];

        log.debug("Parsing Files matching {} as Certificates of type {}", fileNamePattern, certificateType);

        List<ImportJobContext.CertificateEntry> parsedCerts = context.getFiles().entrySet().stream()
            .filter(entry -> fileNamePattern.matcher(entry.getKey()).matches())
            .map(file -> {
                //TODO: handover Domain to context
                Matcher matcher = fileNamePattern.matcher(file.getKey());
                matcher.find();
                String domain = null;
                try {
                    domain = matcher.group("DOMAIN");
                } catch (IllegalArgumentException ignored) {
                }


                if (format.equalsIgnoreCase("JSON")) {
                    return parseJson(file.getValue(), certificateType, domain);
                } else if (format.equalsIgnoreCase("PEM")) {
                    return parsePem(file.getValue(), certificateType, domain);
                } else {
                    throw new ImportJobStepException(true, format + " is not a known format for certificate files");
                }
            })
            .toList();

        context.getParsedCertificates().addAll(parsedCerts);

        log.debug("Finished parsing {} File into Certificate Objects.", parsedCerts.size());
    }

    private ImportJobContext.CertificateEntry parsePem(byte[] file, ImportJobContext.CertificateType certificateType, String domain)
        throws ImportJobStepException {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");

            Certificate cert = certificateFactory.generateCertificate(new ByteArrayInputStream(file));

            if (cert instanceof X509Certificate x509Certificate) {
                X509CertificateHolder x509CertificateHolder = certificateUtils.convertCertificate(x509Certificate);

                RDN[] countryRdns = x509CertificateHolder.getSubject().getRDNs(BCStyle.C);

                if (countryRdns.length != 1) {
                    throw new ImportJobStepException(true, "No Country Attribute in Cert with Subject " +
                        x509CertificateHolder.getSubject());
                }

                String countryCode = IETFUtils.valueToString(countryRdns[0].getFirst().getValue());

                return new ImportJobContext.CertificateEntry(
                    x509CertificateHolder,
                    x509Certificate,
                    null,
                    x509Certificate.getEncoded(),
                    certificateUtils.getCertThumbprint(x509Certificate),
                    null,
                    countryCode,
                    certificateType,
                    domain);
            } else {
                throw new ImportJobStepException(true, "Failed to parse Certificate as X509Certificate");
            }
        } catch (CertificateException | IOException e) {
            throw new ImportJobStepException(true, "Unable to initialize PEM Parser: " + e.getMessage());
        }
    }

    private ImportJobContext.CertificateEntry parseJson(byte[] file, ImportJobContext.CertificateType certificateType, String domain)
        throws ImportJobStepException {
        try {
            JsonStructure json = objectMapper.readValue(file, JsonStructure.class);

            X509CertificateHolder x509CertificateHolder =
                new X509CertificateHolder(Base64.getDecoder().decode(json.getCertificateRawData()));
            RDN[] countryRdns = x509CertificateHolder.getSubject().getRDNs(BCStyle.C);

            if (countryRdns.length != 1) {
                throw new ImportJobStepException(true, "No Country Attribute in Cert with Subject " +
                    x509CertificateHolder.getSubject());
            }

            String countryCode = IETFUtils.valueToString(countryRdns[0].getFirst().getValue());

            X509Certificate x509Certificate = certificateUtils.convertCertificate(x509CertificateHolder);

            return new ImportJobContext.CertificateEntry(
                x509CertificateHolder,
                x509Certificate,
                null,
                x509CertificateHolder.getEncoded(),
                certificateUtils.getCertThumbprint(x509CertificateHolder),
                json.trustAnchorSignature,
                countryCode,
                certificateType,
                domain);

        } catch (DatabindException e) {
            throw new ImportJobStepException(true, "Failed to parse JSON: " + e.getMessage());
        } catch (IOException | CertificateException e) {
            throw new ImportJobStepException(true, "Failed to parse Certificate: " + e.getMessage());
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
