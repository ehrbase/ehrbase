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
package org.ehrbase.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test-only Spring Boot application that enables auto-configuration for integration tests.
 * This provides CacheManager auto-configuration, DataSource auto-configuration, etc.
 * that the production {@code ServiceModuleConfiguration} does not enable (it uses plain
 * {@code @Configuration} + {@code @ComponentScan}).
 *
 * <p>Spring Boot's test framework auto-detects this class as the application entry point
 * for all integration tests in the service module.
 */
@SpringBootApplication(scanBasePackages = {"org.ehrbase.service", "org.ehrbase.cache", "org.ehrbase.repository"})
public class TestApplication {}
