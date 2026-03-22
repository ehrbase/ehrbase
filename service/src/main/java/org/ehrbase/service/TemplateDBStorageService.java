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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TemplateStorage implementation using JOOQ DSL against ehr_system.template table.
 * Replaces the old TemplateDBStorageService that used TemplateStoreRepository (deleted).
 */
@Service
@Transactional
public class TemplateDBStorageService implements TemplateStorage {

    private static final Logger log = LoggerFactory.getLogger(TemplateDBStorageService.class);
    private static final org.jooq.Table<?> TEMPLATE = table(name("ehr_system", "template"));

    private final DSLContext dsl;
    private final boolean allowTemplateOverwrite;

    public TemplateDBStorageService(
            DSLContext dsl,
            @Value("${ehrbase.template.allow-overwrite:false}") boolean allowTemplateOverwrite) {
        this.dsl = dsl;
        this.allowTemplateOverwrite = allowTemplateOverwrite;
    }

    @Override
    public boolean allowTemplateOverwrite() {
        return allowTemplateOverwrite;
    }

    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        return dsl.select()
                .from(TEMPLATE)
                .fetch()
                .stream()
                .map(this::toMetaData)
                .toList();
    }

    @Override
    public Map<UUID, String> findAllTemplateIds() {
        return dsl.select(field(name("id"), UUID.class), field(name("template_id"), String.class))
                .from(TEMPLATE)
                .fetchMap(field(name("id"), UUID.class), field(name("template_id"), String.class));
    }

    @Override
    public TemplateMetaData storeTemplate(OPERATIONALTEMPLATE template) {
        String templateId = template.getTemplateId().getValue();
        String content = template.xmlText();

        dsl.insertInto(TEMPLATE)
                .set(field(name("template_id"), String.class), templateId)
                .set(field(name("content"), String.class), content)
                .set(field(name("sys_tenant"), Short.class), (short) 1)
                .execute();

        log.debug("Stored template: {}", templateId);
        return readTemplate(templateId).orElseThrow();
    }

    @Override
    public Optional<TemplateMetaData> readTemplate(String templateId) {
        Record rec = dsl.select()
                .from(TEMPLATE)
                .where(field(name("template_id"), String.class).eq(templateId))
                .fetchOne();
        return Optional.ofNullable(rec).map(this::toMetaData);
    }

    @Override
    public void deleteTemplate(String templateId) {
        int deleted = dsl.deleteFrom(TEMPLATE)
                .where(field(name("template_id"), String.class).eq(templateId))
                .execute();
        log.debug("Deleted template '{}': {} rows", templateId, deleted);
    }

    @Override
    public List<Pair<UUID, String>> deleteAllTemplates() {
        List<Pair<UUID, String>> deleted = dsl.select(
                        field(name("id"), UUID.class), field(name("template_id"), String.class))
                .from(TEMPLATE)
                .fetch()
                .stream()
                .map(r -> Pair.of(
                        r.get(field(name("id"), UUID.class)), r.get(field(name("template_id"), String.class))))
                .toList();
        dsl.deleteFrom(TEMPLATE).execute();
        return deleted;
    }

    @Override
    public Optional<String> findTemplateIdByUuid(UUID uuid) {
        return Optional.ofNullable(dsl.select(field(name("template_id"), String.class))
                .from(TEMPLATE)
                .where(field(name("id"), UUID.class).eq(uuid))
                .fetchOne(field(name("template_id"), String.class)));
    }

    @Override
    public Optional<UUID> findUuidByTemplateId(String templateId) {
        return Optional.ofNullable(dsl.select(field(name("id"), UUID.class))
                .from(TEMPLATE)
                .where(field(name("template_id"), String.class).eq(templateId))
                .fetchOne(field(name("id"), UUID.class)));
    }

    private TemplateMetaData toMetaData(Record rec) {
        TemplateMetaData meta = new TemplateMetaData();
        String content = rec.get(field(name("content"), String.class));
        if (content != null) {
            try {
                OPERATIONALTEMPLATE opt = TemplateDocument.Factory.parse(
                                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
                        .getTemplate();
                meta.setOperationalTemplate(opt);
            } catch (Exception e) {
                log.warn("Failed to parse template content: {}", e.getMessage());
            }
        }
        meta.setInternalId(rec.get(field(name("id"), UUID.class)));
        OffsetDateTime createdAt = rec.get(field(name("creation_time"), OffsetDateTime.class));
        if (createdAt != null) {
            meta.setCreatedOn(createdAt);
        }
        return meta;
    }
}
