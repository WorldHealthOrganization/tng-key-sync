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

package int_.who.tng.dataimport.repository;

import int_.who.tng.dataimport.entity.TrustedPartyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedPartyRepository extends JpaRepository<TrustedPartyEntity, Long> {

    Optional<TrustedPartyEntity> getFirstByThumbprint(String thumbprint);

}
