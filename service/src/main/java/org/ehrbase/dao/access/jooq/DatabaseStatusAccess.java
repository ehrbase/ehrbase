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
package org.ehrbase.dao.access.jooq;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_DatabaseStatusAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.Routines;
import org.jooq.DSLContext;

public class DatabaseStatusAccess extends DataAccess implements I_DatabaseStatusAccess {

    public DatabaseStatusAccess(DSLContext dslContext, ServerConfig serverConfig) {
        super(dslContext, null, null, serverConfig);
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    public static String getDatabaseVersion(I_DomainAccess domainAccess) {
        return Routines.getSystemVersion(domainAccess.getContext().configuration());
    }
}
