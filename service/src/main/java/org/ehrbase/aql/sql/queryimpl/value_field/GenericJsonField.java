/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.queryimpl.value_field;

import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonArraySplitElements;
import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathItem;
import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathItemAsText;
import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathParameters;
import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_MARKER;
import static org.ehrbase.aql.sql.queryimpl.value_field.Functions.apply;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.ehrbase.aql.sql.queryimpl.FunctionBasedNodePredicateCall;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributePath;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.GenericJsonPath;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.JsonbSelect;
import org.ehrbase.aql.sql.queryimpl.attribute.RMObjectAttribute;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.Function2;
import org.jooq.Function4;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.jooq.TableField;
import org.jooq.impl.DSL;

/**
 * @author Christian Chevalley
 * @since 1.0
 */
@SuppressWarnings({"java:S3776", "java:S3740"})
public class GenericJsonField extends RMObjectAttribute {

    protected Optional<String> jsonPath = Optional.empty();

    private static final String ITERATIVE_MARKER = "'" + AQL_NODE_ITERATIVE_MARKER + "'";

    public GenericJsonField(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    public Field hierObjectId(Field<UUID> uuidField) {
        String rmType = "HIER_OBJECT_ID";
        Function<Field<UUID>, Field<JSON>> function = Routines::jsCanonicalHierObjectId1;
        return jsonField(rmType, function, (TableField) uuidField);
    }

    public Field dvCodedText(Field<DvCodedTextRecord> dvCodedTextRecordTableField) {
        String rmType = "DV_CODED_TEXT";
        Function<Field<DvCodedTextRecord>, Field<JSON>> function = Routines::jsDvCodedTextInner1;
        return jsonField(rmType, function, (TableField) dvCodedTextRecordTableField);
    }

    public Field eventContext(Field<UUID> uuidField) {
        String rmType = "EVENT_CONTEXT";
        Function<Field<UUID>, Field<JSON>> function = Routines::jsContext;
        return jsonField(rmType, function, (TableField) uuidField);
    }

    public Field participations(Field<UUID> uuidField) {
        String rmType = "PARTICIPATION";
        Function<Field<UUID>, Field<JSONB[]>> function = Routines::jsParticipations;
        return jsonField(rmType, function, (TableField) uuidField);
    }

    public Field partyRef(Field<String> namespace, Field<String> type, Field<String> scheme, Field<String> value) {
        String rmType = "PARTY_REF";
        Function4<Field<String>, Field<String>, Field<String>, Field<String>, Field<JSON>> function =
                Routines::jsPartyRef;
        return jsonField(
                rmType, function, (TableField) namespace, (TableField) type, (TableField) scheme, (TableField) value);
    }

    public Field dvDateTime(Field<Timestamp> dateTime, Field<String> timeZoneId) {
        String rmType = "DV_DATE_TIME";
        Function2<Field<Timestamp>, Field<String>, Field<JSON>> function = Routines::jsDvDateTime;
        return jsonField(rmType, function, (TableField) dateTime, (TableField) timeZoneId);
    }

    public Field ehrStatus(Field<UUID> uuidField, Field<String> serverId) {
        String rmType = null;
        Function2<Field<UUID>, Field<String>, Field<JSON>> function = Routines::jsEhrStatus2;
        return jsonField(rmType, function, (TableField) uuidField, (TableField) serverId);
    }

    public Field ehrStatus(Field<UUID> uuidField) {
        String rmType = null;
        Function<Field<UUID>, Field<JSON>> function = Routines::jsEhrStatus1;
        return jsonField(rmType, function, (TableField) uuidField);
    }

    public Field canonicalPartyIdendified(Field<UUID> uuidField) {
        String rmType = "PARTY_IDENTIFIED";
        Function<Field<UUID>, Field<JSON>> function = Routines::jsCanonicalPartyIdentified;
        return jsonField(rmType, function, (TableField) uuidField);
    }

    public Field feederAudit(Field<?> feederAudit) {
        String rmType = "FEEDER_AUDIT";
        return jsonField(rmType, null, (TableField) feederAudit);
    }

    public Field jsonField(String rmType, Object function, TableField... tableFields) {
        fieldContext.setRmType(rmType);
        Configuration configuration = fieldContext.getContext().configuration();

        Field jsonField;

        if (jsonPath.isPresent()) {
            List<String> tokenized = Arrays.asList(jsonpathParameters(jsonPath.get()));

            if (tokenized.contains(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER)) {
                // replace the ITERATIVE_MARKERs by default index
                Collections.replaceAll(tokenized, ITERATIVE_MARKER, "'0'");
                jsonField = new FunctionBasedNodePredicateCall(fieldContext, tokenized).resolve(function, tableFields);
            } else if (tokenized.contains(ITERATIVE_MARKER))
                jsonField = fieldWithJsonArrayIteration(configuration, tokenized, function, tableFields);
            else
                jsonField = DSL.field(jsonpathItemAsText(
                        configuration,
                        DSL.field(apply(function, tableFields).toString()).cast(JSONB.class),
                        tokenized.toArray(new String[] {})));

        } else jsonField = DSL.field(apply(function, tableFields).toString()).cast(String.class);

        // check if the SQL expression contains a set returned in a WHERE clause (implying a lateral join)
        if (jsonField.toString().contains(QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION)
                && fieldContext.getClause().equals(IQueryImpl.Clause.WHERE))
            jsonField = DSL.field(DSL.select(jsonField));

        return as(DSL.field(jsonField));
    }

    private Field fieldWithJsonArrayIteration(
            Configuration configuration, List<String> tokenized, Object function, TableField... tableFields) {

        String[] prefix =
                tokenized.subList(0, tokenized.indexOf(ITERATIVE_MARKER)).toArray(new String[] {});
        String[] remaining = tokenized
                .subList(tokenized.indexOf(ITERATIVE_MARKER) + 1, tokenized.size())
                .toArray(new String[] {});

        // initial
        Field field = jsonpathItem(
                configuration,
                DSL.field(apply(function, tableFields).toString()).cast(JSONB.class),
                prefix);

        while (remaining.length > 0) {
            List<String> tokens = Arrays.asList(remaining.clone());
            if (tokens.contains(ITERATIVE_MARKER)) {
                prefix = tokens.subList(0, tokens.indexOf(ITERATIVE_MARKER)).toArray(new String[] {});
                remaining = tokens.subList(tokens.indexOf(ITERATIVE_MARKER) + 1, tokens.size())
                        .toArray(new String[] {});
            } else {
                prefix = remaining;
                remaining = new String[] {};
            }

            field = DSL.field(jsonpathItemAsText(
                    configuration, jsonArraySplitElements(configuration, field.cast(JSONB.class)), prefix));
        }

        return field;
    }

    @Override
    public Field sqlField() {
        return null;
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        return this;
    }

    public GenericJsonField forJsonPath(String jsonPath) {
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }

        GenericJsonPath genericJsonPath = new GenericJsonPath(jsonPath);

        this.jsonPath = Optional.of(genericJsonPath.jqueryPath());
        fieldContext.setUsingSetReturningFunction(genericJsonPath.isIterative());

        return this;
    }

    public GenericJsonField forJsonPath(String root, String jsonPath) {
        String actualPath = new AttributePath(root).redux(jsonPath);
        return forJsonPath(actualPath);
    }

    public GenericJsonField forJsonPath(String[] path) {
        this.jsonPath = Optional.of(new JsonbSelect(Arrays.asList(path)).field());
        return this;
    }
}
