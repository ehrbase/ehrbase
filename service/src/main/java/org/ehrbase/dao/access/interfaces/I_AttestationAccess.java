/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.dao.access.interfaces;

import com.nedap.archie.rm.generic.Attestation;
import java.util.List;
import java.util.UUID;
import org.ehrbase.dao.access.jooq.AttestationAccess;

public interface I_AttestationAccess extends I_SimpleCRUD {

    /**
     * Retrieve runtime instance of given attestation from DB
     * @param attestationId Given attestation ID
     * @return Attestation access object
     */
    I_AttestationAccess retrieveInstance(UUID attestationId);

    /**
     * Retrieve list of attestation IDs by the reference given by a version object
     * @param domainAccess      General data access
     * @param attestationRef    ID of reference from DB to find associated attestations
     * @return List of ID of attestations referenced by the given reference ID
     */
    static List<UUID> retrieveListOfAttestationsByRef(I_DomainAccess domainAccess, UUID attestationRef) {
        return AttestationAccess.retrieveListOfAttestationsByRef(domainAccess, attestationRef);
    }

    Attestation getAsAttestation();
}
