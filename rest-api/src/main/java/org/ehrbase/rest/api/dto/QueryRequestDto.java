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
package org.ehrbase.rest.api.dto;

import java.util.Map;

/**
 * Request DTO for SQL query execution against {@code ehr_views} schema.
 *
 * @param sql     the SQL query (SELECT only, parameterized)
 * @param params  named parameters for the query
 * @param timeout query timeout in seconds (optional, default from config)
 */
public record QueryRequestDto(String sql, Map<String, Object> params, Integer timeout) {}
