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

import graphql.ExecutionResult;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.language.Field;
import org.ehrbase.service.graphql.GraphQlSchemaRegistryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static graphql.execution.instrumentation.SimpleInstrumentationContext.noOp;

/**
 * GraphQL configuration for EHRbase.
 *
 * <p>Configures:
 * <ul>
 *   <li>Query depth limiting via {@link MaxQueryDepthInstrumentation} (introspection-aware)</li>
 *   <li>Dynamic template schema registration via {@link GraphQlSchemaRegistryService}</li>
 * </ul>
 *
 * <p>Connection types (edges, pageInfo, cursors) are handled entirely by Spring for GraphQL's
 * auto-configured {@code ConnectionFieldTypeVisitor} and {@code WindowConnectionAdapter}.
 */
@Configuration
public class GraphQlConfiguration {

    @Bean
    public Instrumentation maxQueryDepthInstrumentation(@Value("${ehrbase.graphql.max-query-depth:10}") int maxDepth) {
        return new MaxQueryDepthInstrumentation(maxDepth) {
            @Override
            public InstrumentationContext<ExecutionResult> beginExecuteOperation(
                    InstrumentationExecuteOperationParameters parameters, InstrumentationState state) {
                // Exempt introspection queries (__schema, __type) from depth limiting.
                // Standard introspection requires depth ~14 due to nested TypeRef fragments,
                // and is essential for GraphiQL, IDE plugins, and codegen tooling.
                boolean isIntrospection = parameters.getExecutionContext()
                        .getOperationDefinition()
                        .getSelectionSet()
                        .getSelections()
                        .stream()
                        .filter(Field.class::isInstance)
                        .map(sel -> ((Field) sel).getName())
                        .anyMatch(name -> name.startsWith("__"));
                if (isIntrospection) {
                    return noOp();
                }
                return super.beginExecuteOperation(parameters, state);
            }
        };
    }

    @Bean
    public GraphQlSourceBuilderCustomizer dynamicSchemaCustomizer(GraphQlSchemaRegistryService schemaRegistry) {
        return builder -> builder.schemaResources(schemaRegistry.getGeneratedSchemaResource());
    }
}
