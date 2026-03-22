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

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.ehrbase.api.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Manages the lifecycle of the dynamic GraphQL schema.
 * Generates schema at startup and provides hot-reload when templates change.
 *
 * <p>Feeds the generated SDL to Spring for GraphQL's {@code GraphQlSource}.
 */
@Service
@org.springframework.context.annotation.DependsOn("flyway")
public class GraphQlSchemaRegistryService {

    private static final Logger log = LoggerFactory.getLogger(GraphQlSchemaRegistryService.class);

    private final GraphQlSchemaGeneratorService schemaGenerator;
    private final AtomicReference<String> currentSdl = new AtomicReference<>("");
    private final AtomicReference<List<String>> currentQueryFields = new AtomicReference<>(List.of());

    public GraphQlSchemaRegistryService(GraphQlSchemaGeneratorService schemaGenerator) {
        this.schemaGenerator = schemaGenerator;
    }

    @PostConstruct
    void init() {
        try {
            regenerate();
        } catch (Exception e) {
            log.warn(
                    "GraphQL schema generation skipped at startup (database may not be fully migrated yet): {}",
                    e.getMessage());
        }
    }

    /**
     * Regenerates the GraphQL schema from the current view catalog.
     * Called on startup and when a template is uploaded or deleted.
     */
    public void regenerate() {
        short tenantId = TenantContext.getTenantId();
        String sdl = schemaGenerator.generateSchema(tenantId);
        currentSdl.set(sdl);
        currentQueryFields.set(schemaGenerator.getQueryFieldNames(tenantId));
        log.info(
                "GraphQL schema regenerated: {} query fields",
                currentQueryFields.get().size());
    }

    /**
     * Returns the generated SDL as a Spring Resource for GraphQlSource.
     */
    public Resource getGeneratedSchemaResource() {
        String sdl = currentSdl.get();
        if (sdl.isEmpty()) {
            return new ByteArrayResource("# No template views registered yet\n".getBytes(StandardCharsets.UTF_8));
        }
        return new ByteArrayResource(sdl.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns all dynamically registered query field names.
     */
    public List<String> getQueryFieldNames() {
        return currentQueryFields.get();
    }

    /**
     * Maps a GraphQL query field name back to its view name.
     */
    public String resolveViewName(String queryFieldName) {
        return schemaGenerator.queryFieldToViewName(queryFieldName);
    }
}
