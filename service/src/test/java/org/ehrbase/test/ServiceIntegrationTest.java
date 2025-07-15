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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.ehrbase.ServiceModuleConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ContextConfiguration(initializers = ServiceIntegrationTest.IntegrationTestInitializer.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {ServiceModuleConfiguration.class, ServiceTestConfiguration.class},
        properties = {
            "spring.main.banner-mode=off",
            "spring.main.log-startup-info=false",
            "spring.main.lazy-initialization=true",
        })
@ActiveProfiles("test")
public @interface ServiceIntegrationTest {

    class IntegrationTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        EhrbasePostgreSQLContainer ehrDb = EhrbasePostgreSQLContainer.sharedInstance();

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

            TestPropertyValues.of(
                            // configure connection to the running mssql test container
                            "spring.datasource.url=" + ehrDb.getJdbcUrl(),
                            "spring.datasource.driver-class-name=org.postgresql.Driver",
                            "spring.datasource.username=" + ehrDb.getUsername(),
                            "spring.datasource.password=" + ehrDb.getPassword())
                    .applyTo(configurableApplicationContext.getEnvironment());
        }
    }
}
