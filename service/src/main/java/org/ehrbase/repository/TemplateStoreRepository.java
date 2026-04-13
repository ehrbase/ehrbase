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

import static org.ehrbase.jooq.pg.Tables.COMP_VERSION;
import static org.ehrbase.jooq.pg.Tables.COMP_VERSION_HISTORY;
import static org.ehrbase.jooq.pg.tables.TemplateStore.TEMPLATE_STORE;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.jooq.pg.tables.CompVersion;
import org.ehrbase.jooq.pg.tables.CompVersionHistory;
import org.ehrbase.jooq.pg.tables.TemplateStore;
import org.ehrbase.jooq.pg.tables.records.TemplateStoreRecord;
import org.ehrbase.service.TemplateServiceImp.TemplateWithDetails;
import org.ehrbase.service.TimeProvider;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Table;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class TemplateStoreRepository {

    private final DSLContext context;
    private final TimeProvider timeProvider;

    public TemplateStoreRepository(DSLContext context, TimeProvider timeProvider) {
        this.context = context;
        this.timeProvider = timeProvider;
    }

    public TemplateWithDetails store(TemplateWithDetails templateData) {
        TemplateStoreRecord templateStoreRecord = context.newRecord(TEMPLATE_STORE);
        templateStoreRecord.setId(UuidGenerator.randomUUID());
        setTemplateFields(templateData, templateStoreRecord, timeProvider);
        templateStoreRecord.store();
        return buildMetadata(templateStoreRecord);
    }

    public TemplateWithDetails update(TemplateWithDetails templateData) {
        String templateId = templateData.meta().templateId();
        TemplateStoreRecord templateStoreRecord = context.selectFrom(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.TEMPLATE_ID.eq(templateId))
                .fetchOptional()
                .orElseThrow(() -> new ObjectNotFoundException(
                        "OPERATIONALTEMPLATE", "No template with template_id = %s".formatted(templateId)));

        setTemplateFields(templateData, templateStoreRecord, timeProvider);
        templateStoreRecord.update();
        return buildMetadata(templateStoreRecord);
    }

    private void checkUsages() {
        List<String> usedTemplateIds = getAllUsedTemplateIds();
        if (!usedTemplateIds.isEmpty()) {
            boolean single = usedTemplateIds.size() == 1;
            throw new UnprocessableEntityException("Cannot delete %s %s since %s used by at least one composition"
                    .formatted(
                            single ? "template" : "templates",
                            String.join(", ", usedTemplateIds),
                            single ? "it is" : "they are"));
        }
    }

    /**
     *
     * @return number of deleted templated
     */
    public int deleteAllTemplates() {
        checkUsages();
        return context.deleteFrom(TEMPLATE_STORE).execute();
    }

    public List<TemplateService.TemplateDetails> findAllTemplates() {
        TemplateStore templateStore = TEMPLATE_STORE.as("s");
        return context.select(
                        templateStore.ID,
                        templateStore.TEMPLATE_ID,
                        templateStore.CREATION_TIME,
                        templateStore.CONCEPT,
                        templateStore.ROOT_ARCHETYPE)
                .from(templateStore)
                .fetch(r -> new TemplateService.TemplateDetails(
                        r.component1(), r.component2(), r.component3(), r.component4(), r.component5()));
    }

    private static TemplateWithDetails buildMetadata(TemplateStoreRecord rec) {
        return new TemplateWithDetails(
                rec.getContent(),
                new TemplateService.TemplateDetails(
                        rec.getId(),
                        rec.getTemplateId(),
                        rec.getCreationTime(),
                        rec.getConcept(),
                        rec.getRootArchetype()));
    }

    private void delete(UUID id) {
        int execute = context.deleteFrom(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.ID.eq(id))
                .execute();

        if (execute == 0) {
            throw new ObjectNotFoundException("OPERATIONALTEMPLATE", "No template with id = %s".formatted(id));
        }
    }

    /**
     * Deletes an operational template from template storage.<br>
     * The template will be removed physically so ensure that
     * there are no compositions referencing the template.
     *
     * @param uuid - Template pkey to delete from storage
     * @throws UnprocessableEntityException if it is still used
     */
    public void deleteTemplate(UUID uuid) {
        if (isTemplateUsed(uuid)) {
            throw new UnprocessableEntityException(
                    "Cannot delete template %s since it is used by at least one composition"
                            .formatted(findTemplateIdByUuid(uuid).orElse("(unknown)")));
        } else {
            delete(uuid);
        }
    }

    /**
     * Find and returns saved Templates by templateId
     * @param templateIds
     * @return the templates
     */
    public List<TemplateWithDetails> findByTemplateIds(String... templateIds) {

        if (templateIds.length == 0) return List.of();

        return context.selectFrom(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.TEMPLATE_ID.in(templateIds))
                .fetch(TemplateStoreRepository::buildMetadata);
    }

    public Optional<String> findTemplateIdByUuid(UUID uuid) {
        return context.select(TEMPLATE_STORE.TEMPLATE_ID)
                .from(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.ID.eq(uuid))
                .fetchOptional(TEMPLATE_STORE.TEMPLATE_ID);
    }

    public Optional<UUID> findUuidByTemplateId(String templateId) {
        return context.select(TEMPLATE_STORE.ID)
                .from(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.TEMPLATE_ID.eq(templateId))
                .fetchOptional(TEMPLATE_STORE.ID);
    }

    private static void setTemplateFields(
            TemplateWithDetails templateData, TemplateStoreRecord templateStoreRecord, TimeProvider timeProvider) {
        templateStoreRecord.setTemplateId(templateData.meta().templateId());
        templateStoreRecord.setCreationTime(timeProvider.getNow());
        templateStoreRecord.setContent(templateData.operationalTemplate());
        templateStoreRecord.setConcept(templateData.meta().concept());
        templateStoreRecord.setRootArchetype(templateData.meta().archetypeId());
    }

    public boolean isTemplateUsed(UUID templateUuid) {
        CompVersion vTable = COMP_VERSION.as("v");
        CompVersionHistory hTable = COMP_VERSION_HISTORY.as("h");

        return context.select(vTable.VO_ID)
                .from(vTable)
                .where(vTable.TEMPLATE_ID.eq(templateUuid))
                .limit(1)
                .unionAll(context.select(hTable.VO_ID)
                        .from(hTable)
                        .where(hTable.TEMPLATE_ID.eq(templateUuid))
                        .limit(1))
                .limit(1)
                .fetchOptional()
                .isPresent();
    }

    public List<String> getAllUsedTemplateIds() {
        CompVersion vTable = COMP_VERSION.as("v");
        CompVersionHistory hTable = COMP_VERSION_HISTORY.as("h");

        Field<UUID> tid = vTable.TEMPLATE_ID.as("tid");

        Table<Record1<UUID>> usedTemplateUuids = context.selectDistinct(tid)
                .from(vTable)
                .union(context.selectDistinct(hTable.TEMPLATE_ID).from(hTable))
                .asTable("used_tids");

        return context.selectDistinct(TEMPLATE_STORE.TEMPLATE_ID)
                .from(TEMPLATE_STORE)
                .join(usedTemplateUuids)
                .on(TEMPLATE_STORE.ID.eq(usedTemplateUuids.field(tid)))
                .fetch(TEMPLATE_STORE.TEMPLATE_ID);
    }
}
