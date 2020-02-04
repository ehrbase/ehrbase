/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

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

import org.ehrbase.dao.access.jooq.StatusAccess;

import java.util.UUID;

/**
 * Status access layer interface<br>
 * the status entry holds data pertaining to an Ehr owner, generally a patient
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_StatusAccess extends I_SimpleCRUD {

    /**
     * retrieve a status by an identified party id
     *
     * @param domainAccess    SQL access
     * @param partyIdentified Id of an identified party
     * @return UUID
     */
    static I_StatusAccess retrieveInstance(I_DomainAccess domainAccess, UUID partyIdentified) {
        return StatusAccess.retrieveInstance(domainAccess, partyIdentified);
    }

    /**
     * retrieve a status for a named subject (patient)<br>
     * NB. for security reason, most deployment will not provide an explicit subject name, this method is provided
     * for small deployment or test purpose.
     *
     * @param domainAccess SQL access
     * @param partyName    a subject name
     * @return UUID
     */
    static I_StatusAccess retrieveInstanceByNamedSubject(I_DomainAccess domainAccess, String partyName) {
        return StatusAccess.retrieveInstanceByNamedSubject(domainAccess, partyName);
    }

    UUID getId();
}
