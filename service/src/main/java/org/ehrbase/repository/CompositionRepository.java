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
package org.ehrbase.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import com.nedap.archie.rm.composition.Composition;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.repository.composition.CompositionMetadata;
import org.ehrbase.repository.composition.DynamicCompositionReader;
import org.ehrbase.repository.composition.DynamicCompositionWriter;
import org.ehrbase.repository.schema.TemplateSchemaResolver;
import org.ehrbase.repository.schema.TemplateTableMetadata;
import org.ehrbase.repository.versioning.VersioningEngine;
import org.ehrbase.service.AuditEventService;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.TenantGuard;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for composition CRUD operations using normalized template tables.
 * Replaces the legacy JSONB-based CompositionRepository.
 *
 * <p>Uses {@link VersioningEngine} for explicit app-code versioning,
 * {@link DynamicCompositionWriter}/{@link DynamicCompositionReader} for clinical data,
 * and {@link TemplateSchemaResolver} for dynamic table resolution.
 */
@Repository
public class CompositionRepository {

    private static final Logger log = LoggerFactory.getLogger(CompositionRepository.class);

    private static final org.jooq.Table<?> COMPOSITION = table(name("ehr_system", "composition"));
    private static final org.jooq.Table<?> COMPOSITION_HISTORY = table(name("ehr_system", "composition_history"));

    private final DSLContext dsl;
    private final VersioningEngine versioningEngine;
    private final DynamicCompositionWriter writer;
    private final DynamicCompositionReader reader;
    private final TemplateSchemaResolver schemaResolver;
    private final KnowledgeCacheService knowledgeCache;
    private final AuditEventService auditService;
    private final TenantGuard tenantGuard;
    private final RequestContext requestContext;

    public CompositionRepository(
            DSLContext dsl,
            VersioningEngine versioningEngine,
            DynamicCompositionWriter writer,
            DynamicCompositionReader reader,
            TemplateSchemaResolver schemaResolver,
            KnowledgeCacheService knowledgeCache,
            AuditEventService auditService,
            TenantGuard tenantGuard,
            RequestContext requestContext) {
        this.dsl = dsl;
        this.versioningEngine = versioningEngine;
        this.writer = writer;
        this.reader = reader;
        this.schemaResolver = schemaResolver;
        this.knowledgeCache = knowledgeCache;
        this.auditService = auditService;
        this.tenantGuard = tenantGuard;
        this.requestContext = requestContext;
    }

    @Transactional
    public CompositionMetadata create(
            UUID ehrId,
            Composition composition,
            UUID templateUuid,
            String templateId,
            UUID contributionId,
            WebTemplate webTemplate) {

        TemplateTableMetadata tableMeta = schemaResolver.resolve(templateId);

        CompositionMetadata result = versioningEngine.createComposition(
                ehrId,
                composition,
                templateUuid,
                templateId,
                contributionId,
                requestContext.getUserId(),
                requestContext.getUserId(),
                requestContext.getTenantId(),
                webTemplate,
                tableMeta);

        auditService.recordEvent("data_modify", "composition", result.id(), "create", null, null);
        return result;
    }

    public Optional<Composition> findHead(UUID ehrId, UUID compositionId) {
        CompositionMetadata meta = fetchCompositionMetadata(compositionId);
        if (meta == null) {
            return Optional.empty();
        }
        tenantGuard.assertTenantMatch(meta.sysTenant());

        TemplateTableMetadata tableMeta = schemaResolver.resolveByUuid(meta.templateId());
        WebTemplate webTemplate = resolveWebTemplate(meta.templateId());
        auditService.recordEvent("data_access", "composition", compositionId, "read", null, null);
        return reader.readCurrent(compositionId, tableMeta, webTemplate, meta);
    }

    public Optional<Composition> findByVersion(UUID ehrId, UUID compositionId, int version) {
        CompositionMetadata meta = fetchCompositionMetadataForVersion(compositionId, version);
        if (meta == null) {
            return Optional.empty();
        }
        tenantGuard.assertTenantMatch(meta.sysTenant());

        TemplateTableMetadata tableMeta = schemaResolver.resolveByUuid(meta.templateId());
        WebTemplate webTemplate = resolveWebTemplate(meta.templateId());
        auditService.recordEvent("data_access", "composition", compositionId, "read", null, null);
        return reader.readVersion(compositionId, version, tableMeta, webTemplate, meta);
    }

    public Optional<Composition> findAtTime(UUID ehrId, UUID compositionId, OffsetDateTime timestamp) {
        CompositionMetadata meta = fetchCompositionMetadataAtTime(compositionId, timestamp);
        if (meta == null) {
            return Optional.empty();
        }
        tenantGuard.assertTenantMatch(meta.sysTenant());

        TemplateTableMetadata tableMeta = schemaResolver.resolveByUuid(meta.templateId());
        WebTemplate webTemplate = resolveWebTemplate(meta.templateId());
        auditService.recordEvent("data_access", "composition", compositionId, "read", null, null);
        return reader.readAtTime(compositionId, timestamp, tableMeta, webTemplate, meta);
    }

    @Transactional
    public CompositionMetadata update(
            UUID ehrId,
            UUID compositionId,
            Composition composition,
            UUID templateUuid,
            String templateId,
            UUID contributionId,
            int expectedVersion,
            WebTemplate webTemplate) {

        TemplateTableMetadata tableMeta = schemaResolver.resolve(templateId);

        CompositionMetadata result = versioningEngine.updateComposition(
                compositionId,
                ehrId,
                expectedVersion,
                composition,
                templateUuid,
                templateId,
                contributionId,
                requestContext.getUserId(),
                requestContext.getUserId(),
                requestContext.getTenantId(),
                webTemplate,
                tableMeta);

        auditService.recordEvent("data_modify", "composition", compositionId, "update", null, null);
        return result;
    }

    @Transactional
    public void delete(UUID ehrId, UUID compositionId, int expectedVersion, UUID contributionId) {
        CompositionMetadata meta = fetchCompositionMetadata(compositionId);
        if (meta == null) {
            throw new org.ehrbase.api.exception.ObjectNotFoundException("composition", compositionId.toString());
        }
        tenantGuard.assertTenantMatch(meta.sysTenant());

        TemplateTableMetadata tableMeta = schemaResolver.resolveByUuid(meta.templateId());

        versioningEngine.deleteComposition(
                compositionId,
                ehrId,
                expectedVersion,
                contributionId,
                requestContext.getUserId(),
                requestContext.getUserId(),
                requestContext.getTenantId(),
                tableMeta);

        auditService.recordEvent("data_modify", "composition", compositionId, "delete", null, null);
    }

    public boolean exists(UUID compositionId) {
        return dsl.fetchExists(dsl.selectOne()
                .from(COMPOSITION)
                .where(field(name("id"), UUID.class).eq(compositionId)));
    }

    public Optional<Integer> getLatestVersionNumber(UUID compositionId) {
        Record1<Integer> result = dsl.select(field(name("sys_version"), Integer.class))
                .from(COMPOSITION)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .fetchOne();

        if (result != null) {
            return Optional.of(result.value1());
        }

        // Check history for deleted compositions
        result = dsl.select(field(name("sys_version"), Integer.class))
                .from(COMPOSITION_HISTORY)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .orderBy(field(name("sys_version")).desc())
                .limit(1)
                .fetchOne();

        return result != null ? Optional.of(result.value1()) : Optional.empty();
    }

    public boolean isDeleted(UUID ehrId, UUID compositionId, Integer version) {
        // A composition is deleted if it's not in the current table
        // but exists in history with change_type='deleted'
        boolean inCurrent = exists(compositionId);
        if (inCurrent) {
            return false;
        }

        return dsl.fetchExists(dsl.selectOne()
                .from(COMPOSITION_HISTORY)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .and(field(name("change_type"), String.class).eq("deleted")));
    }

    public boolean isTemplateUsed(String templateId) {
        return dsl.fetchExists(dsl.selectOne()
                .from(COMPOSITION)
                .join(table(name("ehr_system", "template")))
                .on(field(name("ehr_system", "composition", "template_id"), UUID.class)
                        .eq(field(name("ehr_system", "template", "id"), UUID.class)))
                .where(field(name("ehr_system", "template", "template_id"), String.class)
                        .eq(templateId)));
    }

    public String retrieveTemplateIdForComposition(UUID compositionId) {
        Record1<String> result = dsl.select(field(name("t", "template_id"), String.class))
                .from(COMPOSITION.as("c"))
                .join(table(name("ehr_system", "template")).as("t"))
                .on(field(name("c", "template_id"), UUID.class).eq(field(name("t", "id"), UUID.class)))
                .where(field(name("c", "id"), UUID.class).eq(compositionId))
                .fetchOne();
        if (result == null) {
            throw new org.ehrbase.api.exception.ObjectNotFoundException("composition", compositionId.toString());
        }
        return result.value1();
    }

    public int getVersionByTimestamp(UUID compositionId, OffsetDateTime timestamp) {
        org.jooq.Record row = dsl.resultQuery(
                        "SELECT sys_version FROM ehr_system.composition WHERE id = ? AND valid_period @> ?::timestamptz"
                                + " UNION ALL"
                                + " SELECT sys_version FROM ehr_system.composition_history WHERE id = ? AND valid_period @> ?::timestamptz"
                                + " LIMIT 1",
                        compositionId,
                        timestamp,
                        compositionId,
                        timestamp)
                .fetchOne();
        if (row == null) {
            throw new org.ehrbase.api.exception.ObjectNotFoundException(
                    "composition",
                    "No version found at timestamp %s for composition %s".formatted(timestamp, compositionId));
        }
        return row.get(field(name("sys_version"), Integer.class));
    }

    public Optional<UUID> getEhrIdForComposition(UUID compositionId) {
        Record1<UUID> result = dsl.select(field(name("ehr_id"), UUID.class))
                .from(COMPOSITION)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .fetchOne();
        return result != null ? Optional.of(result.value1()) : Optional.empty();
    }

    private CompositionMetadata fetchCompositionMetadata(UUID compositionId) {
        Record row = dsl.select()
                .from(COMPOSITION)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .fetchOne();
        return row != null ? mapToCompositionMetadata(row) : null;
    }

    private CompositionMetadata fetchCompositionMetadataForVersion(UUID compositionId, int version) {
        // Check current first
        Record row = dsl.select()
                .from(COMPOSITION)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .and(field(name("sys_version"), Integer.class).eq(version))
                .fetchOne();
        if (row != null) {
            return mapToCompositionMetadata(row);
        }
        // Check history
        row = dsl.select()
                .from(COMPOSITION_HISTORY)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .and(field(name("sys_version"), Integer.class).eq(version))
                .fetchOne();
        return row != null ? mapToCompositionMetadata(row) : null;
    }

    private CompositionMetadata fetchCompositionMetadataAtTime(UUID compositionId, OffsetDateTime timestamp) {
        Record row = dsl.resultQuery(
                        "SELECT * FROM ehr_system.composition WHERE id = ? AND valid_period @> ?::timestamptz"
                                + " UNION ALL"
                                + " SELECT * FROM ehr_system.composition_history WHERE id = ? AND valid_period @> ?::timestamptz"
                                + " LIMIT 1",
                        compositionId,
                        timestamp,
                        compositionId,
                        timestamp)
                .fetchOne();
        return row != null ? mapToCompositionMetadata(row) : null;
    }

    private WebTemplate resolveWebTemplate(UUID templateUuid) {
        Record1<String> templateIdRow = dsl.select(field(name("template_id"), String.class))
                .from(table(name("ehr_system", "template")))
                .where(field(name("id"), UUID.class).eq(templateUuid))
                .fetchOne();
        if (templateIdRow == null) {
            return null;
        }
        return knowledgeCache.getInternalTemplate(templateIdRow.value1());
    }

    private CompositionMetadata mapToCompositionMetadata(Record row) {
        return new CompositionMetadata(
                row.get(field(name("id"), UUID.class)),
                row.get(field(name("ehr_id"), UUID.class)),
                row.get(field(name("template_id"), UUID.class)),
                row.get(field(name("archetype_id"), String.class)),
                row.get(field(name("template_name"), String.class)),
                row.get(field(name("composer_name"), String.class)),
                row.get(field(name("composer_id"), String.class)),
                row.get(field(name("language"), String.class)),
                row.get(field(name("territory"), String.class)),
                row.get(field(name("category_code"), String.class)),
                null, // feederAudit
                null, // participations
                row.get(field(name("sys_version"), Integer.class)),
                row.get(field(name("contribution_id"), UUID.class)),
                row.get(field(name("change_type"), String.class)),
                row.get(field(name("committed_at"), OffsetDateTime.class)),
                row.get(field(name("committer_name"), String.class)),
                row.get(field(name("committer_id"), String.class)),
                row.get(field(name("sys_tenant"), Short.class)));
    }
}
