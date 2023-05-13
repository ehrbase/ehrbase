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
package org.ehrbase.service;

import static org.ehrbase.api.tenant.TenantAuthentication.DEFAULT_SYS_TENANT;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.jooq.ExecuteContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class PersistenceConfig {

    static class ExceptionTranslator extends DefaultExecuteListener {
        @Override
        public void exception(ExecuteContext context) {
            SQLDialect dialect = context.configuration().dialect();
            SQLExceptionTranslator translator = new SQLErrorCodeSQLExceptionTranslator(dialect.name());
            context.exception(
                    translator.translate("Access database using Jooq", context.sql(), context.sqlException()));
        }
    }

    @Qualifier("dataSource")
    @Autowired
    private DataSource dataSource;

    public TransactionAwareDataSourceProxy transactionAwareDataSource() {
        return new TransactionAwareDataSourceProxy(dataSource);
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public DataSourceConnectionProvider connectionProvider() {
        return new DataSourceConnectionProvider(transactionAwareDataSource()) {
            public static final String DB_SET_TENANT_ID = "select  set_config('ehrbase.current_tenant', ?, false)";

            public Connection acquire() {
                Connection connection = super.acquire();
                try (PreparedStatement sql = connection.prepareStatement(DB_SET_TENANT_ID)) {
                    sql.setString(1, Short.toString(DEFAULT_SYS_TENANT));
                    sql.execute();
                    return connection;
                } catch (SQLException e) {
                    try {
                        super.release(connection);
                    } finally {
                        throw new DataAccessException("Failed to set default tenant", e);
                    }
                }
            }
        };
    }

    @Bean
    public ExceptionTranslator exceptionTransformer() {
        return new ExceptionTranslator();
    }

    @Bean
    @Primary
    public DefaultDSLContext dsl(DefaultConfiguration cfg) {
        return new DefaultDSLContext(cfg);
    }

    @Bean
    public DefaultConfiguration configuration(DataSourceConnectionProvider provider) {
        DefaultConfiguration jooqConfiguration = new DefaultConfiguration();
        jooqConfiguration.set(provider);
        jooqConfiguration.set(new DefaultExecuteListenerProvider(exceptionTransformer()));

        SQLDialect dialect = SQLDialect.POSTGRES;
        jooqConfiguration.set(dialect);

        return jooqConfiguration;
    }
}
