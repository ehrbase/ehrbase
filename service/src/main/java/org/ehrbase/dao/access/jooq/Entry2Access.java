/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nedap.archie.rm.composition.Composition;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.aql.dto.path.AqlPath;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_SimpleCRUD;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.jooq.pg.tables.records.Entry2Record;
import org.ehrbase.serialisation.matrixencoding.MatrixFormat;
import org.ehrbase.serialisation.matrixencoding.Row;
import org.ehrbase.service.KnowledgeCacheServiceTemplateProvider;
import org.ehrbase.webtemplate.templateprovider.TemplateProvider;
import org.jooq.JSONB;

/**
 * @author Stefan Spiska
 */
public class Entry2Access extends DataAccess implements I_SimpleCRUD {

    private final DataAccess dataAccess;

    private final List<Entry2Record> matrix;

    public static Entry2Access getNewInstance(I_DomainAccess domain, Composition composition, UUID ehrId) {

        return new Entry2Access(domain, composition, ehrId);
    }

    private Entry2Access(I_DomainAccess domainAccess, Composition composition, UUID ehrId) {
        super(domainAccess);
        dataAccess = domainAccess.getDataAccess();

        TemplateProvider templateProvider = new KnowledgeCacheServiceTemplateProvider(
                domainAccess.getKnowledgeManager(), domainAccess.getIntrospectService());

        List<Row> rows = new MatrixFormat(templateProvider).toTable(composition);

        UUID compositionId = UUID.fromString(composition.getUid().getRoot().getValue());

        matrix = rows.stream()
                .map(r -> {
                    Entry2Record entry2Record = new Entry2Record();
                    entry2Record.setEhrId(ehrId);
                    entry2Record.setCompId(compositionId);
                    entry2Record.setEntityConcept(r.getArchetypeId());
                    entry2Record.setRmEntity(findTypeName(r.getArchetypeId()));
                    entry2Record.setEntityPath(r.getEntityPath().format(AqlPath.OtherPredicatesFormat.SHORTED, true));
                    entry2Record.setNum(r.getNum());
                    entry2Record.setFieldIdx(r.getFieldIdx());
                    entry2Record.setEntityIdx(r.getEntityIdx());
                    entry2Record.setFieldIdxLen(r.getFieldIdx().length);
                    try {
                        entry2Record.setFields(
                                JSONB.jsonbOrNull(MatrixFormat.MAPPER.writeValueAsString(r.getFields())));
                    } catch (JsonProcessingException e) {
                        throw new InternalServerException(e);
                    }

                    return entry2Record;
                })
                .collect(Collectors.toList());
    }

    @Override
    public DataAccess getDataAccess() {
        return dataAccess;
    }

    @Override
    public UUID commit(Timestamp transactionTime) {
        dataAccess.getContext().batchInsert(matrix).execute();

        return matrix.stream().findAny().map(Entry2Record::getCompId).orElseThrow();
    }

    public UUID commit() {
        return commit(TransactionTime.millis());
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        return update(transactionTime, false);
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {

        delete();
        int[] execute = dataAccess.getContext().batchInsert(matrix).execute();
        return Arrays.stream(execute).sum() == matrix.size();
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
        int[] execute = dataAccess.getContext().batchDelete(matrix).execute();
        return Arrays.stream(execute).sum();
    }

    private static String findTypeName(String atCode) {
        String typeName = null;

        if (atCode.contains("openEHR-EHR-")) {

            typeName = StringUtils.substringBetween(atCode, "openEHR-EHR-", ".");
        } else if (atCode.startsWith("at")) {
            typeName = null;
        } else {
            typeName = atCode;
        }
        return typeName;
    }
}
