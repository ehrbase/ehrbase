/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.dao.access.jooq;

import static org.ehrbase.jooq.pg.tables.TemplateStore.TEMPLATE_STORE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_TemplateStoreAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.tables.records.AdminGetTemplateUsageRecord;
import org.ehrbase.jooq.pg.tables.records.TemplateStoreRecord;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

public class TemplateStoreAccess extends DataAccess implements I_TemplateStoreAccess {

    private TemplateStoreRecord templateStoreRecord;

    public TemplateStoreAccess(I_DomainAccess domainAccess, OPERATIONALTEMPLATE operationaltemplate, Short sysTenant) {
        super(domainAccess);
        templateStoreRecord = domainAccess.getContext().newRecord(TEMPLATE_STORE);
        templateStoreRecord.setSysTenant(sysTenant);
        setTemplate(operationaltemplate);
    }

    // internal minimal constructor - needs proper initialization before following usage
    private TemplateStoreAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    @Override
    public UUID commit(Timestamp transactionTime) {
        templateStoreRecord.setSysTransaction(transactionTime);
        templateStoreRecord.store();
        return templateStoreRecord.getId();
    }

    @Override
    public UUID commit() {
        return commit(TransactionTime.millis());
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        return update(transactionTime, false);
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        boolean updated = false;

        if (force || templateStoreRecord.changed()) {

            if (!templateStoreRecord.changed()) {
                // hack: force tell jOOQ to perform update whatever...
                templateStoreRecord.changed(true);
            }
            templateStoreRecord.setSysTransaction(transactionTime);

            updated = templateStoreRecord.update() == 1;
        }

        return updated;
    }

    @Override
    public Boolean update() {
        return update(TransactionTime.millis(), false);
    }

    @Override
    public Boolean update(Boolean force) {
        return update(TransactionTime.millis(), force);
    }

    @Override
    public Integer delete() {
        int count = 0;
        count += templateStoreRecord.delete();
        return count;
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public OPERATIONALTEMPLATE getTemplate() {

        return Optional.ofNullable(templateStoreRecord)
                .map(TemplateStoreRecord::getContent)
                .map(TemplateStoreAccess::buildOperationaltemplate)
                .orElse(null);
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

    @Override
    public void setTemplate(OPERATIONALTEMPLATE template) {
        templateStoreRecord.setId(UUID.fromString(template.getUid().getValue()));
        templateStoreRecord.setTemplateId(template.getTemplateId().getValue());
        XmlOptions opts = new XmlOptions();
        opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
        templateStoreRecord.setContent(template.xmlText(opts));
    }

    public static I_TemplateStoreAccess retrieveInstanceByTemplateId(I_DomainAccess domainAccess, String templateId) {
        TemplateStoreAccess templateStoreAccess = new TemplateStoreAccess(domainAccess);
        templateStoreAccess.templateStoreRecord =
                domainAccess.getContext().fetchOne(TEMPLATE_STORE, TEMPLATE_STORE.TEMPLATE_ID.eq(templateId));
        return templateStoreAccess;
    }

    public static List<TemplateMetaData> fetchAll(I_DomainAccess domainAccess) {
        Result<Record2<String, Timestamp>> records = domainAccess
                .getContext()
                .select(TEMPLATE_STORE.CONTENT, TEMPLATE_STORE.SYS_TRANSACTION)
                .from(TEMPLATE_STORE)
                .fetch();
        return records.parallelStream().map(TemplateStoreAccess::buildMetadata).collect(Collectors.toList());
    }

    public static Set<String> fetchAllTemplateIds(I_DomainAccess domainAccess) {
        Result<Record1<String>> records = domainAccess
                .getContext()
                .select(TEMPLATE_STORE.TEMPLATE_ID)
                .from(TEMPLATE_STORE)
                .fetch();
        return records.parallelStream().map(Record1::component1).collect(Collectors.toSet());
    }

    /**
     * Replaces the old content of a template with the new provided content in the database storage.
     * The target template id must be provided within the new template. This is a destructive
     * operation thus the old template will be checked against Compositions if there are any usages of
     * the template to avoid data inconsistencies.
     *
     * @param domainAccess - Database connection context
     * @param template     - New template data to store
     * @return - Updated template XML content
     */
    public static String adminUpdateTemplate(I_DomainAccess domainAccess, OPERATIONALTEMPLATE template) {

        // Check if template is used anymore
        Result<AdminGetTemplateUsageRecord> usingCompositions = Routines.adminGetTemplateUsage(
                domainAccess.getContext().configuration(),
                template.getTemplateId().getValue());

        if (usingCompositions.isNotEmpty()) {
            // There are compositions using this template -> Return list of uuids
            throw new UnprocessableEntityException(String.format(
                    "Cannot delete template %s since the following compositions are still using it %s",
                    template.getTemplateId().getValue(), usingCompositions.toString()));
        } else {
            XmlOptions opts = new XmlOptions();
            opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
            // Replace template with db function
            return Routines.adminUpdateTemplate(
                    domainAccess.getContext().configuration(),
                    template.getTemplateId().getValue(),
                    template.xmlText(opts));
        }
    }

    /**
     * Removes the template identified by its template_id from database and returns if the operation
     * succeeded.
     *
     * @param domainAccess - Database access instance
     * @param templateId   - Target template_id, e.g. "IDCR - Problem List.v1"
     * @return - Deletion succeeded or not
     */
    public static boolean deleteTemplate(I_DomainAccess domainAccess, String templateId) {

        // Check if template is used in any composition
        Result<AdminGetTemplateUsageRecord> usingCompositions =
                Routines.adminGetTemplateUsage(domainAccess.getContext().configuration(), templateId);
        if (usingCompositions.isNotEmpty()) {
            // There are compositions using this template -> Return list of uuids
            throw new UnprocessableEntityException(String.format(
                    "Cannot delete template %s since the following compositions are still using it %s",
                    templateId, usingCompositions.toString()));
        } else {
            // Template no longer used -> Delete
            return Routines.adminDeleteTemplate(domainAccess.getContext().configuration(), templateId) > 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public static int adminDeleteAllTemplates(I_DomainAccess domainAccess) {

        return Routines.adminDeleteAllTemplates(domainAccess.getContext().configuration());
    }

    private static TemplateMetaData buildMetadata(Record2<String, Timestamp> r) {
        TemplateMetaData templateMetaData = new TemplateMetaData();
        templateMetaData.setOperationaltemplate(TemplateStoreAccess.buildOperationaltemplate(r.component1()));
        // @TODO read from DB
        templateMetaData.setCreatedOn(OffsetDateTime.ofInstant(r.component2().toInstant(), ZoneId.systemDefault()));
        return templateMetaData;
    }
}
