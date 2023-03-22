/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
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

import java.time.LocalDateTime;
import java.util.UUID;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;

/**
 * Common interface for versioned objects, like compositions, folders and statuses.
 */
public interface I_VersionedCRUD {

    /**
     * Commit the object with the necessary metadata.
     * @param timestamp Time of operation
     * @param committerId Audit committer
     * @param systemId Audit system
     * @param description (Optional) Audit description
     * @return ID of object
     */
    UUID commit(LocalDateTime timestamp, UUID committerId, UUID systemId, String description);

    /**
     * Commit the object with the necessary metadata, which will be derived from the contribution.
     *
     * @param timestamp    Time of operation
     * @param contribution Given contribution to use and derive audit data from
     * @param audit
     * @return ID of object
     */
    UUID commit(LocalDateTime timestamp, UUID contribution, UUID audit);

    /**
     * Update the object with the necessary metadata.
     * @param timestamp Time of operation
     * @param committerId Audit committer
     * @param systemId Audit system
     * @param description (Optional) Audit description
     * @param changeType Specific change type, because there are more than DELETED.
     * @return Boolean representing success of update
     */
    boolean update(
            LocalDateTime timestamp,
            UUID committerId,
            UUID systemId,
            String description,
            ContributionChangeType changeType);

    /**
     * Update the object with the necessary metadata, which will be derived from the contribution.
     *
     * @param timestamp    Time of operation
     * @param contribution Given contribution to use and derive audit data from
     * @param audit
     * @return Boolean representing success of update
     */
    boolean update(LocalDateTime timestamp, UUID contribution, UUID audit);

    /**
     * Delete the object with the necessary metadata.
     * @param timestamp Time of operation
     * @param committerId Audit committer
     * @param systemId Audit system
     * @param description (Optional) Audit description
     * @return Number of deleted objects
     */
    int delete(LocalDateTime timestamp, UUID committerId, UUID systemId, String description);

    /**
     * Delete the object with the necessary metadata, which will be derived from the contribution.
     *
     * @param timestamp    Time of operation
     * @param contribution Given contribution to use and derive audit data from
     * @param audit
     * @return Number of deleted objects
     */
    int delete(LocalDateTime timestamp, UUID contribution, UUID audit);
}
