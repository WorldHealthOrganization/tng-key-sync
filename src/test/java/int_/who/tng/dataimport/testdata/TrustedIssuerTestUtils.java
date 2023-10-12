package int_.who.tng.dataimport.testdata;

import int_.who.tng.dataimport.job.ImportJobContext;
import java.util.ArrayList;
import java.util.List;

public class TrustedIssuerTestUtils {

    public static String getJson(String country) {
        return """
                {
                   "name": "Ministry of Health",
                   "url": "did:web:example.com",
                   "urlType": "DID",
                   "hash": "463bcd43a6ae45a5d9606adfb0c2d968cfacb73e0df827f05a7c7f781a1869c5",
                   "sslPublicKeys": [
                     "MIIGwjCCBaqgAwIBAvd3QuY29tMEkGCCsG....Lz3lGqBrHBklHq7x5WK4dAipTLrG39u",
                     "MIIGwjCCBaqgAwIBAvd3QuY29tMEkGCCsG....Lz3lGqBrHBklHq7x5WK4dAipTLrG40u"
                   ],
                   "signature": "MIIEqAYJKoZIhvcNAQcCoIIEmTCCBJUCAQExDTALBglghkgBZQMEAgEwgcIGCSqGSIb3DQEHAaCBtASBsU1pbmlzdHJ5IG9mIEhlYWx0aDtkaWQ6d2ViOmV4YW1wbGUuY29tO0RJRDtzaGEtMjU2IG9mIHRoZSBESUQgZG9jdW1lbnQgMjtUTFMgcHVibGljIGtleSBvZiB0aGUgRElEIGRvY3VtZW50IGhvc3Q7VExTIHB1YmxpYyBrZXkgb2YgdGhlIERJRCBkb2N1bWVudCBob3N0IHRvIHN1cHBvcnQga2V5IHJvdGF0aW9uCqCCAgUwggIBMIIBp6ADAgECAgIQATAKBggqhkjOPQQDAjBZMQswCQYDVQQGEwJDSDEPMA0GA1UECAwGR2VuZXZhMQwwCgYDVQQKDANXSE8xETAPBgNVBAsMCFROR19TSUdOMRgwFgYDVQQDDA9XSE8gVE5HX1NJR04gQ0EwHhcNMjMwNzEzMDkwMzUwWhcNMzMwNzEwMDkwMzUwWjBVMQswCQYDVQQGEwJDSDEPMA0GA1UECAwGR2VuZXZhMQwwCgYDVQQKDANXSE8xDzANBgNVBAsMBlROR19UQTEWMBQGA1UEAwwNV0hPIFROR19UQSBDQTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABEP2rP9irAgW14NAAMNFQYOmPhpDkqa8VEGPzTeB3bRu8uZSICbjcwgr0z8M9Zm4lYuuKEsGMQV9y2dquaQv2EqjYzBhMB0GA1UdDgQWBBQB44eysRHZxWPIKM0G7Rk+uPDlPzAfBgNVHSMEGDAWgBSwLL6TPqcS9cYJIrgRxo62mLdCsjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBhjAKBggqhkjOPQQDAgNIADBFAiBVYQA2LOv1640ww7/Hih9d3j4eNRgYC6jI962Y+rcYYgIhANdzVMZXFh4XvFAwD8Q0gog+VTteRoov3OVM3IdIgaczMYIBsTCCAa0CAQEwXzBZMQswCQYDVQQGEwJDSDEPMA0GA1UECAwGR2VuZXZhMQwwCgYDVQQKDANXSE8xETAPBgNVBAsMCFROR19TSUdOMRgwFgYDVQQDDA9XSE8gVE5HX1NJR04gQ0ECAhABMAsGCWCGSAFlAwQCAaCB5DAYBgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMBwGCSqGSIb3DQEJBTEPFw0yMzEwMTAxNDE3MDVaMC8GCSqGSIb3DQEJBDEiBCDZ1v2ePCmhZCSKYeH9qYlEnbMDHeOGIEp9WiMTnr4cRjB5BgkqhkiG9w0BCQ8xbDBqMAsGCWCGSAFlAwQBKjALBglghkgBZQMEARYwCwYJYIZIAWUDBAECMAoGCCqGSIb3DQMHMA4GCCqGSIb3DQMCAgIAgDANBggqhkiG9w0DAgIBQDAHBgUrDgMCBzANBggqhkiG9w0DAgIBKDAKBggqhkjOPQQDAgRHMEUCIQCPDvXdQzWOKvA1SDGd4r0EVV8wzdMcVIQABW+K82ptigIgD+cv4SipbRdAQXUcMOjsj0B4Z4Z9Wevo4MvOLLYogSA=",
                   "country": "%s"
                 }
              """.formatted(
                country
        );
    }

    public static ImportJobContext.TrustedIssuerEntry getTrustedIssuerEntry(String countryAlpha3, String domain) {
        List<String> sslPublicKeys = new ArrayList<>();
        sslPublicKeys.add("MIIGwjCCBaqgAwIBAvd3QuY29tMEkGCCsG....Lz3lGqBrHBklHq7x5WK4dAipTLrG39u");
        return new ImportJobContext.TrustedIssuerEntry(
                "Ministry of Health",
                "did:web:example.com",
                "DID",
                "463bcd43a6ae45a5d9606adfb0c2d968cfacb73e0df827f05a7c7f781a1869c5",
                sslPublicKeys,
                "MIIEqAYJKoZIhvcNAQcCoIIEmTCCBJUCAQExDTALBglghkgBZQMEAgEwgcIGCSqGSIb3DQEHAaCBtASBsU1pbmlzdHJ5IG9mIEhlYWx0aDtkaWQ6d2ViOmV4YW1wbGUuY29tO0RJRDtzaGEtMjU2IG9mIHRoZSBESUQgZG9jdW1lbnQgMjtUTFMgcHVibGljIGtleSBvZiB0aGUgRElEIGRvY3VtZW50IGhvc3Q7VExTIHB1YmxpYyBrZXkgb2YgdGhlIERJRCBkb2N1bWVudCBob3N0IHRvIHN1cHBvcnQga2V5IHJvdGF0aW9uCqCCAgUwggIBMIIBp6ADAgECAgIQATAKBggqhkjOPQQDAjBZMQswCQYDVQQGEwJDSDEPMA0GA1UECAwGR2VuZXZhMQwwCgYDVQQKDANXSE8xETAPBgNVBAsMCFROR19TSUdOMRgwFgYDVQQDDA9XSE8gVE5HX1NJR04gQ0EwHhcNMjMwNzEzMDkwMzUwWhcNMzMwNzEwMDkwMzUwWjBVMQswCQYDVQQGEwJDSDEPMA0GA1UECAwGR2VuZXZhMQwwCgYDVQQKDANXSE8xDzANBgNVBAsMBlROR19UQTEWMBQGA1UEAwwNV0hPIFROR19UQSBDQTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABEP2rP9irAgW14NAAMNFQYOmPhpDkqa8VEGPzTeB3bRu8uZSICbjcwgr0z8M",
                "DE",
                countryAlpha3,
                domain);
    }

}
