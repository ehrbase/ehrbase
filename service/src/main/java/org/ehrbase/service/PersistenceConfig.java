/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service;

import com.sun.istack.NotNull;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.postgresql.PGProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jooq.JooqProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(JooqProperties.class)
public class PersistenceConfig {

    private static Logger log = LoggerFactory.getLogger(PersistenceConfig.class);

    /** Conditional for presents of <code>spring.datasource.common</code> or <code>spring.datasource.aql</code> with any config */
    static class OnSeparateDataSourcesEnabled implements Condition {

        @Override
        public boolean matches(ConditionContext conditionContext, @NotNull AnnotatedTypeMetadata metadata) {

            return ((AbstractEnvironment) conditionContext.getEnvironment())
                    .getPropertySources().stream()
                            .filter(MapPropertySource.class::isInstance)
                            .map(MapPropertySource.class::cast)
                            .map(MapPropertySource::getSource)
                            .map(Map::keySet)
                            .flatMap(Set::stream)
                            .peek(propKey -> {
                                if (propKey.startsWith("spring.datasource.common")
                                        || propKey.startsWith("spring.datasource.aql")) {
                                    System.out.println(propKey);
                                }
                            })
                            .anyMatch(propKey -> propKey.startsWith("spring.datasource.common")
                                    || propKey.startsWith("spring.datasource.aql"));
        }
    }

    //    private final DataSourceProperties dataSourceProperties;
    private final JooqProperties jooqProperties;

    public PersistenceConfig(@Autowired JooqProperties jooqProperties) {
        this.jooqProperties = jooqProperties;
    }

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.common")
    DataSourceProperties dataSourcePropertiesCommon() {
        return dataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.aql")
    DataSourceProperties dataSourcePropertiesAql() {
        return dataSourceProperties();
    }

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.common.hikari")
    HikariConfig hikariConfigCommon() {
        return hikariConfig();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.aql.hikari")
    HikariConfig hikariConfigAql() {
        return hikariConfig();
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return buildDataSource("COMMON", dataSourcePropertiesCommon(), hikariConfigCommon(), __ -> {});
    }

    @Bean(name = "dataSourceAQL")
    @Conditional(OnSeparateDataSourcesEnabled.class)
    public DataSource dataSourceAQL() {
        return buildDataSource("AQL", dataSourcePropertiesAql(), hikariConfigAql(), hikariDataSource -> {
            // ensure every new transaction is read only by default
            hikariDataSource.setAutoCommit(true);
            hikariDataSource.setConnectionInitSql("set default_transaction_read_only to on;");
        });
    }

    private static DataSource buildDataSource(
            String poolName,
            DataSourceProperties dataSourceProperties,
            HikariConfig hikariConfig,
            Consumer<HikariDataSource> customizer) {

        log.info("Creating DataSource [{}]", poolName);

        final HikariDataSource hikariDataSource = dataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        hikariConfig.copyStateTo(hikariDataSource); // apply *.hikari configurations
        // re-apply common data source property that got lost during #copyStateTo
        hikariDataSource.setJdbcUrl(dataSourceProperties.getUrl());
        hikariDataSource.setUsername(dataSourceProperties.getUsername());
        hikariDataSource.setPassword(dataSourceProperties.getPassword());
        hikariDataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        // apply unique name per pool for debugging purposes
        hikariDataSource.setPoolName(poolName);
        // can be used fo debugging active connections using [SELECT * FROM pg_stat_activity;]
        hikariDataSource.addDataSourceProperty(PGProperty.APPLICATION_NAME.getName(), "EHRbase " + poolName);
        customizer.accept(hikariDataSource);
        return hikariDataSource;
    }

    /** Unifies creation of new transaction manager */
    public interface DataSourceTransactionManagerFactory {

        DataSourceTransactionManager newInstance(DataSource dataSource);
    }

    @Bean
    DataSourceTransactionManagerFactory transactionManagerFactory() {
        return DataSourceTransactionManager::new;
    }

    @Primary
    @Bean
    public PlatformTransactionManager transactionManagerCommon(
            DataSource dataSource,
            DataSourceTransactionManagerFactory dataSourceTransactionManagerFactory,
            ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {

        DataSourceTransactionManager transactionManager = dataSourceTransactionManagerFactory.newInstance(dataSource);
        transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
        return transactionManager;
    }

    @Bean(name = "transactionManagerAQL")
    @Conditional(OnSeparateDataSourcesEnabled.class)
    public PlatformTransactionManager transactionManagerAql(
            @Qualifier("dataSourceAQL") DataSource dataSource,
            DataSourceTransactionManagerFactory dataSourceTransactionManagerFactory,
            ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {

        DataSourceTransactionManager transactionManager = dataSourceTransactionManagerFactory.newInstance(dataSource);
        // read only is enforced during connection time using default_transaction_read_only
        transactionManager.setEnforceReadOnly(false);
        transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
        return transactionManager;
    }

    @Primary
    @Bean
    public DSLContext dsl(DataSource dataSource) {

        DataSourceConnectionProvider dataSourceConnectionProvider = jooqConnectionProvider(dataSource);
        org.jooq.Configuration jooqConfiguration = jooqConfiguration(jooqProperties, dataSourceConnectionProvider);
        return new DefaultDSLContext(jooqConfiguration);
    }

    @Bean("jooqDslAQL")
    public DSLContext aqlDsl(
            DSLContext defaultDslContext,
            @Autowired(required = false) @Qualifier("dataSourceAQL") DataSource dataSourceAQL) {

        return Optional.ofNullable(dataSourceAQL)
                .map(dataSource -> (DSLContext)
                        new DefaultDSLContext(jooqConfiguration(jooqProperties, jooqConnectionProvider(dataSource))))
                .orElse(defaultDslContext);
    }

    public static DataSourceConnectionProvider jooqConnectionProvider(DataSource dataSource) {
        TransactionAwareDataSourceProxy transactionAwareDataSource = new TransactionAwareDataSourceProxy(dataSource);
        return new DataSourceConnectionProvider(transactionAwareDataSource);
    }

    private static org.jooq.Configuration jooqConfiguration(
            JooqProperties jooqProperties, DataSourceConnectionProvider provider) {

        DefaultConfiguration jooqConfiguration = new DefaultConfiguration();
        jooqConfiguration.set(provider);
        jooqConfiguration.set(new DefaultExecuteListenerProvider(new ExceptionTranslator()));
        jooqConfiguration.set(jooqProperties.determineSqlDialect(provider.dataSource()));

        return jooqConfiguration;
    }

    private static class ExceptionTranslator implements ExecuteListener {
        @Override
        public void exception(ExecuteContext context) {
            SQLDialect dialect = context.configuration().dialect();
            SQLException throwable = context.sqlException();
            if (throwable != null) {
                SQLExceptionTranslator translator = new SQLErrorCodeSQLExceptionTranslator(dialect.name());
                context.exception(translator.translate("Access database using Jooq", context.sql(), throwable));
            }
        }
    }
}
