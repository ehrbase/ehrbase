/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.service;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class ConnectionPoolService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${system.type}")
    private String systemType;
    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.password}")
    private String datasourcePass;
    @Value("${spring.datasource.username}")
    private String datasourceUser;

    private DSLContext context;

    @PostConstruct
    public void init() {
        SQLDialect dialect = SQLDialect.POSTGRES_9_5;
        String url = datasourceUrl;
        String login = datasourceUser;
        String password = datasourcePass;

        Integer max_connection = 10;

        Long waitMs = 50L;


        //more optional parameters
        Integer max_idle = null;

        Integer max_active = null;

        Boolean testOnBorrow = null;

        Boolean setPoolPreparedStatements = null;

        Integer setMaxPreparedStatements = null;


        Boolean removeAbandonnned = null;

        Integer removeAbandonnnedTimeout = null;

        Boolean logAbandoned = null;

        Boolean autoReconnect = null;


        Integer initialConnections = null;


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

    public DSLContext getContext() {
        return context;
    }
}
