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
package org.ehrbase.service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.UUID;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.repository.schema.TemplateSchemaResolver;
import org.ehrbase.repository.schema.TemplateTableMetadata;
import org.ehrbase.schemagen.SchemaGenerator;
import org.ehrbase.schemagen.TemplateAnalyzer;
import org.ehrbase.schemagen.model.GeneratedSchema;
import org.ehrbase.schemagen.model.TableDescriptor;
import org.ehrbase.service.graphql.GraphQlSchemaRegistryService;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes the full schema generation pipeline when a template is uploaded:
 * <ol>
 *   <li>Get WebTemplate from KnowledgeCacheService (already parsed on upload)</li>
 *   <li>Analyze WebTemplate → TableDescriptor</li>
 *   <li>Generate DDL (tables + views + search indexes)</li>
 *   <li>Execute DDL against PostgreSQL</li>
 *   <li>Register in schema_registry</li>
 *   <li>Register views in view_catalog</li>
 *   <li>Regenerate GraphQL schema (hot-reload)</li>
 * </ol>
 */
@Service
public class SchemaExecutorService {

    private static final Logger log = LoggerFactory.getLogger(SchemaExecutorService.class);

    private final DSLContext dsl;
    private final KnowledgeCacheService knowledgeCache;
    private final TemplateAnalyzer templateAnalyzer = new TemplateAnalyzer();
    private final SchemaGenerator schemaGenerator = new SchemaGenerator();
    private final TemplateSchemaResolver schemaResolver;
    private final ViewCatalogService viewCatalogService;
    private final GraphQlSchemaRegistryService graphQlSchemaRegistry;

    public SchemaExecutorService(
            DSLContext dsl,
            KnowledgeCacheService knowledgeCache,
            TemplateSchemaResolver schemaResolver,
            ViewCatalogService viewCatalogService,
            GraphQlSchemaRegistryService graphQlSchemaRegistry) {
        this.dsl = dsl;
        this.knowledgeCache = knowledgeCache;
        this.schemaResolver = schemaResolver;
        this.viewCatalogService = viewCatalogService;
        this.graphQlSchemaRegistry = graphQlSchemaRegistry;
    }

    /**
     * Runs the full pipeline for a template that was already stored in the knowledge cache.
     *
     * @param templateId   the template ID string
     * @param templateUuid the template UUID in ehr_system.template
     * @param tenantId     current tenant
     * @return the generated table name in ehr_data
     */
    @Transactional
    public String executeSchemaGeneration(String templateId, UUID templateUuid, short tenantId) {

        // 1. Get WebTemplate (already cached by KnowledgeCacheService on upload)
        WebTemplate webTemplate = knowledgeCache.getInternalTemplate(templateId);

        // 2. Analyze → TableDescriptor
        TableDescriptor tableDescriptor = templateAnalyzer.analyze(webTemplate);

        // 3. Generate DDL
        GeneratedSchema schema = schemaGenerator.generate(tableDescriptor);

        // 4. Execute table DDL (ehr_data schema)
        dsl.execute(schema.ddl());
        log.info("Created tables for template '{}': {}", templateId, tableDescriptor.getFullyQualifiedName());

        // 5. Execute view DDL (ehr_views schema)
        if (!schema.viewDdl().isEmpty()) {
            dsl.execute(schema.viewDdl());
            log.info("Created views for template '{}'", templateId);
        }

        // 6. Execute search DDL (tsvector + GIN indexes)
        if (!schema.searchDdl().isEmpty()) {
            dsl.execute(schema.searchDdl());
            log.info("Created search indexes for template '{}'", templateId);
        }

        // 7. Register in schema_registry
        dsl.insertInto(table(name("ehr_system", "schema_registry")))
                .set(field(name("template_id"), UUID.class), templateUuid)
                .set(field(name("table_name"), String.class), tableDescriptor.getTableName())
                .set(field(name("schema_name"), String.class), tableDescriptor.getSchema())
                .set(
                        field(name("ddl_hash"), String.class),
                        Integer.toHexString(schema.ddl().hashCode()))
                .set(field(name("status"), String.class), "active")
                .set(field(name("sys_tenant"), Short.class), tenantId)
                .onConflict(field(name("table_name")), field(name("sys_tenant")))
                .doNothing()
                .execute();

        // 8. Cache table metadata via fast path
        TemplateTableMetadata metadata = schemaResolver.fromTableDescriptor(tableDescriptor);

        // 9. Register views in view_catalog
        viewCatalogService.registerTemplateViews(
                tableDescriptor.getTableName(), templateId, metadata.columns(), tenantId);

        // 10. Regenerate GraphQL schema (hot-reload)
        graphQlSchemaRegistry.regenerate();

        log.info(
                "Schema generation pipeline complete: template '{}' → table '{}', views + GraphQL refreshed",
                templateId,
                tableDescriptor.getTableName());

        return tableDescriptor.getTableName();
    }
}
