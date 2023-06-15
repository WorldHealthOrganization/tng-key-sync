# TNG Key Import Tool

This tool is designed to import CSCA and DSC Certificates
from [EU Digital Green Certificates Gateway](https://github.com/eu-digital-green-certificates/dgc-gateway) into
Smart Trust Network Gateway.

# Workflow

The following steps are executed by the tool:

1. Download ZIP Archive from EU Publication Service
2. Validate integrity of downloaded ZIP Archive
3. Extract ZIP Archive
4. Remove all Certificates of Countries which are on ignore list
5. Parse all .pem files
6. Onboard all non-existing CSCA Certificates to TNG Gateway Trusted Party Table (CSCA Certificates will be signed by
   TrustAnchor)
7. Create and Onboard SelfSigned Upload Certificates per country (They are required in order to upload the DSC
   Certificates)
   (The PrivateKey of the created Upload Certificate will be thrown away after program execution)
8. Upload all non-existing DSC Certificates to TNG Gateway Signer Information Table (Upload Certificates from Step 7
   will be used to sign)

# Setup

Setup Environment Variables to configure the tool.

| ENV                                           | Description                                                                                                                                        | Example                                                          |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| SPRING_DATASOURCE_URL                         | JDBC URL to connect to the database                                                                                                                | jdbc:postgresql://dbHost:5432/dbName                             |    
| SPRING_DATASOURCE_USERNAME                    | Database User Name                                                                                                                                 | psql                                                             |    
| SPRING_DATASOURCE_PASSWORD                    | Database User Password                                                                                                                             | s3cr3t                                                           |    
| SPRING_DATASOURCE_DRIVERCLASSNAME             | JDBC Driver Class Name                                                                                                                             | org.postgresql.Driver                                            |    
| DGC_PUBLICATION_URL                           | URL of WebService where dcc_trustlist.zip and dcc_trustlist.zip.sig.txt can be found                                                               | https://ec.europa.eu/assets/eu-dcc                               |    
| DGC_PUBLICATION_ALLOWEDSIGNINGCERTTHUMBPRINTS | List of SHA-256 thumbprints (HEX, lowercase) of allowed signer certificates for the ZIP Archive. *If list is empty the validation will be skipped* | 84b0309cb751d0660f48d96b7aff5ce950e741916b40264bc7d7c31d875e063b |    
| DGC_TRUSTANCHOR_KEYSTOREPATH                  | Path to KeyStore (JKS or PKCS#12) holding the TrustAnchor Certificate and PrivateKey                                                               | /var/keys/trustanchor.p12                                        |    
| DGC_TRUSTANCHOR_KEYSTOREPASS                  | Password of the KeyStore and the KeyEntry                                                                                                          | s3cr3t                                                           |    
| DGC_TRUSTANCHOR_CERTIFICATEALIAS              | Alias of the entry in KeyStore holding the TrustAnchor Certificate and PrivateKey                                                                  | trustanchor                                                      |    
| DGC_UPLOADCERTS_VALIDITY                      | Validity in Days of the Upload Certificates that will be issued for uploading DSC                                                                  | 365                                                              |    
| DGC_UPLOADCERTS_COMMONNAME                    | Common Name Attribute of the Subject of the Upload Certificates that will be issued for uploading DSC                                              | TNG Key Import Upload                                            |
| DGC_IGNORECOUNTRIES                           | Comma-seperated list of Countries which shall be ignored by the tool                                                                               | DE,FR                                                            |

# Run

Just run the JAVA Application (or the Docker Image). Import Job will be started automatically without any other trigger.

On success the tool will exit with exit code 0. On error, it will return with exit code 1.
