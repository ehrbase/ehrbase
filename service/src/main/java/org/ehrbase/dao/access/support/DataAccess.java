/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School), Luis Marco-Ruiz (Hannover Medical School).

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
package org.ehrbase.dao.access.support;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.IntrospectService;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.postgresql.ds.PGPoolingDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;


/**
 * Created by Christian Chevalley on 4/21/2015.
 */
public abstract class DataAccess implements I_DomainAccess {

    Logger logger = LogManager.getLogger(DataAccess.class);

    //    protected Connection connection;
    protected DSLContext context;
    protected final I_KnowledgeCache knowledgeManager;
    protected final IntrospectService introspectService;

    protected static String serverNodeId = System.getProperty("server.node.name");

    public DataAccess(SQLDialect dialect, String DBURL, String login, String password, I_KnowledgeCache knowledgeManager, IntrospectService introspectService) {
        //setup connection
        setJDBC(dialect, DBURL, login, password);
        this.knowledgeManager = knowledgeManager;
        this.introspectService = introspectService;
    }

    /**
     * setup a connection handler from properties
     * the following key/value pairs are allowed:
     * <ul>
     * <li>sql_dialect - a valid string describing the sql dialect</li>
     * <li>url - a jdbc URL to connect to the DB</li>
     * <li>login - a valid login name</li>
     * <li>password - password to authenticate</li>
     * </ul>
     *
     * @param properties
     * @see SQLDialect
     * @see java.sql.DriverManager
     */

    public DataAccess(Map<String, Object> properties) {

        String serverConnectionMode = (String) properties.get(I_DomainAccess.KEY_CONNECTION_MODE);

        if (serverConnectionMode != null && serverConnectionMode.equals(I_DomainAccess.PG_POOL)) {
            setPGPoolParameters(properties);
            logger.info("Database connection uses POSTGRES CONNECTION POOLING");
        } else if (serverConnectionMode != null && serverConnectionMode.equals(I_DomainAccess.DBCP2_POOL)) {
            setDBCP2Parameters(properties);
            logger.info("Database connection uses DBCP2 CONNECTION POOLING");
        }
        //default
        else {
            SQLDialect dialect = SQLDialect.valueOf((String) properties.get(I_DomainAccess.KEY_DIALECT));
            String url = (String) properties.get(I_DomainAccess.KEY_URL);
            String login = (String) properties.get(I_DomainAccess.KEY_LOGIN);
            String password = (String) properties.get(I_DomainAccess.KEY_PASSWORD);
            setJDBC(dialect, url, login, password);
            logger.info("Database connection uses JDBC DRIVER");
        }

        knowledgeManager = (I_KnowledgeCache) properties.get(I_DomainAccess.KEY_KNOWLEDGE);
        introspectService = (IntrospectService) properties.get(I_DomainAccess.KEY_INTROSPECT_CACHE);


    }

    public DataAccess(DSLContext context, I_KnowledgeCache knowledgeManager, IntrospectService introspectService) {
//        this.connection = context == null ? null : context.configuration().connectionProvider().acquire();
        this.context = context;
        this.knowledgeManager = knowledgeManager;
        this.introspectService = introspectService;
    }

    public DataAccess(I_DomainAccess domainAccess) {
//        this.connection = domainAccess.getConnection();
        this.context = domainAccess.getContext();
        this.knowledgeManager = domainAccess.getKnowledgeManager();
        this.introspectService = domainAccess.getIntrospectService();

    }

    private void setJDBC(SQLDialect dialect, String DBURL, String login, String password) {
        //use a driver
        Connection connection;
        try {
            connection = DriverManager.getConnection(DBURL, login, password);
        } catch (SQLException e) {
            throw new IllegalArgumentException("SQL exception occurred while connecting:" + e);
        }

        if (connection == null)
            throw new IllegalArgumentException("Could not connect to DB");

        this.context = DSL.using(connection, dialect);

    }

    private void setPGPoolParameters(Map<String, Object> properties) {
//        Connection connection;

        SQLDialect dialect = SQLDialect.valueOf((String) properties.get(I_DomainAccess.KEY_DIALECT));
        String host = (String) properties.get(I_DomainAccess.KEY_HOST);
        String port = (String) properties.get(I_DomainAccess.KEY_PORT);
        String login = (String) properties.get(I_DomainAccess.KEY_LOGIN);
        String password = (String) properties.get(I_DomainAccess.KEY_PASSWORD);
        String database = (String) properties.get(I_DomainAccess.KEY_DATABASE);
//        String schema = (String)properties.get(I_DomainAccess.KEY_SCHEMA);
        Integer max_connection = 10;
        if (properties.containsKey(I_DomainAccess.KEY_MAX_CONNECTION))
            max_connection = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_MAX_CONNECTION));
        Integer initial_connections = null;
        if (properties.containsKey(I_DomainAccess.KEY_INITIAL_CONNECTIONS))
            initial_connections = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_INITIAL_CONNECTIONS));


        //use a datasource
        try {
            PGPoolingDataSource source = new PGPoolingDataSource();
            source.setDataSourceName("pg_pool");
            source.setServerName(host);
            source.setPortNumber(Integer.parseInt(port));
            source.setUser(login);
            source.setPassword(password);
            source.setMaxConnections(max_connection);
            if (initial_connections != null)
                source.setInitialConnections(initial_connections);

            source.setDatabaseName(database);
            this.context = DSL.using(source, dialect);
            logger.info("PG_POOL settings:");
            logger.info("host:" + host);
            logger.info("port:" + port);
            logger.info("database:" + database);
            logger.info("max_connections:" + max_connection);


        } catch (Exception e) {
            throw new IllegalArgumentException("PG_POOL: SQL exception occurred while connecting:" + e);
        }

//        thtext = DSL.using(source, dialect);
//
//
//        if (connection == null)
//            throw new IllegalArgumentException("PG_POOL: Could not connect to DB, please check your parameters");


    }

    private void setDBCP2Parameters(Map<String, Object> properties) {
        SQLDialect dialect = SQLDialect.valueOf((String) properties.get(I_DomainAccess.KEY_DIALECT));
        String url = (String) properties.get(I_DomainAccess.KEY_URL);
        String login = (String) properties.get(I_DomainAccess.KEY_LOGIN);
        String password = (String) properties.get(I_DomainAccess.KEY_PASSWORD);
//        String database = (String)properties.get(I_DomainAccess.KEY_DATABASE);
////        String schema = (String)properties.get(I_DomainAccess.KEY_SCHEMA);
        Integer max_connection = 10;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_MAX_CONNECTION)))
            max_connection = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_MAX_CONNECTION));
        Long waitMs = 50L;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_WAIT_MS)))
            waitMs = Long.parseLong((String) properties.get(I_DomainAccess.KEY_WAIT_MS));

        //more optional parameters
        Integer max_idle = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_MAX_IDLE)))
            max_idle = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_MAX_IDLE));
        Integer max_active = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_MAX_ACTIVE)))
            max_active = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_MAX_ACTIVE));
        Boolean testOnBorrow = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_TEST_ON_BORROW)))
            testOnBorrow = Boolean.parseBoolean((String) properties.get(I_DomainAccess.KEY_TEST_ON_BORROW));
        Boolean setPoolPreparedStatements = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_SET_POOL_PREPARED_STATEMENTS)))
            setPoolPreparedStatements = Boolean.parseBoolean((String) properties.get(I_DomainAccess.KEY_SET_POOL_PREPARED_STATEMENTS));
        Integer setMaxPreparedStatements = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_SET_MAX_PREPARED_STATEMENTS)))
            setMaxPreparedStatements = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_SET_MAX_PREPARED_STATEMENTS));

        Boolean removeAbandonnned = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_REMOVE_ABANDONNED)))
            removeAbandonnned = Boolean.getBoolean((String) properties.get(I_DomainAccess.KEY_REMOVE_ABANDONNED));
        Integer removeAbandonnnedTimeout = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_REMOVE_ABANDONNED_TIMEOUT)))
            removeAbandonnnedTimeout = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_REMOVE_ABANDONNED_TIMEOUT));
        Boolean logAbandoned = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_LOG_ABANDONNED)))
            logAbandoned = Boolean.getBoolean((String) properties.get(I_DomainAccess.KEY_LOG_ABANDONNED));
        Boolean autoReconnect = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_AUTO_RECONNECT)))
            autoReconnect = Boolean.getBoolean((String) properties.get(I_DomainAccess.KEY_AUTO_RECONNECT));

        Integer initialConnections = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_INITIAL_CONNECTIONS)))
            initialConnections = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_INITIAL_CONNECTIONS));

        if (!dialect.equals(SQLDialect.POSTGRES))
            throw new IllegalArgumentException("At this stage only POSTGRES dialect is supported, please check your configuration");

        try {
            logger.info("DBCP2_POOL BasicDataSource (http://commons.apache.org/proper/commons-dbcp/configuration.html)");

            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl(url);
            logger.info("url: " + url);
            dataSource.setUsername(login);
            dataSource.setPassword(password);
            if (setMaxPreparedStatements != null) {
                dataSource.setMaxOpenPreparedStatements(setMaxPreparedStatements);
                logger.info("setMaxOpenPreparedStatements: " + setMaxPreparedStatements);
            }
            if (setPoolPreparedStatements != null) {
                dataSource.setPoolPreparedStatements(setPoolPreparedStatements);
                logger.info("setPoolPreparedStatements: " + setPoolPreparedStatements);
            }

            dataSource.setMaxTotal(max_connection);
            dataSource.setMaxWaitMillis(waitMs);

            if (max_idle != null) {
                dataSource.setMaxIdle(max_idle);
                logger.info("Pool max idle: " + max_idle);
            }

            if (testOnBorrow != null && testOnBorrow) {
                dataSource.setValidationQuery("SELECT 1");
                dataSource.setTestOnReturn(true);
                dataSource.setTestWhileIdle(true);
                dataSource.setTestOnBorrow(true);
                logger.info("Pool setDefaultTestOnBorrow: " + testOnBorrow);
            }
//            dataSource.setDataSourceName("ecis-"+url); //JNDI

            if (max_active != null) {
                logger.info("Pool max active: " + max_active);
                dataSource.setMaxTotal(max_active);
            }

            if (removeAbandonnned != null) {
                logger.info("setRemoveAbandonedOnBorrow: " + removeAbandonnned);
                dataSource.setRemoveAbandonedOnBorrow(removeAbandonnned);
            }

            if (removeAbandonnnedTimeout != null) {
                logger.info("removeAbandonnnedTimeout: " + removeAbandonnnedTimeout);
                dataSource.setRemoveAbandonedTimeout(removeAbandonnnedTimeout);
            }

            if (logAbandoned != null) {
                logger.info("logAbandoned: " + logAbandoned);
                dataSource.setLogAbandoned(logAbandoned);
            }

            if (initialConnections != null) {
                logger.info("setInitialSize (initialConnections): " + initialConnections);
                dataSource.setInitialSize(initialConnections);
            }

            this.context = DSL.using(dataSource, dialect);

            logger.info("Pool max_connections: " + max_connection);
            logger.info("Pool max_wait_millisec: " + waitMs);
            logger.info("");
        } catch (Exception e) {
            throw new IllegalArgumentException("DBCP2_POOL: Exception occurred while connecting:" + e);
        }
    }


    private void setDBCP2SharedDataSourceParameters(Map<String, Object> properties) throws Exception {
        SQLDialect dialect = SQLDialect.valueOf((String) properties.get(I_DomainAccess.KEY_DIALECT));
        String url = (String) properties.get(I_DomainAccess.KEY_URL);
        String login = (String) properties.get(I_DomainAccess.KEY_LOGIN);
        String password = (String) properties.get(I_DomainAccess.KEY_PASSWORD);
//        String database = (String)properties.get(I_DomainAccess.KEY_DATABASE);
////        String schema = (String)properties.get(I_DomainAccess.KEY_SCHEMA);
        Integer max_connection = 10;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_MAX_CONNECTION)))
            max_connection = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_MAX_CONNECTION));
        Long waitMs = 50L;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_WAIT_MS)))
            waitMs = Long.parseLong((String) properties.get(I_DomainAccess.KEY_WAIT_MS));

        //more optional parameters
        Integer max_idle = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_MAX_IDLE)))
            max_idle = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_MAX_IDLE));
        Integer max_active = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_MAX_ACTIVE)))
            max_active = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_MAX_ACTIVE));
        Boolean testOnBorrow = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_TEST_ON_BORROW)))
            testOnBorrow = Boolean.parseBoolean((String) properties.get(I_DomainAccess.KEY_TEST_ON_BORROW));
        Boolean setPoolPreparedStatements = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_SET_POOL_PREPARED_STATEMENTS)))
            setPoolPreparedStatements = Boolean.parseBoolean((String) properties.get(I_DomainAccess.KEY_SET_POOL_PREPARED_STATEMENTS));
        Integer setMaxPreparedStatements = null;
        if (StringUtils.isNotEmpty((String) properties.get(I_DomainAccess.KEY_SET_MAX_PREPARED_STATEMENTS)))
            setMaxPreparedStatements = Integer.parseInt((String) properties.get(I_DomainAccess.KEY_SET_MAX_PREPARED_STATEMENTS));

//        Boolean removeAbandonnned = null;
//        if (StringUtils.isNotEmpty((String)properties.get(I_DomainAccess.KEY_REMOVE_ABANDONNED)))
//            removeAbandonnned = Boolean.getBoolean((String)properties.get(I_DomainAccess.KEY_REMOVE_ABANDONNED));
//        Long removeAbandonnnedTimeout = null;
//        if (StringUtils.isNotEmpty((String)properties.get(I_DomainAccess.KEY_REMOVE_ABANDONNED_TIMEOUT)))
//            removeAbandonnnedTimeout = Long.parseLong((String) properties.get(I_DomainAccess.KEY_REMOVE_ABANDONNED_TIMEOUT));
//        Boolean logAbandoned = null;
//        if (StringUtils.isNotEmpty((String)properties.get(I_DomainAccess.KEY_LOG_ABANDONNED)))
//            logAbandoned = Boolean.getBoolean((String)properties.get(I_DomainAccess.KEY_LOG_ABANDONNED));
//        Boolean autoReconnect = null;
//        if (StringUtils.isNotEmpty((String)properties.get(I_DomainAccess.KEY_AUTO_RECONNECT)))
//            autoReconnect = Boolean.getBoolean((String)properties.get(I_DomainAccess.KEY_AUTO_RECONNECT));

//        Integer initial_connections = null;
//        if (properties.containsKey(I_DomainAccess.KEY_INITIAL_CONNECTIONS))
//            initial_connections = Integer.parseInt((String)properties.get(I_DomainAccess.KEY_INITIAL_CONNECTIONS));

        if (!dialect.equals(SQLDialect.POSTGRES))
            throw new IllegalArgumentException("At this stage only POSTGRES dialect is supported, please check your configuration");

        try {
            logger.info("DBCP2_POOL settings:");
            DriverAdapterCPDS cpds = new DriverAdapterCPDS();
            cpds.setDriver("org.postgresql.Driver");
            cpds.setUrl(url);
            logger.info("url: " + url);
            cpds.setUser(login);
            cpds.setPassword(password);
            if (setMaxPreparedStatements != null) {
                cpds.setMaxPreparedStatements(setMaxPreparedStatements);
                logger.info("setMaxPreparedStatements: " + setMaxPreparedStatements);
            }
            if (setPoolPreparedStatements != null) {
                cpds.setPoolPreparedStatements(setPoolPreparedStatements);
                logger.info("setPoolPreparedStatements: " + setPoolPreparedStatements);
            }


            SharedPoolDataSource dataSource = new SharedPoolDataSource();
            dataSource.setConnectionPoolDataSource(cpds);
            dataSource.setMaxTotal(max_connection);
            dataSource.setDefaultMaxWaitMillis(waitMs);

            if (max_idle != null) {
                cpds.setMaxIdle(max_idle);
                logger.info("Pool max idle: " + max_idle);
            }

            if (testOnBorrow != null && testOnBorrow) {
                dataSource.setValidationQuery("SELECT 1");
                dataSource.setDefaultTestOnReturn(true);
                dataSource.setDefaultTestWhileIdle(true);
                dataSource.setDefaultTestOnBorrow(true);
            }
//            dataSource.setDataSourceName("ecis-"+url); //JNDI
            if (testOnBorrow != null) {
                logger.info("Pool setDefaultTestOnBorrow: " + testOnBorrow);
                dataSource.setDefaultTestOnBorrow(testOnBorrow);
            }
            if (max_active != null) {
                logger.info("Pool max active: " + max_active);
                dataSource.setDefaultMaxTotal(max_active);
            }
            ;

            this.context = DSL.using(dataSource, dialect);

            logger.info("Pool max_connections: " + max_connection);
            logger.info("Pool max_wait_millisec: " + waitMs);
            logger.info("");
        } catch (Exception e) {
            throw new IllegalArgumentException("DBCP2_POOL: Exception occurred while connecting:" + e);
        }
    }


    @Override
    public SQLDialect getDialect() {
        return context.dialect();
    }

    @Override
    public Connection getConnection() {
        return context.configuration().connectionProvider().acquire();
//        return connection;
    }

    @Override
    public void releaseConnection(Connection connection) {
        context.configuration().connectionProvider().release(connection);
    }

    @Override
    public DSLContext getContext() {
        return context;
    }

    @Override
    public I_KnowledgeCache getKnowledgeManager() {
        return knowledgeManager;
    }

    @Override
    public IntrospectService getIntrospectService() {
        return introspectService;
    }


    @Override
    public String getServerNodeId() {
        if (serverNodeId == null || serverNodeId.length() == 0)
            return "local.ethercis.com";
        return serverNodeId;
    }

}
