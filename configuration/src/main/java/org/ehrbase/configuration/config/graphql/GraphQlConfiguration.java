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

import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import org.ehrbase.service.graphql.GraphQlSchemaRegistryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * GraphQL configuration for EHRbase.
 *
 * <p>Configures:
 * <ul>
 *   <li>Query depth limiting via {@link MaxQueryDepthInstrumentation}</li>
 *   <li>Dynamic template schema registration via {@link GraphQlSchemaRegistryService}</li>
 * </ul>
 *
 * <p>Connection types (edges, pageInfo, cursors) are handled entirely by Spring for GraphQL's
 * auto-configured {@code ConnectionFieldTypeVisitor} and {@code WindowConnectionAdapter}.
 * The {@link org.ehrbase.service.graphql.fetcher.GenericViewDataFetcher} returns
 * {@link org.springframework.data.domain.Window} objects that the framework converts
 * to Relay-style connections automatically.
 */
@Configuration
public class GraphQlConfiguration {

    @Bean
    public Instrumentation maxQueryDepthInstrumentation(@Value("${ehrbase.graphql.max-query-depth:10}") int maxDepth) {
        return new MaxQueryDepthInstrumentation(maxDepth);
    }

    @Bean
    public GraphQlSourceBuilderCustomizer dynamicSchemaCustomizer(GraphQlSchemaRegistryService schemaRegistry) {
        return builder -> builder.schemaResources(schemaRegistry.getGeneratedSchemaResource());
    }
}
