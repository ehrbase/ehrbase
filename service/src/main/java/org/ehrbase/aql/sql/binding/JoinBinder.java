/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

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

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.sql.queryimpl.CompositionAttributeQuery;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.tables.records.*;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * Created by christian on 10/31/2016.
 */
@SuppressWarnings({"java:S3776","java:S3740","java:S1452"})
public class JoinBinder implements IJoinBinder {

    public static final String COMPOSITION_JOIN = "composition_join";
    public static final Table<CompositionRecord> compositionRecordTable = COMPOSITION.as(COMPOSITION_JOIN);
    public static final String SYSTEM_JOIN = "system_join";
    public static final Table<SystemRecord> systemRecordTable = SYSTEM.as(SYSTEM_JOIN);
    public static final String STATUS_JOIN = "status_join";
    public static final Table<StatusRecord> statusRecordTable = STATUS.as(STATUS_JOIN);
    public static final String EHR_JOIN = "ehr_join";
    public static final Table<EhrRecord> ehrRecordTable = EHR_.as(EHR_JOIN);
    public static final Table<PartyIdentifiedRecord> composerRef = PARTY_IDENTIFIED.as("composer_ref");
    public static final Table<PartyIdentifiedRecord> subjectRef = PARTY_IDENTIFIED.as("subject_ref");
    public static final Table<PartyIdentifiedRecord> facilityRef = PARTY_IDENTIFIED.as("facility_ref");
    private boolean compositionJoined = false;
    private boolean statusJoined = false;
    private boolean subjectJoin = false;
    private boolean eventContextJoined = false;
    private boolean facilityJoined = false;
    private boolean composerJoined = false;
    private boolean ehrJoined = false;
    private boolean systemJoined = false;

    SelectQuery<?> selectQuery;

    I_DomainAccess domainAccess;

    public JoinBinder(I_DomainAccess domainAccess,  SelectQuery<?> selectQuery) {
        this.domainAccess = domainAccess;
        this.selectQuery = selectQuery;
    }

    /**
     * Warning: JOIN sequence is important!
     *
     * @param compositionAttributeQuery
     */
    public SelectQuery<?> addJoinClause(CompositionAttributeQuery compositionAttributeQuery) {

        if (compositionAttributeQuery == null)
            return selectQuery;

        if (!compositionAttributeQuery.isUseEntry()  && noJoinRequired(compositionAttributeQuery)){
            selectQuery = simpleFromClause(selectQuery, compositionAttributeQuery);
        }
        else {
            if (compositionAttributeQuery.isJoinSubject()) {
                joinSubject(selectQuery, compositionAttributeQuery);
            }
            if (compositionAttributeQuery.isJoinComposition()) {
                joinComposition(selectQuery);
            }
            if (compositionAttributeQuery.isJoinEventContext()) {
                joinEventContext(selectQuery);
            }
            if (compositionAttributeQuery.isJoinContextFacility()) {
                joinContextFacility(selectQuery);
            }
            if (compositionAttributeQuery.isJoinComposer()) {
                joinComposer(selectQuery);
            }
            if (compositionAttributeQuery.isJoinEhr()) {
                joinEhr(selectQuery);
            }
            if (compositionAttributeQuery.isJoinSystem()) {
                joinSystem(selectQuery);
            }
            if (compositionAttributeQuery.isJoinEhrStatus() || compositionAttributeQuery.containsEhrStatus()) {
                joinEhrStatus(selectQuery, compositionAttributeQuery);
            }
        }

        return selectQuery;
    }

    private boolean noJoinRequired(CompositionAttributeQuery compositionAttributeQuery){
        return (compositionAttributeQuery.isJoinComposer() ? 2 : 0) +
                (compositionAttributeQuery.isJoinEventContext() ? 1 : 0) +
                (compositionAttributeQuery.isJoinComposition() ? 2 : 0) +
                (compositionAttributeQuery.isJoinContextFacility() ? 2 : 0) +
                (compositionAttributeQuery.isJoinEhr() ? 1 : 0) +
                (compositionAttributeQuery.isJoinEhrStatus() ? 2 : 0) +
                (compositionAttributeQuery.isJoinSubject() ? 2 : 0) +
                (compositionAttributeQuery.isJoinSystem() ? 1 : 0) == 1;
    }

    private SelectQuery<?> simpleFromClause(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {

        List<Field<?>> selectFields = selectQuery.getSelect();
        SelectQuery<?> selectQuery1 = domainAccess.getContext().selectQuery();
        selectQuery1.addSelect(selectFields);

        if (compositionAttributeQuery.isJoinEhr()){
            selectQuery1.addFrom(EHR_.as(EHR_JOIN));
        }
        else if (compositionAttributeQuery.isJoinComposition()){
            selectQuery1.addFrom(COMPOSITION.as(COMPOSITION_JOIN));
        }
        else if (compositionAttributeQuery.isJoinSystem()){
            selectQuery1.addFrom(SYSTEM.as(SYSTEM_JOIN));
        }
        return selectQuery1;
    }

    private void joinComposition(SelectQuery<?> selectQuery) {
        if (compositionJoined)
            return;
        selectQuery.addJoin(compositionRecordTable, JoinType.RIGHT_OUTER_JOIN, DSL.field(compositionRecordTable.field(COMPOSITION.ID)).eq(ENTRY.COMPOSITION_ID));
        compositionJoined = true;
    }

    private void joinSystem(SelectQuery<?> selectQuery) {
        if (systemJoined)
            return;
        selectQuery.addJoin(systemRecordTable, JoinType.RIGHT_OUTER_JOIN, DSL.field(systemRecordTable.field(SYSTEM.ID)).eq(DSL.field(ehrRecordTable.field(EHR_.SYSTEM_ID.getName(), UUID.class))));
        systemJoined = true;
    }

    private void joinEhrStatus(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {
        if (statusJoined) return;
        if (compositionAttributeQuery.isJoinComposition() || compositionAttributeQuery.useFromEntry()) {
            joinComposition(selectQuery);
            selectQuery.addJoin(statusRecordTable,
                    DSL.field(statusRecordTable.field(STATUS.EHR_ID.getName(), UUID.class))
                            .eq(DSL.field(compositionRecordTable.field(COMPOSITION.EHR_ID.getName(), UUID.class))));
            statusJoined = true;
        } else {//assume it is joined on EHR
            joinEhr(selectQuery);
            selectQuery.addJoin(statusRecordTable,
                    DSL.field(statusRecordTable.field(STATUS.EHR_ID.getName(), UUID.class))
                            .eq(DSL.field(ehrRecordTable.field(EHR_.ID.getName(), UUID.class))));
            statusJoined = true;
        }
    }

    private void joinSubject(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {
        if (subjectJoin) return;
        joinEhrStatus(selectQuery, compositionAttributeQuery);
        Table<PartyIdentifiedRecord> subjectTable = subjectRef;
        selectQuery.addJoin(subjectTable,
                DSL.field(subjectTable.field(PARTY_IDENTIFIED.ID.getName(), UUID.class))
                        .eq(DSL.field(statusRecordTable.field(STATUS.PARTY.getName(), UUID.class))));
        subjectJoin = true;
    }

    private void joinEventContext(SelectQuery<?> selectQuery) {
        if (eventContextJoined) return;
        selectQuery.addJoin(EVENT_CONTEXT, EVENT_CONTEXT.COMPOSITION_ID.eq(ENTRY.COMPOSITION_ID));
        eventContextJoined = true;
    }

    private void joinContextFacility(SelectQuery<?> selectQuery) {
        if (facilityJoined) return;
        joinEventContext(selectQuery);
        Table<PartyIdentifiedRecord> facilityTable = facilityRef;
        selectQuery.addJoin(facilityTable,
                EVENT_CONTEXT.FACILITY
                        .eq(DSL.field(facilityTable.field(PARTY_IDENTIFIED.ID.getName(), UUID.class))));
        facilityJoined = true;
    }

    private void joinComposer(SelectQuery<?> selectQuery) {
        if (composerJoined) return;
        joinComposition(selectQuery);
        Table<PartyIdentifiedRecord> composerTable = composerRef;
        selectQuery.addJoin(composerTable,
                DSL.field(compositionRecordTable.field(COMPOSITION.COMPOSER.getName(), UUID.class))
                        .eq(DSL.field(composerTable.field(PARTY_IDENTIFIED.ID.getName(), UUID.class))));
        composerJoined = true;
    }

    private void joinEhr(SelectQuery<?> selectQuery) {
        if (ehrJoined) return;
        joinComposition(selectQuery);
        selectQuery.addJoin(ehrRecordTable,
                JoinType.RIGHT_OUTER_JOIN,
                DSL.field(ehrRecordTable.field(EHR_.ID.getName(), UUID.class))
                        .eq(DSL.field(compositionRecordTable.field(COMPOSITION.EHR_ID.getName(), UUID.class))));
        ehrJoined = true;
    }

}
