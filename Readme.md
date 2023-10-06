# TNG Key Import Tool

This tool is designed to import CSCA and DSC Certificates
from [EU Digital Green Certificates Gateway](https://github.com/eu-digital-green-certificates/dgc-gateway) and TNG
Onboarding Repositories into Smart Trust Network Gateway.

## Usage

The tool provides configurable atomic steps to process the data. These steps can be orchestrated to realize different
workflows.
Each step has access to one shared virtual context which can store files and certificates. This allows the user to
construct complex workflows to process data from different sources into TNG.

The flow needs to be configured via the application property ```dgc.import-job-steps```.
It expects an array of ImportJobStep-Definitions containing the name of the step and a list of arguments.

Example:

```yaml
dgc:
  import-job-steps:
    - name: DownloadFile
      args:
        - https://example.org/files/archive.zip
        - archive.zip
    - name: ExtractZip
      args:
        - archive.zip
        - archive
```

## Available Import Steps

The following steps can be used to create a custom import workflow.

### DownloadFile

Downloads a file from given URL and stores it with the provided name in the JobContext.

| Argument Number | Description                                           | Example                      |
|-----------------|-------------------------------------------------------|------------------------------|
| 0               | URL of the File to download                           | https://example.org/file.zip |
| 1               | Name with which the file will be stored in JobContext | file.zip                     |

To use a HTTP-Proxy configure it globally within the application properties:

```yaml
dgc:
  proxy:
    host: proxy.example.org
    port: 8080
```

### ExtractZip

Extracts a ZIP-Archive within the JobContext. All Files of the archive will be placed into the provided directory.

| Argument Number | Description                              | Example          |
|-----------------|------------------------------------------|------------------|
| 0               | Filename of the Archive to extract       | file.zip         |
| 1               | Directory Name to extract all files into | zip-file-content |

### MapPrivateKey

Scans all certificates in JobContext and checks whether the provided PrivateKey matches the PublicKey of the
certificate.
The PrivateKey will be linked to all matching certificates. The PrivateKey can then be used to sign data with the
certificate.

| Argument Number | Description                                                                          | Example                |
|-----------------|--------------------------------------------------------------------------------------|------------------------|
| 0               | CertificateType the scan is restricted to (one of AUTHENTICATION, UPLOAD, CSCA, DSC) | UPLOAD                 |
| 1               | Path to the KeyStore containing the PrivateKey                                       | /var/data/keystore.p12 |
| 2               | Password of the KeyStore                                                             | s3cret                 |
| 3               | Alias of the PrivateKey within the KeyStore                                          | sign-key               |
| 4               | Password of the PrivateKey within the KeyStore                                       | s3cret                 |

### ParseCertificates

Scans the JobContext Files for certificates and parses them in order to prepare them for later import.

| Argument Number | Description                                                                                                                                                                                            | Example                                                                                                                         |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| 0               | RegEx to define which files should be parsed. As optional feature you may use a capture group "DOMAIN" to define a domain. If no capture group is defined, "DCC" is always used as the default domain. | ^zip-file-content/DSC/.\*\.pem$ <br/><br/> Example with domain capture group: ^zip-file-content/DSC/(?\<DOMAIN\>[\w-]+)/.\*.pem |
| 1               | CertificateType the parsed certificates should be handled as (one of AUTHENTICATION, UPLOAD, CSCA, DSC)                                                                                                | DSC                                                                                                                             |
| 2               | Input Format of the files (one of PEM, JSON - see below for details)                                                                                                                                   | PEM                                                                                                                             |

The ParseCertificates Step can handle two different input formats for certificates:

* JSON - A JSON file having the key
    * ```trustAnchorSignature``` - Base64 encoded detached CMS Signature of a TrustAnchor or Upload Certificate
    * ```certificateRawData``` - Base64 Encoded RAW Data of Certificate (aka *Base64 Encoded DER* or *PEM without
      Linebreaks and Headers*)
    * ```certificateThumbprint``` - Ignored, actual value will be extracted from certificate
    * ```country``` - Ignored, actual value will be extracted from certificate
* PEM - Just PKCS#8 Encoded Certificates

### RemoveExistingCertificatesFromContext

Removes all Certificates from JobContext which already exist in target database.

| Argument Number | Description                                                                               | Example |
|-----------------|-------------------------------------------------------------------------------------------|---------|
| 0               | CertificateType the cleanup should be done for (one of AUTHENTICATION, UPLOAD, CSCA, DSC) | DSC     |

### RemoveIgnoredCountries

Removes all Certificates from JobContext having one of the specified Country Attributes

| Argument Number | Description           | Example |
|-----------------|-----------------------|---------|
| 0 .. n          | 2-Letter Country Code | DE      |

### SaveCertificatesInDb

Final Step to persist processed certificates in Database.

| Argument Number | Description                                                           | Example |
|-----------------|-----------------------------------------------------------------------|---------|
| 0               | CertificateType to persist (one of AUTHENTICATION, UPLOAD, CSCA, DSC) | DSC     |

### SignCertificates

Create CMS Signature of Certificate which do not already have a (TrustAnchor- or Upload-) signature using certificates
with linked PrivateKey in the JobContext. The step takes care that the Country-Attribute of the Signer- and the
ToBeSigned-
Certificate matches.
This Step can be used to create TrustAnchor- and Upload-Certificate Signatures.

| Argument Number | Description                                                                         | Example |
|-----------------|-------------------------------------------------------------------------------------|---------|
| 0               | Type of Signer-Certificates used to sign (one of AUTHENTICATION, UPLOAD, CSCA, DSC) | UPLOAD  |
| 1               | Type of Certificates to be signed (one of AUTHENTICATION, UPLOAD, CSCA, DSC)        | DSC     |

### VerifyFileSignature

Check integrity of a File in the JobContext with a Signature File in the JobContext. It's also possible to check the
SignerCertificate.

| Argument Number | Description                                                | Example                                                          |
|-----------------|------------------------------------------------------------|------------------------------------------------------------------|
| 0               | Name of the file to check                                  | archive.zip                                                      |
| 1               | Name of the file containing the detached CMS Signature     | archive.zip.sig                                                  |
| 2 .. n          | List of allowed SHA-256 thumbprints of Signer Certificates | 84b0309cb751d0660f48d96b7aff5ce950e741916b40264bc7d7c31d875e063b |

If no thumbprints of Signer-Certificates are provided the Step will throw an uncritical exception. In order to allow the
process to continue
it is required to set the attribute ```failOnCriticalException``` to ```false``` for this step.

## Prepared Workflows

This tool will be shipped with some prepared workflows which can be activated via Spring-Profiles.

### EU DSC, TNG Onboarding TrustedParty Import

The following steps are executed by this workflow:

1. Download ZIP Archive from EU Publication Service
2. Validate integrity of downloaded ZIP Archive
3. Extract ZIP Archive
4. Parse all DSC Certificates
5. Download ZIP Archive from TNG Onboarding Repository
6. Extract ZIP Archive
7. Parse all TLS, SCA and UP Certificates
8. Try to link UP_SYNC PrivateKey to UP Certificates
9. Sign DSC Certificates with UP Certificates having a PrivateKey (Upload Signature)
10. Remove Certificates from Countries which are on the Ignore-List
11. Remove Certificates which are already present in Database
12. Persist remaining Certificates (DSC, TLS, SCA, UP)

The following workflow-specific environment variables need to be set:

| ENV                        | Description                                                                                            | Optional | Example (Default if optional)                                                                |
|----------------------------|--------------------------------------------------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------|
| EUDCC_ARCHIVE_URL          | URL to Download the Publication Archive of EU DCC                                                      | Yes      | https://ec.europa.eu/assets/eu-dcc/dcc_trustlist.zip                                         |    
| EUDCC_SIGNATURE_URL        | URL to Download the Signature File of Publication Archive of EU DCC                                    | Yes      | https://ec.europa.eu/assets/eu-dcc/dcc_trustlist.zip.sig.txt                                 |    
| EUDCC_SIGNATURE_SIGNERCERT | SHA-256 thumbprints (HEX, lowercase) of allowed signer certificate for the EU DCC Publication Archive  | No       | 84b0309cb751d0660f48d96b7aff5ce950e741916b40264bc7d7c31d875e063b                             |    
| TNG_ONBOARDING_ARCHIVE_URL | URL to Download the Archive with onboarded Certificates for TNG                                        | Yes      | https://github.com/WorldHealthOrganization/tng-participants-prod/archive/refs/heads/main.zip |
| TNG_UPSYNC_KEYSTOREPATH    | Path to KeyStore (JKS or PKCS#12) holding the UP_SYNC PrivateKey                                       | No       | /var/keys/up_sync.p12                                                                        |    
| TNG_UPSYNC_KEYSTOREPASS    | Password of the KeyStore and the KeyEntry                                                              | No       | s3cr3t                                                                                       |
| TNG_UPSYNC_ALIAS           | Alias of the entry in KeyStore holding the UP_SYNC PrivateKey                                          | No       | up-sync                                                                                      |
| TNG_IGNORECOUNTRIES        | Comma-seperated list of Countries which shall be ignored by the tool                                   | Yes      | DE,FR (default empty)                                                                        |

To enable the workflow activate the spring profile ```eudsc-tng-onboarding```

### EU DSC, TNG Onboarding TrustedParty Import (Without EU DSC Certificate Check)

This workflow does the same as *EU DSC, TNG Onboarding TrustedParty Import* but without checking the thumbprint of the
signer certificate of the EU DCC Publication Archive.

The ENV ```EUDCC_SIGNATURE_SIGNERCERT``` can be left out.

To enable the workflow activate the spring profile ```eudsc-tng-onboarding-no-zip-cert-check```

### TNG Onboarding TrustedParty Import

This workflow imports all onboarded Certificates from TNG Onboarding Repository.

The following steps are executed by this workflow:

1. Download ZIP Archive from TNG Onboarding Repository
2. Extract ZIP Archive
3. Parse all TLS, SCA and UP Certificates
4. Remove Certificates from Countries which are on the Ignore-List
5. Remove Certificates which are already present in Database
6. Persist remaining Certificates (TLS, SCA, UP)

The following workflow specific environment variables need to be set:

| ENV                        | Description                                                                                            | Optional | Example (Default if optional)                                                                |
|----------------------------|--------------------------------------------------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------|
| TNG_ONBOARDING_ARCHIVE_URL | URL to Download the Archive with onboarded Certificates for TNG                                        | Yes      | https://github.com/WorldHealthOrganization/tng-participants-prod/archive/refs/heads/main.zip |
| TNG_IGNORECOUNTRIES        | Comma-seperated list of Countries which shall be ignored by the tool                                   | Yes      | DE,FR                                                                                        |

To enable the workflow activate the spring profile ```tng-onboarding```

## Global Settings

The following global settings are required in order to run the tool.

| ENV                               | Description                                                          | Example                              |
|-----------------------------------|----------------------------------------------------------------------|--------------------------------------|
| SPRING_DATASOURCE_URL             | JDBC URL to connect to the database                                  | jdbc:postgresql://dbHost:5432/dbName |    
| SPRING_DATASOURCE_USERNAME        | Database User Name                                                   | psql                                 |    
| SPRING_DATASOURCE_PASSWORD        | Database User Password                                               | s3cr3t                               |    
| SPRING_DATASOURCE_DRIVERCLASSNAME | JDBC Driver Class Name                                               | org.postgresql.Driver                |

Optional Settings:

| ENV                    | Description                                                          | Example           |
|------------------------|----------------------------------------------------------------------|-------------------|
| SPRING_PROFILES_ACTIVE | Name of the Spring Profile to activate to select a prepared workflow | tng-onboarding    |
| DGC_PROXY_HOST         | Hostname of HTTP Proxy to Download Files                             | proxy.example.org |
| DGC_PROXY_PORT         | Port of HTTP Proxy to Download Files                                 | 8080              |

# Run

Just run the Java Application (or the Docker Image). Import Job will be started automatically without any other trigger.

On success, the tool will exit with exit code 0. On error, it will return with exit code 1.
