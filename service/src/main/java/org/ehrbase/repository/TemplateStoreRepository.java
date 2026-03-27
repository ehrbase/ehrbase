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
import static org.ehrbase.jooq.pg.tables.TemplateStore.TEMPLATE_STORE;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.xml.namespace.QName;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.jooq.pg.tables.TemplateStore;
import org.ehrbase.jooq.pg.tables.records.TemplateStoreRecord;
import org.ehrbase.service.TimeProvider;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
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

    public TemplateMetaData store(OPERATIONALTEMPLATE operationaltemplate) {
        TemplateStoreRecord templateStoreRecord = context.newRecord(TEMPLATE_STORE);
        templateStoreRecord.setId(UuidGenerator.randomUUID());
        setTemplateFields(operationaltemplate, templateStoreRecord, timeProvider);
        templateStoreRecord.store();
        return buildMetadata(
                templateStoreRecord.getId(), templateStoreRecord.getCreationTime(), templateStoreRecord.getContent());
    }

    public TemplateMetaData update(OPERATIONALTEMPLATE operationaltemplate) {
        String templateId = operationaltemplate.getTemplateId().getValue();
        TemplateStoreRecord templateStoreRecord = context.selectFrom(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.TEMPLATE_ID.eq(templateId))
                .fetchOptional()
                .orElseThrow(() -> new ObjectNotFoundException(
                        "OPERATIONALTEMPLATE", "No template with id = %s".formatted(templateId)));

        setTemplateFields(operationaltemplate, templateStoreRecord, timeProvider);
        templateStoreRecord.update();
        return buildMetadata(
                templateStoreRecord.getId(), templateStoreRecord.getCreationTime(), templateStoreRecord.getContent());
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

    private static TemplateMetaData buildMetadata(Record3<String, OffsetDateTime, UUID> r) {
        return buildMetadata(r.component3(), r.component2(), r.component1());
    }

    private static TemplateMetaData buildMetadata(
            UUID internalId, OffsetDateTime creationTime, String templateContent) {
        TemplateMetaData templateMetaData = new TemplateMetaData();
        templateMetaData.setOperationalTemplate(templateContent);
        templateMetaData.setCreatedOn(creationTime);
        templateMetaData.setInternalId(internalId);
        return templateMetaData;
    }

    public void delete(UUID id) {

        int execute = context.deleteFrom(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.ID.eq(id))
                .execute();

        if (execute == 0) {
            throw new ObjectNotFoundException("OPERATIONALTEMPLATE", "No template with id = %s".formatted(id));
        }
    }

    @Deprecated(forRemoval = true)
    public void delete(String templateId) {

        int execute = context.deleteFrom(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.TEMPLATE_ID.eq(templateId))
                .execute();

        if (execute == 0) {
            throw new ObjectNotFoundException("OPERATIONALTEMPLATE", "No template with id = %s".formatted(templateId));
        }
    }

    /**
     * Find and returns saved Templates by templateId
     * @param templateIds
     * @return the templates
     */
    public List<TemplateMetaData> findByTemplateIds(String... templateIds) {

        if (templateIds.length == 0) return List.of();

        return context.select(TEMPLATE_STORE.CONTENT, TEMPLATE_STORE.CREATION_TIME, TEMPLATE_STORE.ID)
                .from(TEMPLATE_STORE)
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
            OPERATIONALTEMPLATE template, TemplateStoreRecord templateStoreRecord, TimeProvider timeProvider) {
        templateStoreRecord.setTemplateId(template.getTemplateId().getValue());
        templateStoreRecord.setCreationTime(timeProvider.getNow());

        XmlOptions opts = new XmlOptions();
        opts.setSaveSyntheticDocumentElement(
                new QName("http://schemas.openehr.org/v1", "template")); // XXX CDR-2305 v2???
        templateStoreRecord.setContent(template.xmlText(opts));

        templateStoreRecord.setConcept(template.getConcept());
        templateStoreRecord.setRootArchetype(
                template.getDefinition().getArchetypeId().getValue());
    }

    public List<String> getTemplateUsages() {
        return context.selectDistinct(TEMPLATE_STORE.TEMPLATE_ID)
                .from(TEMPLATE_STORE)
                .join(COMP_VERSION)
                .on(COMP_VERSION.TEMPLATE_ID.eq(TEMPLATE_STORE.ID))
                .fetch(TEMPLATE_STORE.TEMPLATE_ID);
    }
}
