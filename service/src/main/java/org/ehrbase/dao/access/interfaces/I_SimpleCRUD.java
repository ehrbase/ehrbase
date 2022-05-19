/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Generic CRUD Interface<br>
 * The CRUD operation are performed on the current access layer instance<br>
 * NB: read is implemented as a static (generally as retrieveInstanceForExistingFolder(...))
 * Created by Christian Chevalley on 4/10/2015.
 */
public interface I_SimpleCRUD {
    /**
     * storeComposition a new entry in the DB
     *
     * @return the UUID of the newly created record
     * @throws RuntimeException
     */
    UUID commit(Timestamp transactionTime);

    /**
     * storeComposition a new entry in the DB, using a default transaction time<br>
     * only implemented with "root" tables: ehr, contribution and composition!
     *
     * @return the UUID of the newly created record
     * @throws RuntimeException
     */
    UUID commit();

    /**
     * updateComposition the current entry<br>
     * depending on the implementation, records are updated only if one or more field(s) have been changed
     *
     * @return
     * @throws RuntimeException
     */
    Boolean update(Timestamp transactionTime);

    /**
     * updateComposition the current entry even if the record is not modified<br>
     * intended to be used with temporal tables to ensure that a set of interdependent tables are
     * updated in sync. This approach is used to simplify versions retrieval.
     *
     * @param force
     * @return
     * @throws RuntimeException
     */
    Boolean update(Timestamp transactionTime, boolean force);

    /**
     * updateComposition the current entry using time now as the system transaction time<br>
     * only implemented at root level object (contribution, composition, ehr).
     * depending on the implementation, records are updated only if one or more field(s) have been changed
     *
     * @return
     * @throws RuntimeException
     */
    Boolean update();

    /**
     * updateComposition the current entry using time now as the system transaction time<br>
     * only implemented at root level object (contribution, composition, ehr).
     * depending on the implementation, records are updated only if one or more field(s) have been changed
     *
     * @return
     * @throws RuntimeException
     */
    Boolean update(Boolean force);

    /**
     * delete the Versioned Object associated with the instance implementing this Data Access Interface.
     * Relies on ON DELETE CASCADE
     *
     * @return
     * @throws RuntimeException
     */
    Integer delete();
}
