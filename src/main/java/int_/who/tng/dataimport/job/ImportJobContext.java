package int_.who.tng.dataimport.job;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.cert.X509CertificateHolder;

@Getter
@Setter
@NoArgsConstructor
public class ImportJobContext {

    private final Map<String, byte[]> files = new HashMap<>();

    private final List<CertificateEntry> parsedCertificates = new ArrayList<>();

    private final List<TrustedIssuerEntry> parsedIssuers = new ArrayList<>();

    //TODO: Add parsedTrustedReferences

    public enum CertificateType {
        DSC,
        CSCA,
        AUTHENTICATION,
        UPLOAD,
        DECA
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class CertificateEntry {
        private X509CertificateHolder parsedCertificateHolder;

        private X509Certificate parsedCertificate;

        private PrivateKey privateKey;

        private byte[] rawCertificate;

        private String thumbprint;

        /**
         * TrustAnchor or UploadCert Signature
         */
        private String signature;

        /**
         * 2-Digit Country Code
         */
        private String country;

        private CertificateType certificateType;

        private String domain;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class TrustedIssuerEntry {

        private String name;

        private String url;

        private String urlType;

        private String hash;

        private List<String> sslPublicKeys;

        private String signature;

        private String country;

        private String countryAlpha3;

        private String domain;
    }
}
