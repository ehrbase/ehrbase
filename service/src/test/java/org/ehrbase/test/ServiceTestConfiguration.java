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

import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.ehrbase.service.TimeProvider;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@TestConfiguration
@AutoConfigureCache
public class ServiceTestConfiguration {

    static {
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");
    }

    @PostConstruct
    public void initializeTestContext() {
        // Allow interaction with the Authentication via Authentication facade
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "integration-test",
                        "integration-test-principal",
                        List.of(new SimpleGrantedAuthority("integration-test-authority"))));
    }

    @Bean
    public DataSource dataSource() {

        // We reuse the same postgres container for all Integration tests
        EhrbasePostgreSQLContainer ehrdb = EhrbasePostgreSQLContainer.sharedInstance();

        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.type(SimpleDriverDataSource.class);
        dataSourceBuilder.driverClassName(ehrdb.getDriverClassName());
        dataSourceBuilder.url(ehrdb.getJdbcUrl());
        dataSourceBuilder.username(ehrdb.getUsername());
        dataSourceBuilder.password(ehrdb.getPassword());
        return dataSourceBuilder.build();
    }

    /**
     * The default implementation is RequestScoped - but we do not have a request during tests.
     */
    @Bean
    public TimeProvider timeProvider() {
        return OffsetDateTime::now;
    }
}
