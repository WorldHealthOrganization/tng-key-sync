/*-
 * ---license-start
 * WHO Digital Documentation Covid Certificate Gateway Service / ddcc-gateway
 * ---
 * Copyright (C) 2022 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package int_.who.tng.dataimport.config;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("dgc")
public class DccConfigProperties {

    private final KeyStoreWithAlias trustAnchor = new KeyStoreWithAlias();

    private final ProxyConfig proxy = new ProxyConfig();

    private final List<ImportJobStep> importJobSteps = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportJobStep {

        /**
         * Name of the Import Job Step to execute
         */
        private ImportJobStepNames name;

        /**
         * [optional] Arguments passed to the step.
         */
        private String[] args = new String[0];

        /**
         * [optional] Specify whether the whole process should fail on critical exception. (default: true)
         */
        private boolean failOnCriticalException = true;

        public enum ImportJobStepNames {
            DownloadFile,
            VerifyFileSignature,
            ExtractZip,
            ParseCertificates,
            RemoveIgnoredCountries,
            RemoveExistingCertificatesFromContext,
            SaveCertificatesInDb,
            MapPrivateKey,
            AddCertificate,
            SignCertificates,
            ParseTrustedIssuers,
            RemoveExistingIssuersFromContext,
            SaveTrustedIssuersInDb
        }
    }

    @Getter
    @Setter
    public static class ProxyConfig {

        private String host;
        private int port = -1;
    }

    @Getter
    @Setter
    public static class KeyStoreWithAlias {
        private String keyStorePath;
        private String keyStorePass;
        private String certificateAlias;
    }
}
