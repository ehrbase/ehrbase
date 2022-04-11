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

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.RevisionHistory;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.DuplicateObjectException;
import org.ehrbase.api.exception.GeneralRequestProcessingException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.EhrStatusDto;

public interface EhrService extends BaseService {
  /**
   * Creates new EHR instance, with default settings and values when no status or ID is supplied.
   *
   * @param ehrId Optional, sets custom ID
   * @param status Optional, sets custom status
   * @return UUID of new EHR instance
   * @throws DuplicateObjectException when given party/subject already has an EHR
   * @throws InternalServerException when unspecified error occurs
   */
  UUID create(UUID ehrId, EhrStatus status);

  @Deprecated
  Optional<EhrStatusDto> getEhrStatusEhrScape(UUID ehrUuid, CompositionFormat format);

  /**
   * Gets latest EHR_STATUS of the given EHR.
   *
   * @param ehrUuid EHR subject
   * @return Latest EHR_STATUS or empty
   */
  Optional<EhrStatus> getEhrStatus(UUID ehrUuid);

  /**
   * Gets particular EHR_STATUS matching the given version Uid.
   *
   * @param ehrUuid            Root EHR
   * @param versionedObjectUid Given Uid of EHR_STATUS
   * @param version            Given version of EHR_STATUS
   * @return Matching EHR_STATUS or empty
   */
  Optional<OriginalVersion<EhrStatus>> getEhrStatusAtVersion(UUID ehrUuid, UUID versionedObjectUid, int version);

  /**
   * Update the EHR_STATUS linked to the given EHR
   *
   * @param ehrId ID of linked EHR
   * @param status input EHR_STATUS
   * @param contribution Optional ID of custom contribution. Can be null.
   * @return {@link Optional<EhrStatus>} containing the updated status on success
   * @throws org.ehrbase.api.exception.ObjectNotFoundException when given ehrId cannot be found
   * @throws org.ehrbase.api.exception.InvalidApiParameterException when given status is invalid,
   *     e.g. not a valid openEHR RM object
   */
  UUID updateStatus(UUID ehrId, EhrStatus status, UUID contribution);

  Optional<UUID> findBySubject(String subjectId, String nameSpace);

  /**
   * Checks if there is an ehr entry existing for specified ehrId.
   *
   * @param ehrId - Target EHR identified
   * @return EHR with id exists
   */
  boolean doesEhrExist(UUID ehrId);

  /**
   * Get latest version UID of an EHR_STATUS by given associated EHR UID.
   *
   * @param ehrId EHR ID
   * @return EHR_STATUS version UID
   */
  String getLatestVersionUidOfStatus(UUID ehrId);

  DvDateTime getCreationTime(UUID ehrId);

  /**
   * Get version number of EHR_STATUS associated with given EHR UID at given timestamp.
   *
   * @param ehrUid    EHR UID
   * @param timestamp Timestamp of point in time
   * @return version number
   */
  Integer getEhrStatusVersionByTimestamp(UUID ehrUid, Timestamp timestamp);

  /**
   * Return True if a EHR with identifier ehrId exists.
   * Implements has_ehr from the openEHR Platform Abstract Service Model.
   *
   * @param ehrId identifier to test
   * @return True when existing, false if not
   */
  boolean hasEhr(UUID ehrId);

  /**
   * Return True if a EHR_STATUS with identifier statusId exists.
   *
   * @param statusId identifier to test
   * @return True when existing, false if not
   */
  boolean hasStatus(UUID statusId);

  /**
   * Returns true if an EHR_STATUS for the given EHR ID exists and has the is_modifiable flag set to true.
   *
   * @param ehrId EHR ID to check
   * @return true if flag is true, false otherwise
   */
  boolean isModifiable(UUID ehrId);

  /**
   * Helper to get (Versioned Object) Uid of EHR_STATUS of given EHR.
   *
   * @param ehrUid Uid of EHR
   * @return UUID of corresponding EHR_STATUS
   */
  UUID getEhrStatusVersionedObjectUidByEhr(UUID ehrUid);

  /**
   * Gets version container EhrStatus associated with given EHR.
   *
   * @param ehrUid Given EHR ID
   * @return Version container object
   */
  VersionedEhrStatus getVersionedEhrStatus(UUID ehrUid);

  /**
   * Gets revision history of EhrStatus associated with given EHR.
   *
   * @param ehrUid Given EHR ID
   * @return Revision history object
   */
  RevisionHistory getRevisionHistoryOfVersionedEhrStatus(UUID ehrUid);

  /**
   * Reads the EHR entry from database and returns the ID of the root directory entry.
   *
   * @param ehrId - EHR id to find the directory for
   * @return UUID of the root directory if existing
   */
  UUID getDirectoryId(UUID ehrId);

  /**
   * Removes the directory information from EHR table entry after deletion of the corresponding folder from
   * folders table. If there were no such folder it will return a successful deletion.
   *
   * @param ehrId - Target EHR id
   * @return Directory entry is now 'null'
   */
  boolean removeDirectory(UUID ehrId);

  /**
   * Admin method to delete an EHR from the DB. See EHRbase Admin API specification for details.
   *
   * @param ehrId EHR to delete
   */
  void adminDeleteEhr(UUID ehrId);

  void adminPurgePartyIdentified();

  void adminDeleteOrphanHistory();

  /**
   * Helper to directly get the linked subject ID to given EHR.
   *
   * @param ehrId Given EHR ID
   * @return Linked subject ID or null
   */
  UUID getSubjectUuid(String ehrId);

  /**
   * Helper to directly get the external subject reference form the linked subject to given EHR.
   *
   * @param ehrId Given EHR ID
   * @return Linked external subject reference or null
   */
  String getSubjectExtRef(String ehrId);

  List<String> getSubjectExtRefs(Collection<String> ehrIds);  
  
  /**
   * Checks if an EHR with the given UUID exists.
   * Throws {@link ObjectNotFoundException} if no EHR is found
   *
   * @param ehrId EHR ID to check
   */
  default void checkEhrExists(UUID ehrId) {
    if (!doesEhrExist(ehrId)) {
      throw new ObjectNotFoundException("DIRECTORY", String.format("EHR with id %s not found", ehrId.toString()));
    }
  }

  /**
   * Checks if the EHR with the given UUID is modifiable.
   * Throws {@link ObjectNotFoundException} if no EHR is found.
   * Throws {@link GeneralRequestProcessingException} if the EHR is not modifiable.
   *
   * @param ehrId EHR ID to check
   */
  default void checkEhrExistsAndIsModifiable(UUID ehrId) {
    checkEhrExists(ehrId);
    if (!isModifiable(ehrId)) {
      throw new StateConflictException(
          String.format("EHR with id %s does not allow modification", ehrId.toString()));
    }
  }
}
