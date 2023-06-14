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

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
    name = "dccPublicationClient",
    url = "${dgc.publication.url}",
    configuration = DccPublicationClientConfig.class)
public interface DccPublicationClient {

    /**
     * Download ZIP Archive of DCC Publication.
     */
    @GetMapping(value = "dcc_trustlist.zip", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    byte[] downloadPublicationArchive();

    /**
     * Download Signature File of DCC Publication.
     */
    @GetMapping(value = "dcc_trustlist.zip.sig.txt", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    byte[] downloadPublicationSignature();
}
