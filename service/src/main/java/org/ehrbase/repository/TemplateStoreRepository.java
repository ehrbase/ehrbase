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

import static org.ehrbase.jooq.pg.tables.TemplateStore.TEMPLATE_STORE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.xml.namespace.QName;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.jooq.pg.tables.records.TemplateStoreRecord;
import org.ehrbase.service.TimeProvider;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record3;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.stereotype.Repository;

@Repository
public class TemplateStoreRepository {

    private final DSLContext context;
    private final TimeProvider timeProvider;

    public TemplateStoreRepository(DSLContext context, TimeProvider timeProvider) {
        this.context = context;
        this.timeProvider = timeProvider;
    }

    public void store(OPERATIONALTEMPLATE operationaltemplate) {
        TemplateStoreRecord templateStoreRecord = context.newRecord(TEMPLATE_STORE);
        setTemplate(operationaltemplate, templateStoreRecord, rec -> rec.setId(UUID.randomUUID()));
        templateStoreRecord.setCreationTime(timeProvider.getNow());
        templateStoreRecord.store();
    }

    public void update(OPERATIONALTEMPLATE operationaltemplate) {
        String templateId = operationaltemplate.getTemplateId().getValue();
        TemplateStoreRecord templateStoreRecord = context.selectFrom(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.TEMPLATE_ID.eq(templateId))
                .fetchOptional()
                .orElseThrow(() -> new ObjectNotFoundException(
                        "OPERATIONALTEMPLATE", "No template with id = %s".formatted(templateId)));

        setTemplate(operationaltemplate, templateStoreRecord, rec -> rec.setId(rec.getId()));
        templateStoreRecord.setCreationTime(timeProvider.getNow());
        templateStoreRecord.update();
    }

    public List<TemplateMetaData> findAll() {

        return context.select(TEMPLATE_STORE.CONTENT, TEMPLATE_STORE.CREATION_TIME, TEMPLATE_STORE.ID)
                .from(TEMPLATE_STORE)
                .fetch()
                .map(TemplateStoreRepository::buildMetadata);
    }

    public List<String> findAllTemplateIds() {
        return context.select(TEMPLATE_STORE.TEMPLATE_ID)
                .from(TEMPLATE_STORE)
                .fetch()
                .map(Record1::value1);
    }

    private static TemplateMetaData buildMetadata(Record3<String, OffsetDateTime, UUID> r) {
        TemplateMetaData templateMetaData = new TemplateMetaData();
        templateMetaData.setOperationalTemplate(buildOperationaltemplate(r.component1()));
        templateMetaData.setInternalId(r.component3());
        templateMetaData.setCreatedOn(r.component2());
        return templateMetaData;
    }

    public void delete(String templateId) {

        int execute = context.deleteFrom(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.TEMPLATE_ID.eq(templateId))
                .execute();

        if (execute == 0) {
            throw new ObjectNotFoundException("OPERATIONALTEMPLATE", "No template with id = %s".formatted(templateId));
        }
    }

    public Optional<OPERATIONALTEMPLATE> findByTemplateId(String templateId) {

        return context.select(TEMPLATE_STORE.CONTENT)
                .from(TEMPLATE_STORE)
                .where(TEMPLATE_STORE.TEMPLATE_ID.eq(templateId))
                .fetchOptional()
                .map(Record1::value1)
                .map(TemplateStoreRepository::buildOperationaltemplate);
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

    private static OPERATIONALTEMPLATE buildOperationaltemplate(String content) {
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        org.openehr.schemas.v1.TemplateDocument document;
        try {
            document = org.openehr.schemas.v1.TemplateDocument.Factory.parse(inputStream);
        } catch (XmlException | IOException e) {
            throw new InternalServerException(e.getMessage());
        }

        return document.getTemplate();
    }

    private static void setTemplate(
            OPERATIONALTEMPLATE template,
            TemplateStoreRecord templateStoreRecord,
            Consumer<TemplateStoreRecord> setId) {
        setId.accept(templateStoreRecord);
        templateStoreRecord.setTemplateId(template.getTemplateId().getValue());
        XmlOptions opts = new XmlOptions();
        opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
        templateStoreRecord.setContent(template.xmlText(opts));
    }
}
