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
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("dgc")
public class DccConfigProperties {

    private final KeyStoreWithAlias trustAnchor = new KeyStoreWithAlias();

    private final Publication publication = new Publication();

    private final UploadCertConfig uploadCerts = new UploadCertConfig();

    private final List<String> ignoreCountries = new ArrayList<>();

    @Getter
    @Setter
    public static class UploadCertConfig {

        /**
         * CommonName used for Self-Signed Upload Certificates.
         */
        private String commonName;

        /**
         * Validity of the created Upload Certificates in days.
         */
        private Integer validity;
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

    @Getter
    @Setter
    public static class Publication {
        /**
         * List of thumbprints of expected signing certificates for the downloaded Publication Archive.
         * (SHA-256, HEX representation, lowercase, e.g.
         * 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4)
         */
        private List<String> allowedSigningCertThumbprints = new ArrayList<>();
        private String url;
        private ProxyConfig proxy = new ProxyConfig();
    }
}
