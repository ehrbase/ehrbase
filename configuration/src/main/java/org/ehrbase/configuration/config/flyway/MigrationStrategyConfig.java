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
package org.ehrbase.configuration.config.flyway;

import java.util.Map;
import java.util.function.Consumer;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MigrationStrategyConfig {

    private static final Logger log = LoggerFactory.getLogger(MigrationStrategyConfig.class);

    public enum MigrationStrategy {
        DISABLED(f -> {}),
        MIGRATE(Flyway::migrate),
        VALIDATE(Flyway::validate);

        private final Consumer<Flyway> strategy;

        MigrationStrategy(Consumer<Flyway> strategy) {
            this.strategy = strategy;
        }

        void applyStrategy(Flyway flyway) {
            strategy.accept(flyway);
        }
    }

    @Value("${spring.flyway.ehr-schema:ehr}")
    private String ehrSchema;

    @Value("${spring.flyway.ext-schema:ext}")
    private String extSchema;

    @Value("${spring.flyway.ehr-location:classpath:db/migration/ehr}")
    private String ehrLocation;

    @Value("${spring.flyway.ext-location:classpath:db/migration/ext}")
    private String extLocation;

    @Value("${spring.flyway.ext-strategy:MIGRATE}")
    private MigrationStrategy extStrategy = MigrationStrategy.MIGRATE;

    @Value("${spring.flyway.ehr-strategy:MIGRATE}")
    private MigrationStrategy ehrStrategy = MigrationStrategy.MIGRATE;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            if (extStrategy != MigrationStrategy.DISABLED) {
                extStrategy.applyStrategy(setSchema(flyway, extSchema)
                        .locations(extLocation)
                        // ext was not yet managed by flyway
                        .baselineOnMigrate(true)
                        .baselineVersion("1")
                        .placeholders(Map.of("extSchema", extSchema))
                        .load());
            } else {
                log.info("Flyway migration for schema 'ext' is disabled");
            }
            if (ehrStrategy != MigrationStrategy.DISABLED) {
                ehrStrategy.applyStrategy(setSchema(flyway, ehrSchema)
                        .placeholders(Map.of("ehrSchema", ehrSchema))
                        .locations(ehrLocation)
                        .load());
            } else {
                log.info("Flyway migration for schema 'ehr' is disabled");
            }
        };
    }

    private FluentConfiguration setSchema(Flyway flyway, String schema) {
        FluentConfiguration fluentConfiguration = Flyway.configure()
                .dataSource(flyway.getConfiguration().getDataSource())
                .schemas(schema);
        return fluentConfiguration;
    }
}
