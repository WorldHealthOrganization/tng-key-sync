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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DccKeyStore {

    private final DccConfigProperties dccConfigProperties;

    /**
     * Creates a KeyStore instance with keys for DGC TrustAnchor.
     *
     * @return KeyStore Instance
     */
    @Bean
    @Primary
    @Qualifier("trustAnchor")
    public KeyStore trustAnchorKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");

        loadKeyStore(
            keyStore,
            dccConfigProperties.getTrustAnchor().getKeyStorePath(),
            dccConfigProperties.getTrustAnchor().getKeyStorePass().toCharArray());

        return keyStore;
    }

    private void loadKeyStore(KeyStore keyStore, String path, char[] password) throws Exception {
        try (InputStream fileStream = getStream(path)) {
            keyStore.load(fileStream, password);
        } catch (Exception e) {
            log.error("Could not load Keystore {}", path);
            throw e;
        }
    }

    private InputStream getStream(String path) throws FileNotFoundException {
        if (path.startsWith("classpath:")) {
            String resourcePath = path.substring(10);
            return getClass().getClassLoader().getResourceAsStream(resourcePath);
        } else {
            File file = new File(path);
            return file.exists() ? new FileInputStream(path) : null;
        }
    }
}
