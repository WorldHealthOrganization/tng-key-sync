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

package int_.who.tng.dataimport.client;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import int_.who.tng.dataimport.config.DccConfigProperties;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;

@RequiredArgsConstructor
@Slf4j
public class DccPublicationClientConfig {

    private final DccConfigProperties config;

    /**
     * Configure FeignClient with ProxySettings.
     */
    @Bean
    public Client publicationDownloadClient() throws NoSuchAlgorithmException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        httpClientBuilder.setSSLContext(SSLContext.getDefault());
        httpClientBuilder.setSSLHostnameVerifier(new DefaultHostnameVerifier());

        if (config.getPublication().getProxy().getHost() != null
            && config.getPublication().getProxy().getPort() != -1
            && !config.getPublication().getProxy().getHost().isEmpty()) {
            log.info("Using Proxy for DCC Publication Connection");
            // Set proxy
            httpClientBuilder.setProxy(new HttpHost(
                config.getPublication().getProxy().getHost(),
                config.getPublication().getProxy().getPort()
            ));
        } else {
            log.info("Using no proxy for DCC Publication Connection");
        }

        return new ApacheHttpClient(httpClientBuilder.build());
    }
}
