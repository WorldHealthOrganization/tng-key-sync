package int_.who.tng.dataimport.job;

import eu.europa.ec.dgc.signing.SignedCertificateMessageBuilder;
import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.config.DccConfigProperties;
import int_.who.tng.dataimport.entity.SignerInformationEntity;
import int_.who.tng.dataimport.entity.TrustedPartyEntity;
import int_.who.tng.dataimport.repository.SignerInformationRepository;
import int_.who.tng.dataimport.repository.TrustedPartyRepository;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportDSCStep {

    @Qualifier("trustAnchor")
    private final PrivateKey trustAnchorPrivateKey;

    @Qualifier("trustAnchor")
    private final X509CertificateHolder trustAnchorCertificate;

    private final TrustedPartyRepository trustedPartyRepository;

    private final SignerInformationRepository signerInformationRepository;

    private final DccConfigProperties dccConfigProperties;

    private final CertificateUtils certificateUtils;

    private final Map<String, KeyPair> uploadCertificates = new HashMap<>();

    public void exec(List<ParseCertificatesStep.ArchiveCertificateEntry> certificateList) {
        log.info("Importing DSC into Database");

        certificateList.stream()
            .filter(this::isDsc)
            .filter(this::doesNotAlreadyExists)
            .map(this::getOrCreateUploadCertificate)
            .map(this::sign)
            .forEach(this::insert);
    }

    private DscCertificateWithUploadCertificate getOrCreateUploadCertificate(
        ParseCertificatesStep.ArchiveCertificateEntry dsc) {

        KeyPair uploadCertificate = uploadCertificates.computeIfAbsent(dsc.country(), country -> {
            KeyPair generatedCertificate = generateUploadCertificate(country);
            String thumbprint = certificateUtils.getCertThumbprint(generatedCertificate.certificate());
            String trustAnchorSignature =
                signWithTrustAnchor(generatedCertificate.certificate(), thumbprint, dsc.country());
            insertUploadCertificate(generatedCertificate.certificate, thumbprint, trustAnchorSignature, dsc.country());

            return generatedCertificate;
        });

        return new DscCertificateWithUploadCertificate(
            dsc,
            uploadCertificate.certificate(),
            uploadCertificate.privateKey()
        );
    }

    private void insertUploadCertificate(X509CertificateHolder uploadCertificate, String thumbprint,
                                         String trustAnchorSignature, String country) {
        try {
            String certRawData = Base64.getEncoder().encodeToString(
                uploadCertificate.getEncoded());

            TrustedPartyEntity trustedPartyEntity = new TrustedPartyEntity();
            trustedPartyEntity.setCountry(country);
            trustedPartyEntity.setCertificateType(TrustedPartyEntity.CertificateType.UPLOAD);
            trustedPartyEntity.setThumbprint(thumbprint);
            trustedPartyEntity.setRawData(certRawData);
            trustedPartyEntity.setSignature(trustAnchorSignature);

            trustedPartyRepository.save(trustedPartyEntity);

            log.info("Inserted UPLOAD Certificate with thumbprint {} for country {}", thumbprint, country);
        } catch (IOException e) {
            log.error("Failed to convert Certificate");
        }
    }

    private String signWithTrustAnchor(X509CertificateHolder certificate, String thumbprint, String country) {
        log.info("Signing UPLOAD Certificate with thumbprint {} for country {} with TrustAnchor",
            thumbprint, country);

        byte[] signature = new SignedCertificateMessageBuilder()
            .withSigningCertificate(trustAnchorCertificate, trustAnchorPrivateKey)
            .withPayload(certificate)
            .build(true);

        return Base64.getEncoder().encodeToString(signature);
    }

    private KeyPair generateUploadCertificate(String country) {
        log.info("Generating Upload Certificate for Country {}", country);
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();

            X500Name subject = new X500NameBuilder()
                .addRDN(X509ObjectIdentifiers.countryName, country)
                .addRDN(X509ObjectIdentifiers.commonName, dccConfigProperties.getUploadCerts().getCommonName())
                .build();

            BigInteger certSerial = new BigInteger(Long.toString(System.currentTimeMillis()));

            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate());

            Date validFrom = new Date();
            Date validTo =
                Date.from(Instant.now().plus(dccConfigProperties.getUploadCerts().getValidity(), ChronoUnit.DAYS));

            JcaX509v3CertificateBuilder certBuilder =
                new JcaX509v3CertificateBuilder(subject, certSerial, validFrom, validTo, subject, keyPair.getPublic());

            BasicConstraints basicConstraints = new BasicConstraints(false);
            certBuilder.addExtension(Extension.basicConstraints, true, basicConstraints);

            return new KeyPair(
                certBuilder.build(contentSigner),
                keyPair.getPrivate()
            );
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | OperatorCreationException |
                 CertIOException e) {
            log.error("Failed to create Upload Certificate for {}", country);
            System.exit(1);
            return null;
        }
    }

    private void insert(SignedDscCertificate signedDscCertificate) {
        try {
            String certRawData = Base64.getEncoder().encodeToString(
                signedDscCertificate.certificateEntry.certificate().getEncoded());

            SignerInformationEntity signerInformationEntity = new SignerInformationEntity();
            signerInformationEntity.setCountry(signedDscCertificate.certificateEntry.country());
            signerInformationEntity.setCertificateType(SignerInformationEntity.CertificateType.DSC);
            signerInformationEntity.setThumbprint(signedDscCertificate.certificateEntry.thumbprint());
            signerInformationEntity.setRawData(certRawData);
            signerInformationEntity.setSignature(signedDscCertificate.uploadSignature);

            signerInformationRepository.save(signerInformationEntity);

            log.info("Inserted DSC with thumbprint {} for country {}",
                signedDscCertificate.certificateEntry.thumbprint(), signedDscCertificate.certificateEntry.country());

        } catch (IOException e) {
            log.error("Failed to convert Certificate");
        }
    }

    private SignedDscCertificate sign(DscCertificateWithUploadCertificate dsc) {
        log.info("Signing DSC with thumbprint {} for country {} with Upload Certificate",
            dsc.certificateEntry.thumbprint(), dsc.certificateEntry.country());

        byte[] signature = new SignedCertificateMessageBuilder()
            .withSigningCertificate(dsc.uploadCertificate, dsc.uploadCertificatePrivateKey)
            .withPayload(dsc.certificateEntry.certificate())
            .build(true);

        return new SignedDscCertificate(
            dsc.certificateEntry,
            Base64.getEncoder().encodeToString(signature));
    }

    private boolean doesNotAlreadyExists(ParseCertificatesStep.ArchiveCertificateEntry certificateEntry) {
        if (trustedPartyRepository.getFirstByThumbprintAndCertificateType(
            certificateEntry.thumbprint(), TrustedPartyEntity.CertificateType.CSCA).isPresent()) {
            log.info("CSCA with thumbprint {} for country {} already exists",
                certificateEntry.thumbprint(), certificateEntry.country());

            return false;
        } else {
            return true;
        }
    }

    /**
     * Filter Certificates to get only DSC
     */
    private boolean isDsc(ParseCertificatesStep.ArchiveCertificateEntry certificateEntry) {
        return certificateEntry.type() == ParseCertificatesStep.ArchiveCertificateEntry.ArchiveEntryType.DSC;
    }

    private record SignedDscCertificate(
        ParseCertificatesStep.ArchiveCertificateEntry certificateEntry,
        String uploadSignature
    ) {
    }

    private record DscCertificateWithUploadCertificate(
        ParseCertificatesStep.ArchiveCertificateEntry certificateEntry,
        X509CertificateHolder uploadCertificate,
        PrivateKey uploadCertificatePrivateKey
    ) {
    }

    private record KeyPair(
        X509CertificateHolder certificate,
        PrivateKey privateKey
    ) {
    }

}
