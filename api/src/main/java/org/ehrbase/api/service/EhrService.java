/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.api.service;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.changecontrol.VersionedObject;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.ValidationException;

public interface EhrService {

    /**
     * Creates new EHR instance, with default settings and values when no status or ID is supplied.
     *
     * @param ehrId Optional, sets custom ID
     * @param status Optional, sets custom status
     * @return UUID of new EHR instance
     * @throws StateConflictException  when an EHR with the given id already exist
     * @throws ValidationException when given status is invalid, e.g. not a valid openEHR RM object
     */
    UUID create(@Nullable UUID ehrId, @Nullable EhrStatusDto status);

    /**
     * Gets latest EHR_STATUS of the given EHR.
     *
     * @param ehrUuid EHR subject
     * @return Latest EHR_STATUS
     * @throws ObjectNotFoundException if no EHR is found
     */
    EhrStatusDto getEhrStatus(UUID ehrUuid);

    /**
     * Gets particular EHR_STATUS matching the given version Uid.
     *
     * @param ehrUuid            Root EHR
     * @param versionedObjectUid Given Uid of EHR_STATUS
     * @param version            Given version of EHR_STATUS
     * @return Matching EHR_STATUS or empty
     * @throws ObjectNotFoundException if no EHR is found
     */
    Optional<OriginalVersion<EhrStatusDto>> getEhrStatusAtVersion(UUID ehrUuid, UUID versionedObjectUid, int version);

    /**
     * Update the EHR_STATUS linked to the given EHR
     *
     * @param ehrId        ID of linked EHR
     * @param status       input EHR_STATUS
     * @param contribution Optional ID of custom contribution. Can be null.
     * @param audit        Audit event id
     * @return {@link UUID} of the updated status
     * @throws ObjectNotFoundException if no EHR is found
     * @throws ValidationException when given status is invalid, e.g. not a valid openEHR RM object
     */
    ObjectVersionId updateStatus(
            UUID ehrId, EhrStatusDto status, ObjectVersionId targetObjId, UUID contribution, UUID audit);

    /**
     * Search for an EHR_STATUS based on the given subject id and namespace
     *
     * @param subjectId ID of the EHR_STATUS subject
     * @param nameSpace of the EHR_STATUS subject
     * @return {@link Optional<UUID>} of the matching EHR_STATUS
     */
    Optional<UUID> findBySubject(String subjectId, String nameSpace);

    /**
     * Get latest version UID of an EHR_STATUS by given associated EHR UID.
     *
     * @param ehrId EHR ID
     * @return EHR_STATUS version UID
     * @throws ObjectNotFoundException if no EHR is found
     */
    ObjectVersionId getLatestVersionUidOfStatus(UUID ehrId);

    /**
     * Get version number of EHR_STATUS associated with given EHR UID at given timestamp.
     *
     * @param ehrUid    EHR UID
     * @param timestamp Timestamp of point in time
     * @return version number
     * @throws ObjectNotFoundException if no EHR is found
     */
    ObjectVersionId getEhrStatusVersionByTimestamp(UUID ehrUid, OffsetDateTime timestamp);

    /**
     * Provides the creation time of the given EHR id.
     *
     * @param ehrId ID of the EHR
     * @return {@link DvDateTime} of the EHR creation
     */
    DvDateTime getCreationTime(UUID ehrId);

    /**
     * Return True if a EHR with identifier ehrId exists.
     * Implements has_ehr from the openEHR Platform Abstract Service Model.
     *
     * @param ehrId identifier to test
     * @return True when existing, false if not
     */
    boolean hasEhr(UUID ehrId);

    /**
     * Gets version container EhrStatus associated with given EHR.
     *
     * @param ehrId Given EHR ID
     * @return Version container object
     * @throws ObjectNotFoundException if no EHR is found
     */
    VersionedObject<EhrStatusDto> getVersionedEhrStatus(UUID ehrId);

    /**
     * Gets revision history of EhrStatus associated with given EHR.
     *
     * @param ehrId Given EHR ID
     * @return Revision history object
     * @throws ObjectNotFoundException if no EHR is found
     */
    RevisionHistory getRevisionHistoryOfVersionedEhrStatus(UUID ehrId);

    /**
     * Admin method to delete an EHR from the DB. See EHRbase Admin API specification for details.
     *
     * @param ehrId EHR to delete
     */
    void adminDeleteEhr(UUID ehrId);

    /**
     * Helper to directly get the external subject reference form the linked subject to given EHR.
     *
     * @param ehrId Given EHR ID
     * @return Linked external subject reference or null
     * @throws ObjectNotFoundException if no EHR is found
     */
    String getSubjectExtRef(String ehrId);

    /**
     * Checks if an EHR with the given UUID exists.
     *
     * @param ehrId EHR ID to check
     * @throws ObjectNotFoundException if no EHR is found
     */
    void checkEhrExists(UUID ehrId);

    /**
     * Checks if the EHR with the given UUID is modifiable.
     *
     * @param ehrId EHR ID to check
     * @throws ObjectNotFoundException if no EHR is found.
     * @throws StateConflictException if the EHR is not modifiable.
     */
    void checkEhrExistsAndIsModifiable(UUID ehrId);
}
