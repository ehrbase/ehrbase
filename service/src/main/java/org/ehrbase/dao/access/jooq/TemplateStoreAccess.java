/*
 *
 *  * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
 *  *
 *  * This file is part of project EHRbase
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.ehrbase.dao.access.jooq;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_TemplateStoreAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.TemplateStoreRecord;
import org.jooq.Record1;
import org.jooq.Result;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.ehrbase.jooq.pg.tables.TemplateStore.TEMPLATE_STORE;

public class TemplateStoreAccess extends DataAccess implements I_TemplateStoreAccess {


    private TemplateStoreRecord templateStoreRecord;

    public TemplateStoreAccess(I_DomainAccess domainAccess, OPERATIONALTEMPLATE operationaltemplate) {
        super(domainAccess);
        templateStoreRecord = domainAccess.getContext().newRecord(TEMPLATE_STORE);
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
        return commit(Timestamp.valueOf(LocalDateTime.now()));
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
                //hack: force tell jOOQ to perform updateComposition whatever...
                templateStoreRecord.changed(true);
            }
            templateStoreRecord.setSysTransaction(transactionTime);


            updated = templateStoreRecord.store() == 1;
        }

        return updated;
    }

    @Override
    public Boolean update() {
        return update(Timestamp.valueOf(LocalDateTime.now()), false);
    }

    @Override
    public Boolean update(Boolean force) {
        return update(Timestamp.valueOf(LocalDateTime.now()), force);
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
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        org.openehr.schemas.v1.TemplateDocument document = null;
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
        templateStoreAccess.templateStoreRecord = domainAccess.getContext().fetchOne(TEMPLATE_STORE, TEMPLATE_STORE.TEMPLATE_ID.eq(templateId));
        return templateStoreAccess;
    }

    public static List<OPERATIONALTEMPLATE> fetchAll(I_DomainAccess domainAccess) {
        Result<Record1<String>> records = domainAccess.getContext().select(TEMPLATE_STORE.CONTENT).from(TEMPLATE_STORE).fetch();
        return records.getValues(0).stream()
                .map(s -> (String) s)
                .map(TemplateStoreAccess::buildOperationaltemplate)
                .collect(Collectors.toList());

    }
}
