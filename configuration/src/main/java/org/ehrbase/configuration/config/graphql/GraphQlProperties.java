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
package org.ehrbase.configuration.config.graphql;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for EHRbase GraphQL layer.
 *
 * @param statementTimeout  PostgreSQL statement timeout per query (default: 10s)
 * @param maxQueryDepth     maximum query nesting depth (default: 10)
 * @param maxQueryComplexity maximum query complexity score (default: 100)
 */
@ConfigurationProperties(prefix = "ehrbase.graphql")
public record GraphQlProperties(String statementTimeout, int maxQueryDepth, int maxQueryComplexity) {

    public GraphQlProperties {
        if (statementTimeout == null) statementTimeout = "10s";
        if (maxQueryDepth <= 0) maxQueryDepth = 10;
        if (maxQueryComplexity <= 0) maxQueryComplexity = 100;
    }
}
