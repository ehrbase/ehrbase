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
package org.ehrbase.service.graphql.security;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures query complexity scoring to reject expensive GraphQL queries.
 * Each field = 1 point. Rejects queries exceeding the configured maximum.
 */
@Configuration
public class QueryComplexityInstrumentation {

    @Bean
    public Instrumentation maxQueryComplexityInstrumentation(
            @Value("${ehrbase.graphql.max-query-complexity:100}") int maxComplexity) {
        return new MaxQueryComplexityInstrumentation(maxComplexity);
    }
}
