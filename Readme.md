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
6. Onboard all non-existing CSCA Certificates to TNG Gateway Trusted Party Table (CSCA Certificates will be signed by TrustAnchor)
7. Create and Onboard SelfSigned Upload Certificates per country (They are required in order to upload the DSC Certificates)
   (The PrivateKey of the created Upload Certificate will be thrown away after program execution)
8. Upload all non-existing DSC Certificates to TNG Gateway Signer Information Table (Upload Certificates from Step 7 will be used to sign)

# Setup

Setup Environment Variables to configure the tool. 

*Description and Example value is still tbd*

| ENV                                           | Description                                                          | Example |
|-----------------------------------------------|----------------------------------------------------------------------|---------|
| SPRING_DATASOURCE_URL                         |                                                                      |         |    
| SPRING_DATASOURCE_USERNAME                    |                                                                      |         |    
| SPRING_DATASOURCE_PASSWORD                    |                                                                      |         |    
| SPRING_DATASOURCE_DRIVERCLASSNAME             |                                                                      |         |    
| DGC_PUBLICATION_URL                           |                                                                      |         |    
| DGC_PUBLICATION_ALLOWEDSIGNINGCERTTHUMBPRINTS |                                                                      |         |    
| DGC_TRUSTANCHOR_KEYSTOREPATH                  |                                                                      |         |    
| DGC_TRUSTANCHOR_KEYSTOREPASS                  |                                                                      |         |    
| DGC_TRUSTANCHOR_CERTIFICATEALIAS              |                                                                      |         |    
| DGC_UPLOADCERTS_VALIDITY                      |                                                                      |         |    
| DGC_UPLOADCERTS_COMMONNAME                    |                                                                      |         |
| DGC_IGNORECOUNTRIES                           | Comma-seperated list of Countries which shall be ignored by the tool | DE,FR   |

# Run

Just run the JAVA Application (or the Docker Image). Import Job will be started automatically without any other trigger.

On success the tool will exit with exit code 0. On error, it will return with exit code 1.
