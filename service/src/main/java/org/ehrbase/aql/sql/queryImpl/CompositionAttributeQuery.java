/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.aql.sql.queryImpl;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.binding.I_JoinBinder;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Timestamp;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * map an AQL datavalue expression into a SQL field
 * <p>
 * Created by christian on 5/6/2016.
 */
public class CompositionAttributeQuery extends ObjectQuery implements I_QueryImpl, I_JoinBinder {

    private String serverNodeId;
    private String columnAlias;
    private boolean containsEhrStatus = false;
    private boolean containsOtherContext = false;
    private boolean containsEhrId = false;
    private String ehrIdAlias;

    private boolean ehrIdFiltered = false; //true if the query specifies the ehr id (in the AQL FROM clause)
    private boolean compositionIdFiltered = false; //true if the query contains a where clause with composition id specified
    private boolean compositionIdField = false;

    //boolean indicating the resulting joins to generate
    private boolean joinComposition = false;
    private boolean joinEventContext = false;
    private boolean joinSubject = false;
    private boolean joinEhr = false;
    private boolean joinEhrStatus = false;
    private boolean joinComposer = false;
    private boolean joinContextFacility = false;

    private final String entry_root;
    //    private MetaData metaData;
    private final IntrospectService introspectCache;

    public CompositionAttributeQuery(DSLContext context, PathResolver pathResolver, String serverNodeId, String entry_root, IntrospectService introspectCache) {
        super(context, pathResolver);
        this.serverNodeId = serverNodeId;
        this.entry_root = entry_root;
        this.introspectCache = introspectCache;
    }

    @Override
    public Field<?> makeField(String templateId, UUID compositionId, String identifier, I_VariableDefinition variableDefinition, boolean withAlias, Clause clause) {
        //resolve composition attributes and/or context
        columnAlias = variableDefinition.getPath();
        compositionIdField = false;
        if (columnAlias == null) {
//            if (identifier.contains("/")) //a composite, unsupported attribute
//                return null;
//            else {
//                //assume it is the whole composition object
//                return rawUid(compositionId, withAlias, variableDefinition.getAlias());
//            }
            return null;
        }
        switch (columnAlias) {
            case "uid/value":
                if (clause == Clause.WHERE)
                    compositionIdFiltered = true;
                else
                    compositionIdField = true;

                joinComposition = true;

                if (withAlias)
                    return uid(compositionId, withAlias, variableDefinition.getAlias());
                else {
                    return rawUid(compositionId, withAlias, variableDefinition.getAlias());
                }
//                return rawUid(compositionId, withAlias, variableDefinition.getAlias());
            case "name/value":
                return name(compositionId, withAlias, variableDefinition.getAlias(), clause);
            case "archetype_node_id":
                return archetypeNodeId(compositionId, withAlias, variableDefinition.getAlias());
            case "template_id":
                return templateId(compositionId, withAlias, variableDefinition.getAlias());
            case "language/value":
                joinComposition = true;
                return language(compositionId, withAlias, variableDefinition.getAlias());
            case "territory/value":
                joinComposition = true;
                return territory(compositionId, withAlias, variableDefinition.getAlias());
            case "composer/name":
                joinComposer = true;
                return composerNameValue(compositionId, withAlias, variableDefinition.getAlias());
            case "composer/id/namespace":
            case "composer/external_ref/namespace":
                joinComposer = true;
                return composerIdNamespace(compositionId, withAlias, variableDefinition.getAlias());
            case "composer/id/scheme":
            case "composer/external_ref/scheme":
                joinComposer = true;
                return composerIdScheme(compositionId, withAlias, variableDefinition.getAlias());
            case "composer/id/ref":
            case "composer/external_ref/id/value":
                joinComposer = true;
                return composerIdRef(compositionId, withAlias, variableDefinition.getAlias());
            case "composer/type":
                joinComposer = true;
                return composerType(compositionId, withAlias, variableDefinition.getAlias());
            case "context/start_time/value":
                joinEventContext = true;
                return contextStartTime(compositionId, withAlias, variableDefinition.getAlias());
            case "context/end_time/value":
                joinEventContext = true;
                return contextEndTime(compositionId, withAlias, variableDefinition.getAlias());
            case "context/location":
                joinEventContext = true;
                return contextLocation(compositionId, withAlias, variableDefinition.getAlias());
            case "context/facility/name/value":
            case "context/health_care_facility/name/value":
                joinContextFacility = true;
                return contextFacilityName(compositionId, withAlias, variableDefinition.getAlias());
            case "context/facility/id/namespace":
            case "context/health_care_facility/id/namespace":
                joinContextFacility = true;
                return contextFacilityNamespace(compositionId, withAlias, variableDefinition.getAlias());
            case "context/facility/id/ref":
            case "context/health_care_facility/id/ref":
                joinContextFacility = true;
                return contextFacilityRef(compositionId, withAlias, variableDefinition.getAlias());
            case "context/facility/id/scheme":
            case "context/health_care_facility/id/scheme":
                joinContextFacility = true;
                return contextFacilityScheme(compositionId, withAlias, variableDefinition.getAlias());
            case "context/facility/id/type":
            case "context/health_care_facility/id/type":
                joinContextFacility = true;
                return contextFacilityType(compositionId, withAlias, variableDefinition.getAlias());
            case "ehr_status/subject/external_ref/namespace":
                joinSubject = true;
                return ehrStatusSubjectNamespace(compositionId, withAlias, variableDefinition.getAlias());
            case "ehr_status/subject/external_ref/id/value":
                joinSubject = true;
                return ehrStatusSubjectIdValue(compositionId, withAlias, variableDefinition.getAlias());
            case "ehr_id/value":
                if (clause == Clause.FROM)
                    ehrIdFiltered = true;
                return ehrIdValue(compositionId, withAlias, variableDefinition.getAlias());
            case "archetype_details/template_id/value":
                return templateIdValue(compositionId, withAlias, variableDefinition.getAlias());


        }
        if (columnAlias.startsWith("ehr_status/other_details")) {
            joinEhrStatus = true;
            return ehrStatusOtherDetails(variableDefinition, withAlias);
        }
        if (columnAlias.startsWith("context/other_context")) {
            joinEventContext = true;
            return ehrContextOtherContext(variableDefinition, withAlias);
        }
        throw new IllegalArgumentException("Could not interpret field:" + columnAlias);
    }

    @Override
    public Field<?> whereField(String templateId, UUID compositionId, String identifier, I_VariableDefinition variableDefinition) {
        return makeField(templateId, compositionId, identifier, variableDefinition, false, Clause.WHERE);
    }

    private Field<?> uid(UUID compositionId, boolean alias, String aliasStr) {

        //use inline SQL as it seems coalesce is not going through with POSTGRES dialect
        SelectQuery<?> subSelect = context.selectQuery();
        subSelect.addSelect(DSL.count());
        subSelect.addFrom(COMPOSITION_HISTORY);
        subSelect.addConditions(I_JoinBinder.compositionRecordTable.field("id", UUID.class).eq(COMPOSITION_HISTORY.ID));
        subSelect.addGroupBy(COMPOSITION_HISTORY.ID);

        String coalesceVersion = "1 + COALESCE(\n(" + subSelect + "), 0)";

        Field<?> select = DSL.field(I_JoinBinder.compositionRecordTable.field("id")
                        + "||"
                        + DSL.val("::")
                        + "||"
                        + DSL.val(serverNodeId)
                        + "||"
                        + DSL.val("::")
                        + "||"
                        + DSL.field(coalesceVersion)
                , SQLDataType.VARCHAR)
                .as(alias && aliasStr != null && !aliasStr.isEmpty() ? aliasStr : "uid");

        return select;
    }

    private Field<?> rawUid(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(I_JoinBinder.compositionRecordTable.field("id", UUID.class).as(aliasStr == null ? columnAlias : aliasStr));
            return select;
        } else
            return DSL.field(I_JoinBinder.compositionRecordTable.field("id", UUID.class));
    }

    private Field<?> name(UUID compositionId, boolean alias, String aliasStr, Clause clause) {
        //extract the composition name from the jsonb root key
        String trimName = "trim(LEADING '''' FROM (trim(TRAILING ''']' FROM\n" +
                " (regexp_split_to_array((select root_json_key from jsonb_object_keys(" + ENTRY.ENTRY_ + ") root_json_key where root_json_key like '/composition%')," +
                " 'and name/value=')) [2])))";
        //postgresql equivalent expression
        if (alias) {
            Field<?> select = DSL.field(trimName).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else {
            if (clause.equals(Clause.WHERE)) {
                trimName = "(SELECT " + trimName + ")";
            }
            return DSL.field(trimName);
        }
    }

    private Field<?> archetypeNodeId(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(ENTRY.ARCHETYPE_ID).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(ENTRY.ARCHETYPE_ID);
    }

    private Field<?> templateId(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(ENTRY.TEMPLATE_ID).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(ENTRY.TEMPLATE_ID);
    }

    private Field<?> language(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(COMPOSITION.LANGUAGE).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(COMPOSITION.LANGUAGE);
    }

    private Field<?> territory(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(COMPOSITION.TERRITORY).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(COMPOSITION.TERRITORY);
    }

    private Field<?> composerNameValue(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(composerRef.field(PARTY_IDENTIFIED.NAME)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(composerRef.field(PARTY_IDENTIFIED.NAME));
    }

    private Field<?> composerIdNamespace(UUID compositionId, boolean alias, String aliasStr) {
        SelectQuery selectQuery = context.selectQuery();
        if (alias) {
            Field<?> select = DSL.field(composerRef.field(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(composerRef.field(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE));
    }

    private Field<?> composerIdScheme(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(composerRef.field(PARTY_IDENTIFIED.PARTY_REF_SCHEME)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(composerRef.field(PARTY_IDENTIFIED.PARTY_REF_SCHEME));
    }

    private Field<?> composerIdRef(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(composerRef.field(PARTY_IDENTIFIED.PARTY_REF_VALUE)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(composerRef.field(PARTY_IDENTIFIED.PARTY_REF_VALUE));
    }

    private Field<?> composerType(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(composerRef.field(PARTY_IDENTIFIED.PARTY_REF_TYPE)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(composerRef.field(PARTY_IDENTIFIED.PARTY_REF_TYPE));
    }

    private Field<?> tzNoZulu(Field<String> field) {
        return DSL.field(DSL.decode().when(field.equal("UTC"), "Z").otherwise(field));
    }

    private Field<?> prettyDateTime(Field<Timestamp> dateTime, Field<String> timeZone) {
        return DSL.field("to_char(" + dateTimeOffsetTimezone(dateTime, timeZone) + ",'YYYY-MM-DD\"T\"HH24:MI:SS')");
    }

    //sql expression adjusting the date with the timezone. Ignore literals and null timezone
    private Field<?> dateTimeOffsetTimezone(Field<Timestamp> dateTime, Field<String> timeZone) {
        return DSL.field("(" + dateTime + "::timestamptz AT TIME ZONE 'UTC'" +
                " + (case when left(" + timeZone + ",1)='+'" +
                " then \"interval\"(" + timeZone + ")" +
                " else \"interval\"('+00:00')" +
                " end))");
    }

    private Field<?> contextStartTime(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(prettyDateTime(EVENT_CONTEXT.START_TIME, EVENT_CONTEXT.START_TIME_TZID) + "||" + tzNoZulu(EVENT_CONTEXT.START_TIME_TZID))
                    .as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(prettyDateTime(EVENT_CONTEXT.START_TIME, EVENT_CONTEXT.START_TIME_TZID) + "||" + tzNoZulu(EVENT_CONTEXT.START_TIME_TZID));
    }

    private Field<?> contextEndTime(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(prettyDateTime(EVENT_CONTEXT.END_TIME, EVENT_CONTEXT.END_TIME_TZID) + "||" + tzNoZulu(EVENT_CONTEXT.END_TIME_TZID))
                    .as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(prettyDateTime(EVENT_CONTEXT.END_TIME, EVENT_CONTEXT.START_TIME_TZID) + "||" + tzNoZulu(EVENT_CONTEXT.END_TIME_TZID));
    }

    private Field<?> contextLocation(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(EVENT_CONTEXT.LOCATION).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(EVENT_CONTEXT.LOCATION);
    }

    private Field<?> contextFacilityName(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(facilityRef.field(PARTY_IDENTIFIED.NAME)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(facilityRef.field(PARTY_IDENTIFIED.NAME));
    }

    private Field<?> templateIdValue(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(ENTRY.TEMPLATE_ID).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(ENTRY.TEMPLATE_ID);
    }

    private Field<?> contextFacilityRef(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(facilityRef.field(PARTY_IDENTIFIED.PARTY_REF_VALUE)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(facilityRef.field(PARTY_IDENTIFIED.PARTY_REF_VALUE));
    }

    private Field<?> contextFacilityScheme(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(facilityRef.field(PARTY_IDENTIFIED.PARTY_REF_SCHEME)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(facilityRef.field(PARTY_IDENTIFIED.PARTY_REF_SCHEME));
    }

    private Field<?> contextFacilityNamespace(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(facilityRef.field(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(facilityRef.field(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE));
    }

    private Field<?> contextFacilityType(UUID compositionId, boolean alias, String aliasStr) {
        if (alias) {
            Field<?> select = DSL.field(facilityRef.field(PARTY_IDENTIFIED.PARTY_REF_TYPE)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(facilityRef.field(PARTY_IDENTIFIED.PARTY_REF_TYPE));
    }

    private Field<?> ehrStatusSubjectIdValue(UUID compositionId, boolean alias, String aliasStr) {
        containsEhrStatus = true;
        if (alias) {
            Field<?> select = DSL.field(subjectRef.field(PARTY_IDENTIFIED.PARTY_REF_VALUE)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(subjectRef.field(PARTY_IDENTIFIED.PARTY_REF_VALUE));
    }

    private Field<?> ehrStatusSubjectNamespace(UUID compositionId, boolean alias, String aliasStr) {
        containsEhrStatus = true;
        if (alias) {
            Field<?> select = DSL.field(subjectRef.field(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE)).as(aliasStr == null ? columnAlias : aliasStr);
            return select;
        } else
            return DSL.field(subjectRef.field(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE));
    }

    private Field<?> ehrIdValue(UUID compositionId, boolean alias, String aliasStr) {
        containsEhrId = true;
        ehrIdAlias = (aliasStr == null ? columnAlias : aliasStr);
        if (useFromEntry()) {
            joinEhr = true;
            if (alias) {
                Field<?> select = DSL.field("{0}", ehrRecordTable.field(EHR_.ID.getName())).as(aliasStr == null ? columnAlias : aliasStr);
                return select;
            } else
                return DSL.field(ehrRecordTable.field(ehrRecordTable.field(EHR_.ID.getName())));
        } else if (!containsEhrStatus()) {
            joinEhr = true;
            if (alias) {
                Field<?> select = DSL.field("{0}", ehrRecordTable.field(EHR_.ID.getName())).as(aliasStr == null ? columnAlias : aliasStr);
                return select;
            } else
                return DSL.field(ehrRecordTable.field(EHR_.ID.getName()));
        } else {
            if (alias) {
                Field<?> select = DSL.field("{0}", ehrRecordTable.field(EHR_.ID.getName())).as(aliasStr == null ? columnAlias : aliasStr);
                return select;
            } else
                return DSL.field(ehrRecordTable.field(EHR_.ID.getName()));
        }
    }

    private Field<?> ehrStatusOtherDetails(I_VariableDefinition variableDefinition, boolean withAlias) {
        containsEhrStatus = true;
        String variablePath = variableDefinition.getPath().substring("ehr_status/other_details".length() + 1);
        Field<?> field = new JsonbEntryQuery(context, introspectCache, pathResolver, entry_root).makeField(JsonbEntryQuery.OTHER_ITEM.OTHER_DETAILS, null, variableDefinition.getAlias(), variablePath, withAlias);
        return field;
    }

    private Field<?> ehrContextOtherContext(I_VariableDefinition variableDefinition, boolean withAlias) {
        containsOtherContext = true;
        String variablePath = variableDefinition.getPath().substring("context/other_context".length() + 1);
        variablePath = variablePath.substring(variablePath.indexOf("]") + 1);
        String otherContextPath = "/" + variableDefinition.getPath().substring(0, variableDefinition.getPath().indexOf("]") + 1);
        Field<?> field = new JsonbEntryQuery(context, introspectCache, pathResolver, entry_root).makeField(JsonbEntryQuery.OTHER_ITEM.OTHER_CONTEXT, null, variableDefinition.getAlias(), variablePath, withAlias);
        return field;
    }

    public boolean containsEhrStatus() {
        return containsEhrStatus;
    }

    public boolean containsEhrId() {
        return containsEhrId;
    }

    public String getEhrIdAlias() {
        return ehrIdAlias;
    }

    @Override
    public boolean isJsonDataBlock() {
        return false;
    }

    @Override
    public boolean isEhrIdFiltered() {
        return ehrIdFiltered;
    }

    @Override
    public boolean isCompositionIdFiltered() {
        return compositionIdFiltered;
    }

    @Override
    public boolean isContainsJqueryPath() {
        return false;
    }

    @Override
    public boolean isUseEntry() {

        return false;
    }

    @Override
    public String getJsonbItemPath() {
        return null;
    }

    public boolean isJoinComposition() {
        return joinComposition;
    }

    public boolean isJoinEventContext() {
        return joinEventContext;
    }

    public boolean isJoinSubject() {
        return joinSubject;
    }

    public boolean isJoinEhr() {
        return joinEhr;
    }

    public boolean isJoinEhrStatus() {
        return joinEhrStatus;
    }

    public boolean isJoinComposer() {
        return joinComposer;
    }

    public boolean isJoinContextFacility() {
        return joinContextFacility;
    }

    public boolean isCompositionIdField() {
        return compositionIdField;
    }

    public Table<PartyIdentifiedRecord> getComposerRef() {
        return composerRef;
    }

    public Table<PartyIdentifiedRecord> getSubjectRef() {
        return subjectRef;
    }

    public Table<PartyIdentifiedRecord> getFacilityRef() {
        return facilityRef;
    }

    /**
     * true if the expression contains path and then use ENTRY as primary from table
     *
     * @return
     */
    public boolean useFromEntry() {
        return pathResolver.hasPathExpression();
    }
}
