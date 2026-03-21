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
import org.ehrbase.service.graphql.fetcher.AuditEventSubscriptionFetcher;
import org.ehrbase.service.graphql.fetcher.CompositionSubscriptionFetcher;
import org.ehrbase.service.graphql.fetcher.CreateCompositionFetcher;
import org.ehrbase.service.graphql.fetcher.CreateEhrFetcher;
import org.ehrbase.service.graphql.fetcher.DeleteCompositionFetcher;
import org.ehrbase.service.graphql.fetcher.GenericViewDataFetcher;
import org.ehrbase.service.graphql.fetcher.UpdateCompositionFetcher;
import org.ehrbase.service.graphql.scalars.DateTimeRangeScalar;
import org.ehrbase.service.graphql.scalars.DateTimeScalar;
import org.ehrbase.service.graphql.scalars.JsonScalar;
import org.ehrbase.service.graphql.scalars.LongScalar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;

/**
 * Configures GraphQL runtime wiring: custom scalars, query data fetchers, mutations, and subscriptions.
 */
@Component
public class GraphQlRuntimeWiringConfigurer implements RuntimeWiringConfigurer {

    private static final Logger log = LoggerFactory.getLogger(GraphQlRuntimeWiringConfigurer.class);

    private final GenericViewDataFetcher genericViewDataFetcher;
    private final GraphQlSchemaRegistryService schemaRegistry;
    private final CreateEhrFetcher createEhrFetcher;
    private final CreateCompositionFetcher createCompositionFetcher;
    private final UpdateCompositionFetcher updateCompositionFetcher;
    private final DeleteCompositionFetcher deleteCompositionFetcher;
    private final CompositionSubscriptionFetcher compositionSubscriptionFetcher;
    private final AuditEventSubscriptionFetcher auditEventSubscriptionFetcher;

    public GraphQlRuntimeWiringConfigurer(
            GenericViewDataFetcher genericViewDataFetcher,
            GraphQlSchemaRegistryService schemaRegistry,
            CreateEhrFetcher createEhrFetcher,
            CreateCompositionFetcher createCompositionFetcher,
            UpdateCompositionFetcher updateCompositionFetcher,
            DeleteCompositionFetcher deleteCompositionFetcher,
            CompositionSubscriptionFetcher compositionSubscriptionFetcher,
            AuditEventSubscriptionFetcher auditEventSubscriptionFetcher) {
        this.genericViewDataFetcher = genericViewDataFetcher;
        this.schemaRegistry = schemaRegistry;
        this.createEhrFetcher = createEhrFetcher;
        this.createCompositionFetcher = createCompositionFetcher;
        this.updateCompositionFetcher = updateCompositionFetcher;
        this.deleteCompositionFetcher = deleteCompositionFetcher;
        this.compositionSubscriptionFetcher = compositionSubscriptionFetcher;
        this.auditEventSubscriptionFetcher = auditEventSubscriptionFetcher;
    }

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        // Custom scalars
        builder.scalar(DateTimeScalar.INSTANCE);
        builder.scalar(DateTimeRangeScalar.INSTANCE);
        builder.scalar(JsonScalar.INSTANCE);
        builder.scalar(LongScalar.INSTANCE);

        // Template-derived query fields
        for (String queryField : schemaRegistry.getQueryFieldNames()) {
            builder.type("Query", wiring -> wiring.dataFetcher(queryField, genericViewDataFetcher));
        }

        // Mutations
        builder.type(
                "Mutation",
                wiring -> wiring.dataFetcher("createEhr", createEhrFetcher)
                        .dataFetcher("createComposition", createCompositionFetcher)
                        .dataFetcher("updateComposition", updateCompositionFetcher)
                        .dataFetcher("deleteComposition", deleteCompositionFetcher));

        // Subscriptions
        builder.type(
                "Subscription",
                wiring -> wiring.dataFetcher("onCompositionChange", compositionSubscriptionFetcher)
                        .dataFetcher("onAuditEvent", auditEventSubscriptionFetcher));

        log.info(
                "GraphQL runtime wiring: 4 scalars, {} queries, 4 mutations, 2 subscriptions",
                schemaRegistry.getQueryFieldNames().size());
    }
}
