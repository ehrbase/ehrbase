/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.test;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.jooq.tools.jdbc.SingleConnectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class EhrbasePostgreSQLContainer extends JdbcDatabaseContainer<EhrbasePostgreSQLContainer> {

    private final Logger logger = LoggerFactory.getLogger("\uD83D\uDC33 psql");
    private static EhrbasePostgreSQLContainer sharedContainer;

    public static synchronized EhrbasePostgreSQLContainer sharedInstance() {
        if (sharedContainer == null) {
            sharedContainer = new EhrbasePostgreSQLContainer();
            sharedContainer.start();
        }
        return sharedContainer;
    }

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("postgres");
    public static final Integer POSTGRESQL_PORT = 5432;
    private String databaseName;
    private String username;
    private String password;

    public EhrbasePostgreSQLContainer() {
        this(DEFAULT_IMAGE_NAME.withTag("18"));
    }

    public EhrbasePostgreSQLContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        this.databaseName = "ehrbase";
        this.username = "ehrbase";
        this.password = "ehrbase";
        waitStrategy = new LogMessageWaitStrategy()
                .withRegEx(".*database system is ready to accept connections.*\\s")
                .withTimes(2)
                .withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS));
        addExposedPort(POSTGRESQL_PORT);
        withLogConsumer(outputFrame -> logger.info(outputFrame.getUtf8String().trim()));
        withEnv("POSTGRES_DB", databaseName);
        withEnv("POSTGRES_USER", username);
        withEnv("POSTGRES_PASSWORD", password);
    }

    /**
     * @deprecated
     */
    @Override
    @SuppressWarnings("deprecation")
    @Deprecated
    protected @NotNull Set<Integer> getLivenessCheckPorts() {
        return super.getLivenessCheckPorts();
    }

    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    public String getJdbcUrl() {
        String additionalUrlParams = this.constructUrlParameters("?", "&");
        return "jdbc:postgresql://" + this.getHost() + ":" + this.getMappedPort(POSTGRESQL_PORT) + "/"
                + this.databaseName + additionalUrlParams;
    }

    @Override
    public String getDatabaseName() {
        return this.databaseName;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    public EhrbasePostgreSQLContainer withDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return this.self();
    }

    @Override
    public EhrbasePostgreSQLContainer withUsername(String username) {
        this.username = username;
        return this.self();
    }

    @Override
    public EhrbasePostgreSQLContainer withPassword(String password) {
        this.password = password;
        return this.self();
    }

    @Override
    protected void waitUntilContainerStarted() {
        this.getWaitStrategy().waitUntilReady(this);
    }

    @Override
    protected void runInitScriptIfRequired() {
        try (var conn = createConnection("")) {
            // Create required schemas and install extensions for PG18+
            try (Statement stmt = conn.createStatement()) {
                // Create schemas for the new 5-schema architecture
                stmt.execute("CREATE SCHEMA IF NOT EXISTS ext");
                stmt.execute("CREATE SCHEMA IF NOT EXISTS ehr");
                stmt.execute("CREATE SCHEMA IF NOT EXISTS ehr_system");
                stmt.execute("CREATE SCHEMA IF NOT EXISTS ehr_data");
                stmt.execute("CREATE SCHEMA IF NOT EXISTS ehr_views");
                stmt.execute("CREATE SCHEMA IF NOT EXISTS ehr_staging");

                // Install PG18 extensions in the ext schema
                stmt.execute("CREATE EXTENSION IF NOT EXISTS btree_gist SCHEMA ext");
                stmt.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm SCHEMA ext");
                stmt.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA ext");
                stmt.execute("CREATE EXTENSION IF NOT EXISTS ltree SCHEMA ext");

                // Create roles referenced by compliance migrations
                stmt.execute(
                        "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ehrbase_restricted') THEN CREATE ROLE ehrbase_restricted NOLOGIN; END IF; END $$");
                stmt.execute(
                        "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ehrbase_audit') THEN CREATE ROLE ehrbase_audit NOLOGIN; END IF; END $$");
                stmt.execute("GRANT ehrbase_restricted TO " + username);
                stmt.execute("GRANT ehrbase_audit TO " + username);

                // Set search path for test user
                stmt.execute("ALTER ROLE " + username + " SET search_path TO ehr_system, ehr_data, ehr_views, ext");
            }

            var ds = new SingleConnectionDataSource(conn);

            // Migrate ehr_system schema
            Flyway.configure()
                    .dataSource(ds)
                    .schemas("ehr_system")
                    .placeholders(Map.of("ehrSchema", "ehr_system"))
                    .locations("classpath:db/migration/ehr")
                    .load()
                    .migrate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
