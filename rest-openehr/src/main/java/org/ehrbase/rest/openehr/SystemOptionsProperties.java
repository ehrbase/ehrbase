/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.rest.openehr;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable values of the openEHR System API options and conformance statement.
 *
 * @param solution name of the solution
 * @param vendor   name of the vendor
 */
@ConfigurationProperties(prefix = "ehrbase.rest.system")
public record SystemOptionsProperties(String solution, String vendor) {}
