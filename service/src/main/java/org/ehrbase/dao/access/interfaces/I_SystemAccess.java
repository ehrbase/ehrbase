/*
 * Copyright (c) 2015-2022 vitasystems GmbH and Hannover Medical School.
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

import static org.ehrbase.jooq.pg.Tables.SYSTEM;

import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.jooq.SystemAccess;

/**
 * System access layer interface.
 *
 * @author Christian Chevalley
 * @author Jake Smolka
 * @since 1.0
 */
@SuppressWarnings("java:S114")
public interface I_SystemAccess extends I_SimpleCRUD {

    static I_SystemAccess getInstance(I_DomainAccess domainAccess, String description, String settings) {
        return new SystemAccess(domainAccess, description, settings);
    }

    /**
     * retrieve a system entry by its Id
     *
     * @param domainAccess SQL access
     * @param id           UUID
     * @return UUID
     */
    static I_SystemAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        return SystemAccess.retrieveInstance(domainAccess, id);
    }

    /**
     * retrieve the Id of a system by name (or settings)
     *
     * @param domainAccess SQL access
     * @param settings     a string describing the system (arbitrary convention)
     * @return UUID or null if not found
     * @throws IllegalArgumentException if couldn't retrieve instance with given settings
     */
    static UUID retrieveInstanceId(I_DomainAccess domainAccess, String settings) {
        return SystemAccess.retrieveInstanceId(domainAccess, settings);
    }

    /**
     * Helper to retrieve or storeComposition a local host identifier<br>
     * the local settings is a combination of MAC address and hostname:<br>
     * for example: 44-87-FC-A9-B4-B2|TEST-PC<br>
     * if the system is not yet in the DB it is created, it is retrieved otherwise
     *
     * @return UUID of local system from DB
     * @throws InternalServerException when accessing network interface failed
     */
    static UUID createOrRetrieveLocalSystem(I_DomainAccess domainAccess) {
        return SystemAccess.createOrRetrieveLocalSystem(domainAccess);
    }

    /**
     * Try to retrieve system with given input. If not available create instance.
     *
     * @param domainAccess Data Access Object
     * @param description  Optional description, can be NULL to use default
     * @param settings     a string describing the system (arbitrary convention)
     * @return UUID of system entry
     */
    static UUID createOrRetrieveInstanceId(I_DomainAccess domainAccess, String description, String settings) {
        return SystemAccess.createOrRetrieveInstanceId(domainAccess, description, settings);
    }

    static Integer delete(I_DomainAccess domainAccess, UUID id) {
        return domainAccess.getContext().delete(SYSTEM).where(SYSTEM.ID.eq(id)).execute();
    }

    UUID getId();

    String getSettings();

    String getDescription();
}
