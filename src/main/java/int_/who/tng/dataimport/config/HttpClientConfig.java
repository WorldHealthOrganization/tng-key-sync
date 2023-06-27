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

import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class HttpClientConfig {

    private final DccConfigProperties config;

    /**
     * Configure HTTP Client with ProxySettings.
     */
    @Bean
    public CloseableHttpClient apacheHttpClient() throws NoSuchAlgorithmException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        httpClientBuilder.setSSLContext(SSLContext.getDefault());
        httpClientBuilder.setSSLHostnameVerifier(new DefaultHostnameVerifier());

        if (config.getProxy().getHost() != null
            && config.getProxy().getPort() != -1
            && !config.getProxy().getHost().isEmpty()) {
            log.info("Using Proxy for HTTP Connection");

            // Set proxy
            httpClientBuilder.setProxy(new HttpHost(
                config.getProxy().getHost(),
                config.getProxy().getPort()
            ));
        } else {
            log.info("Using no proxy for HTTP Connection");
        }

        return httpClientBuilder.build();
    }
}
