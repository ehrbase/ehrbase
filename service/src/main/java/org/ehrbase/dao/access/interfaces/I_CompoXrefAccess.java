/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School).

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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
import java.util.Map;
import java.util.UUID;

/**
 * Deals with composition links. For example to link an ACTION with an INSTRUCTION with  or an OBSERVATION
 * Created by christian on 9/12/2016.
 */
public interface I_CompoXrefAccess {

    /**
     * Retrieves list of children matching given parent/master ID
     *
     * @param masterUid parent or master {@link UUID}
     * @return A list of children represented as map, where each child has its system transaction time as value
     */
    Map<UUID, Timestamp> getLinkList(UUID masterUid);

    /**
     * Retrieve only the latest (by system transaction time) of children matching given parent/master ID
     * @param masterUid parent or master {@link UUID}
     * @return {@link UUID} of newest child
     */
    UUID getLastLink(UUID masterUid);

    /**
     * Insert a new link between given master and child
     * @param masterUid parent or master {@link UUID}
     * @param childUid child {@link UUID}
     * @return the number of inserted records
     */
    int setLink(UUID masterUid, UUID childUid);
}
