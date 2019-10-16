/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Jake Smolka (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.api.service;

import org.ehrbase.api.definitions.CompositionFormat;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.DuplicateObjectException;
import com.nedap.archie.rm.ehr.EhrStatus;
import org.ehrbase.api.exception.InternalServerException;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EhrService {
    /**
     * Creates new EHR instance, with default settings and values when no status or ID is supplied.
     * @param status Optional, sets custom status
     * @param ehrId Optional, sets custom ID
     * @return UUID of new EHR instance
     * @throws DuplicateObjectException when given party/subject already has an EHR
     * @throws InternalServerException when unspecified error occurs
     */
    UUID create(EhrStatus status, UUID ehrId);

    @Deprecated
    Optional<EhrStatusDto> getEhrStatusEhrScape(UUID ehrUuid, CompositionFormat format);

    /**
     * Gets latest EHR_STATUS of the given EHR.
     * @param ehrUuid EHR subject
     * @return Latest EHR_STATUS or empty
     */
    Optional<EhrStatus> getEhrStatus(UUID ehrUuid);

    /**
     * Gets particular EHR_STATUS matching the given version Uid.
     * @param ehrUuid Root EHR
     * @param versionedObjectUid Given Uid of EHR_STATUS
     * @param version Given version of EHR_STATUS
     * @return Matching EHR_STATUS or empty
     */
    Optional<EhrStatus> getEhrStatusAtVersion(UUID ehrUuid, UUID versionedObjectUid, int version);

    Optional<EhrStatus> updateStatus(UUID ehrId, EhrStatus status);

    Optional<UUID> findBySubject(String subjectId, String nameSpace);

    /**
     * Checks if there is an ehr entry existing for specified ehrId.
     *
     * @param ehrId - Target EHR identified
     * @return EHR with id exists
     */
    boolean doesEhrExist(UUID ehrId);

    /**
     * FIXME EHR_STATUS: add docs
     * @param ehrId
     * @return
     */
    String getLatestVersionUidOfStatus(UUID ehrId);

    UUID getSystemUuid();    // from BaseService

    LocalDateTime getCreationTime(UUID ehrId);

    /**
     * FIXME EHR_STATUS: docs
     * @param ehrUid
     * @param timestamp
     * @return
     */
    Integer getEhrStatusVersionByTimestamp(UUID ehrUid, Timestamp timestamp);

    /**
     * Return True if a EHR with identifier ehrId exists.
     * Implements has_ehr from the openEHR Platform Abstract Service Model.
     * @param ehrId identifier to test
     * @return True when existing, false if not
     */
    Boolean hasEhr(UUID ehrId);

    /**
     * Helper to get (Versioned Object) Uid of EHR_STATUS of given EHR.
     * @param ehrUid Uid of EHR
     * @return UUID of corresponding EHR_STATUS
     */
    UUID getEhrStatusVersionedObjectUidByEhr(UUID ehrUid);

}
