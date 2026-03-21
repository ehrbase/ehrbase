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
package org.ehrbase.service.graphql;

import graphql.schema.idl.RuntimeWiring;
import org.ehrbase.service.graphql.fetcher.GenericViewDataFetcher;
import org.ehrbase.service.graphql.scalars.DateTimeRangeScalar;
import org.ehrbase.service.graphql.scalars.DateTimeScalar;
import org.ehrbase.service.graphql.scalars.JsonScalar;
import org.ehrbase.service.graphql.scalars.LongScalar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;

/**
 * Configures GraphQL runtime wiring: custom scalars and dynamic template query data fetchers.
 *
 * <p>Mutations and subscriptions are handled by {@link org.ehrbase.service.graphql.controller.EhrGraphQlController}
 * using {@code @MutationMapping} and {@code @SubscriptionMapping} annotations — Spring for GraphQL 2.0.2
 * auto-detects these and wires them automatically.
 */
@Component
public class GraphQlRuntimeWiringConfigurer implements RuntimeWiringConfigurer {

    private static final Logger log = LoggerFactory.getLogger(GraphQlRuntimeWiringConfigurer.class);

    private final GenericViewDataFetcher genericViewDataFetcher;
    private final GraphQlSchemaRegistryService schemaRegistry;

    public GraphQlRuntimeWiringConfigurer(
            GenericViewDataFetcher genericViewDataFetcher, GraphQlSchemaRegistryService schemaRegistry) {
        this.genericViewDataFetcher = genericViewDataFetcher;
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        // Custom scalars
        builder.scalar(DateTimeScalar.INSTANCE);
        builder.scalar(DateTimeRangeScalar.INSTANCE);
        builder.scalar(JsonScalar.INSTANCE);
        builder.scalar(LongScalar.INSTANCE);

        // Dynamic template-derived query fields (backed by GenericViewDataFetcher)
        for (String queryField : schemaRegistry.getQueryFieldNames()) {
            builder.type("Query", wiring -> wiring.dataFetcher(queryField, genericViewDataFetcher));
        }

        log.info(
                "GraphQL runtime wiring: 4 scalars, {} dynamic query fields"
                        + " (mutations + subscriptions via @Controller annotations)",
                schemaRegistry.getQueryFieldNames().size());
    }
}
