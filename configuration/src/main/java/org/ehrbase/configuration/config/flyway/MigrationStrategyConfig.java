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
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MigrationStrategyConfig {

    @Value("${spring.flyway.ehr-schema:ehr}")
    private String ehrSchema;

    @Value("${spring.flyway.ext-schema:ext}")
    private String extSchema;

    @Value("${spring.flyway.ehr-location:classpath:db/migration/ehr}")
    private String ehrLocation;

    @Value("${spring.flyway.ext-location:classpath:db/migration/ext}")
    private String extLocation;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            setSchema(flyway, extSchema)
                    .locations(extLocation)
                    // ext was not yet managed by flyway
                    .baselineOnMigrate(true)
                    .baselineVersion("1")
                    .placeholders(Map.of("extSchema", extSchema))
                    .load()
                    .migrate();
            setSchema(flyway, ehrSchema)
                    .placeholders(Map.of("ehrSchema", ehrSchema))
                    .locations(ehrLocation)
                    .load()
                    .migrate();
        };
    }

    private FluentConfiguration setSchema(Flyway flyway, String schema) {
        FluentConfiguration fluentConfiguration = Flyway.configure()
                .dataSource(flyway.getConfiguration().getDataSource())
                .schemas(schema);
        return fluentConfiguration;
    }
}
