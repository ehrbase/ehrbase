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

import java.sql.Connection;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.support.ServiceDataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

/**
 * Helper to hold SQL context and knowledge cache reference
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_DomainAccess {

    String PG_POOL = "PG_POOL";
    String DBCP2_POOL = "DBCP2_POOL";
    String KEY_MAX_IDLE = "max_idle";
    String KEY_MAX_ACTIVE = "max_active";
    String KEY_REMOVE_ABANDONNED = "remove_abandonned";
    String KEY_REMOVE_ABANDONNED_TIMEOUT = "remove_abandonned_timeout";
    String KEY_TEST_ON_BORROW = "test_on_borrow";
    String KEY_LOG_ABANDONNED = "log_abandonned";
    String KEY_AUTO_RECONNECT = "auto_reconnect";

    String KEY_DIALECT = "dialect";
    String KEY_URL = "url";
    String KEY_LOGIN = "login";
    String KEY_PASSWORD = "password";
    String KEY_KNOWLEDGE = "knowledge";
    String KEY_HOST = "host";
    String KEY_PORT = "port";
    String KEY_MAX_CONNECTION = "max_connection";
    String KEY_INITIAL_CONNECTIONS = "initial_connections";
    String KEY_CONNECTION_MODE = "connection_mode";
    String KEY_DATABASE = "database";
    String KEY_WAIT_MS = "wait_milliseconds";
    String KEY_SCHEMA = "schema";
    String KEY_SET_POOL_PREPARED_STATEMENTS = "set_pool_prepared_statement";
    String KEY_SET_MAX_PREPARED_STATEMENTS = "set_max_prepared_statements";
    String KEY_INTROSPECT_CACHE = "introspect";

    static I_DomainAccess getInstance(I_DomainAccess dataAccess) {
        return new ServiceDataAccess(dataAccess);
    }

    /**
     * get jOOQ SQL dialect
     *
     * @return SQLDialect
     * @see org.jooq.SQLDialect
     */
    SQLDialect getDialect();

    /**
     * get the JDBC connection to the DB
     *
     * @return Connection
     * @see java.sql.Connection
     */
    Connection getConnection();

    /**
     * have JOOQ release the DB connection
     */
    void releaseConnection(Connection connection);

    /**
     * get the jOOQ DSL context to perform DB queries
     *
     * @return DSLContext
     * @see org.jooq.DSLContext
     */
    DSLContext getContext();

    /**
     * get the interface to the current knowledge cache
     *
     * @return I_KnowledgeCache
     * @see I_KnowledgeCache
     */
    I_KnowledgeCache getKnowledgeManager();

    IntrospectService getIntrospectService();

    ServerConfig getServerConfig();

    DataAccess getDataAccess();
}
