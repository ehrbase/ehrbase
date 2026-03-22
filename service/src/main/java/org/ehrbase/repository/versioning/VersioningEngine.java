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
package org.ehrbase.repository.versioning;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import com.nedap.archie.rm.composition.Composition;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.repository.composition.CompositionMetadata;
import org.ehrbase.repository.composition.DynamicCompositionWriter;
import org.ehrbase.repository.schema.TemplateTableMetadata;
import org.ehrbase.service.TimeProvider;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explicit app-code versioning engine for compositions and EHR status.
 * NO database triggers. PG18 {@code WITHOUT OVERLAPS} is a safety net only.
 *
 * <p>All versioning methods are {@link Transactional}, which means PostgreSQL's {@code now()}
 * returns the same transaction-scoped timestamp for every statement within a single
 * create/update/delete operation. This guarantees temporal continuity in {@code valid_period}
 * ranges without passing timestamps between Java and SQL.
 *
 * <p>Handles the full versioning lifecycle:
 * <ul>
 *   <li>CREATE: INSERT new version (sys_version=1, open-ended valid_period)</li>
 *   <li>UPDATE: Archive old to _history, INSERT new version (sys_version+1)</li>
 *   <li>DELETE: Archive to _history, remove from current table</li>
 * </ul>
 */
@Component
public class VersioningEngine {

    private static final Logger log = LoggerFactory.getLogger(VersioningEngine.class);

    private static final org.jooq.Table<?> COMPOSITION = table(name("ehr_system", "composition"));

    /**
     * Columns in ehr_system.composition / composition_history (explicit list for INSERT ... SELECT).
     * Using explicit columns avoids issues with column default mismatches and ensures the
     * archive INSERT can transform valid_period inline.
     */
    private static final String COMPOSITION_COLUMNS =
            "id, ehr_id, template_id, valid_period, archetype_id, template_name, "
                    + "composer_name, composer_id, language, territory, category_code, feeder_audit, "
                    + "participations, sys_version, contribution_id, change_type, committed_at, "
                    + "committer_name, committer_id, sys_tenant";

    private final DSLContext dsl;
    private final DynamicCompositionWriter writer;
    private final TimeProvider timeProvider;

    public VersioningEngine(DSLContext dsl, DynamicCompositionWriter writer, TimeProvider timeProvider) {
        this.dsl = dsl;
        this.writer = writer;
        this.timeProvider = timeProvider;
    }

    /**
     * CREATE a new composition version (sys_version=1).
     * The column default {@code tstzrange(now(), NULL)} sets valid_period automatically.
     */
    @Transactional
    public CompositionMetadata createComposition(
            UUID ehrId,
            Composition composition,
            UUID templateUuid,
            String templateId,
            UUID contributionId,
            String committerName,
            String committerId,
            short tenantId,
            WebTemplate webTemplate,
            TemplateTableMetadata tableMeta) {

        OffsetDateTime now = timeProvider.getNow();
        String archetypeId = composition.getArchetypeNodeId();
        String language =
                composition.getLanguage() != null ? composition.getLanguage().getCodeString() : null;
        String territory =
                composition.getTerritory() != null ? composition.getTerritory().getCodeString() : null;

        Record1<UUID> result = dsl.insertInto(COMPOSITION)
                .set(field(name("ehr_id"), UUID.class), ehrId)
                .set(field(name("template_id"), UUID.class), templateUuid)
                .set(field(name("archetype_id"), String.class), archetypeId)
                .set(field(name("template_name"), String.class), templateId)
                .set(field(name("composer_name"), String.class), committerName)
                .set(field(name("composer_id"), String.class), committerId)
                .set(field(name("language"), String.class), language)
                .set(field(name("territory"), String.class), territory)
                .set(field(name("sys_version"), Integer.class), 1)
                .set(field(name("contribution_id"), UUID.class), contributionId)
                .set(field(name("change_type"), String.class), "creation")
                .set(field(name("committed_at"), OffsetDateTime.class), now)
                .set(field(name("committer_name"), String.class), committerName)
                .set(field(name("committer_id"), String.class), committerId)
                .set(field(name("sys_tenant"), Short.class), tenantId)
                .returningResult(field(name("id"), UUID.class))
                .fetchOne();

        UUID compositionId = result != null ? result.value1() : null;
        if (compositionId == null) {
            throw new IllegalStateException("Failed to create composition: no ID returned");
        }

        writer.write(compositionId, ehrId, tenantId, composition, webTemplate, tableMeta);

        log.debug("Created composition: id={} ehr={} template={} version=1", compositionId, ehrId, templateId);

        return new CompositionMetadata(
                compositionId,
                ehrId,
                templateUuid,
                archetypeId,
                templateId,
                committerName,
                committerId,
                language,
                territory,
                null,
                null,
                null,
                1,
                contributionId,
                "creation",
                now,
                committerName,
                committerId,
                tenantId);
    }

    /**
     * UPDATE a composition: archive old version to _history, insert new version.
     *
     * <p>All SQL uses {@code now()} for timestamps. Since this method is {@link Transactional},
     * {@code now()} returns the same value for every statement, guaranteeing that the history
     * row's upper bound equals the new row's lower bound with zero gaps or overlaps.
     */
    @Transactional
    public CompositionMetadata updateComposition(
            UUID compositionId,
            UUID ehrId,
            int expectedVersion,
            Composition newComposition,
            UUID templateUuid,
            String templateId,
            UUID contributionId,
            String committerName,
            String committerId,
            short tenantId,
            WebTemplate webTemplate,
            TemplateTableMetadata tableMeta) {

        // Optimistic locking: verify current version matches expected
        Record currentRow = dsl.select()
                .from(COMPOSITION)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .and(field(name("sys_version"), Integer.class).eq(expectedVersion))
                .fetchOne();

        if (currentRow == null) {
            throw new PreconditionFailedException(
                    "Composition version mismatch: expected %d".formatted(expectedVersion));
        }

        int newVersion = expectedVersion + 1;

        // Step 1: Archive current row to _history with closed valid_period
        archiveCompositionToHistory(compositionId);

        // Step 2: Archive clinical data BEFORE deleting composition (FK constraint)
        writer.archiveByCompositionId(compositionId, tableMeta);

        // Step 3: Delete old row from current table
        dsl.deleteFrom(COMPOSITION)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .execute();

        // Step 4: INSERT new version — valid_period starts at now() (same transaction timestamp)
        String archetypeId = newComposition.getArchetypeNodeId();
        String language = newComposition.getLanguage() != null
                ? newComposition.getLanguage().getCodeString()
                : null;
        String territory = newComposition.getTerritory() != null
                ? newComposition.getTerritory().getCodeString()
                : null;

        dsl.execute(
                "INSERT INTO ehr_system.composition "
                        + "(" + COMPOSITION_COLUMNS + ") "
                        + "VALUES (?, ?, ?, tstzrange(now(), NULL), ?, ?, ?, ?, ?, ?, NULL, NULL, '[]'::jsonb, ?, ?, ?, now(), ?, ?, ?)",
                compositionId,
                ehrId,
                templateUuid,
                archetypeId,
                templateId,
                committerName,
                committerId,
                language,
                territory,
                newVersion,
                contributionId,
                "modification",
                committerName,
                committerId,
                tenantId);

        // Step 5: Insert new clinical data with correct sys_version
        writer.write(compositionId, ehrId, tenantId, newVersion, newComposition, webTemplate, tableMeta);

        log.debug("Updated composition: id={} version={}->{}", compositionId, expectedVersion, newVersion);

        OffsetDateTime now = timeProvider.getNow();
        return new CompositionMetadata(
                compositionId,
                ehrId,
                templateUuid,
                archetypeId,
                templateId,
                committerName,
                committerId,
                language,
                territory,
                null,
                null,
                null,
                newVersion,
                contributionId,
                "modification",
                now,
                committerName,
                committerId,
                tenantId);
    }

    /**
     * DELETE a composition: archive to _history, remove from current.
     */
    @Transactional
    public void deleteComposition(
            UUID compositionId,
            UUID ehrId,
            int expectedVersion,
            UUID contributionId,
            String committerName,
            String committerId,
            short tenantId,
            TemplateTableMetadata tableMeta) {

        // Optimistic locking
        Record currentRow = dsl.select()
                .from(COMPOSITION)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .and(field(name("sys_version"), Integer.class).eq(expectedVersion))
                .fetchOne();

        if (currentRow == null) {
            throw new PreconditionFailedException(
                    "Composition version mismatch: expected %d".formatted(expectedVersion));
        }

        // Step 1: Archive current to _history with closed period
        archiveCompositionToHistory(compositionId);

        // Step 2: INSERT deletion marker into _history.
        // Uses tstzrange(now(), NULL) — an open-ended range starting at deletion time.
        int deletionVersion = expectedVersion + 1;
        dsl.execute(
                "INSERT INTO ehr_system.composition_history "
                        + "(id, ehr_id, template_id, archetype_id, template_name, "
                        + "composer_name, composer_id, valid_period, sys_version, contribution_id, change_type, "
                        + "committed_at, committer_name, committer_id, sys_tenant) "
                        + "SELECT id, ehr_id, template_id, archetype_id, template_name, composer_name, composer_id, "
                        + "tstzrange(now(), NULL), ?, ?, 'deleted', now(), ?, ?, sys_tenant "
                        + "FROM ehr_system.composition WHERE id = ?",
                deletionVersion,
                contributionId,
                committerName,
                committerId,
                compositionId);

        // Step 3: Archive + delete clinical data BEFORE deleting composition (FK constraint)
        writer.archiveByCompositionId(compositionId, tableMeta);

        // Step 4: Delete from current table
        dsl.deleteFrom(COMPOSITION)
                .where(field(name("id"), UUID.class).eq(compositionId))
                .execute();

        log.debug("Deleted composition: id={} version={}", compositionId, deletionVersion);
    }

    /**
     * Archives the current composition row to composition_history with a closed valid_period.
     * Uses {@code now()} (transaction-scoped) to close the period, ensuring temporal continuity
     * with any subsequent INSERT that also uses {@code now()} as the start of its valid_period.
     */
    private void archiveCompositionToHistory(UUID compositionId) {
        dsl.execute(
                "INSERT INTO ehr_system.composition_history (" + COMPOSITION_COLUMNS + ") "
                        + "SELECT id, ehr_id, template_id, tstzrange(lower(valid_period), now()), "
                        + "archetype_id, template_name, composer_name, composer_id, language, territory, "
                        + "category_code, feeder_audit, participations, sys_version, contribution_id, "
                        + "change_type, committed_at, committer_name, committer_id, sys_tenant "
                        + "FROM ehr_system.composition WHERE id = ?",
                compositionId);
    }
}
