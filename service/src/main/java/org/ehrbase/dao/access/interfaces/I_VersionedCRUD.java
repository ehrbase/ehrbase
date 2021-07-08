/*
 * Copyright (c) 2021 Jake Smolka (Hannover Medical School) and Vitasystems GmbH.
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

package org.ehrbase.dao.access.interfaces;

import java.sql.Timestamp;
import java.util.UUID;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;

// TODO-526: omitting `force` attributes for now. great change they are only called with like `false` anyway. Add one method with `force` each, if necessary.

/**
 * Common interface for versioned objects, like compositions, folders and statuses.
 *
 */
public interface I_VersionedCRUD {

  // TODO-526: add docs etc.

  /**
   * Commit the object with the necessary metadata.
   * @param timestamp
   * @param committerId
   * @param systemId
   * @param description
   * @return
   */
  UUID commit(Timestamp timestamp, UUID committerId, UUID systemId, String description);

  /**
   * Commit the object with the necessary metadata, which will be derived from the contribution.
   * @param timestamp
   * @param contribution
   * @return
   */
  UUID commit(Timestamp timestamp, UUID contribution);

  /**
   *
   * @param timestamp
   * @param committerId
   * @param systemId
   * @param description
   * @param changeType Specific change type, because there are more than DELETED.
   * @return
   */
  Boolean update(Timestamp timestamp, UUID committerId, UUID systemId, String description, ContributionChangeType changeType);

  Boolean update(Timestamp timestamp, UUID contribution);

  /**
   *
   */
  Integer delete(Timestamp timestamp, UUID committerId, UUID systemId, String description);

  Integer delete(Timestamp timestamp, UUID contribution);

}
