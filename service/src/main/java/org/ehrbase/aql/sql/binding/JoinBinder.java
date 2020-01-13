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

import org.ehrbase.aql.sql.queryImpl.CompositionAttributeQuery;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.jooq.JoinType;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * Created by christian on 10/31/2016.
 */
public class JoinBinder implements I_JoinBinder {

    private boolean isWholeComposition = false;

    private boolean compositionJoined = false;
    private boolean statusJoined = false;
    private boolean subjectJoin = false;
    private boolean eventContextJoined = false;
    private boolean facilityJoined = false;
    private boolean composerJoined = false;
    private boolean ehrJoined = false;
    private boolean systemJoined = false;

    SelectQuery<?> selectQuery;

    public JoinBinder(SelectQuery<?> selectQuery, boolean isWholeComposition) {
        this.selectQuery = selectQuery;
        this.isWholeComposition = isWholeComposition;
    }

    public JoinBinder() {
        this.isWholeComposition = false;
    }

    /**
     * Warning: JOIN sequence is important!
     *
     * @param compositionAttributeQuery
     */
    public SelectQuery<?> addJoinClause(CompositionAttributeQuery compositionAttributeQuery) {
//        if (isWholeComposition)
//            return;
        if (compositionAttributeQuery.isJoinSubject()) {
            joinSubject(selectQuery, compositionAttributeQuery);
        }
        if (compositionAttributeQuery.isJoinComposition()) {
            joinComposition(selectQuery, compositionAttributeQuery);
        }
        if (compositionAttributeQuery.isJoinEventContext()) {
            joinEventContext(selectQuery, compositionAttributeQuery);
        }
        if (compositionAttributeQuery.isJoinContextFacility()) {
            joinContextFacility(selectQuery, compositionAttributeQuery);
        }
        if (compositionAttributeQuery.isJoinComposer()) {
            joinComposer(selectQuery, compositionAttributeQuery);
        }
        if (compositionAttributeQuery.isJoinEhr()) {
            joinEhr(selectQuery, compositionAttributeQuery);
        }
        if (compositionAttributeQuery.isJoinSystem()) {
            joinSystem(selectQuery, compositionAttributeQuery);
        }
        if (compositionAttributeQuery.isJoinEhrStatus() || compositionAttributeQuery.containsEhrStatus()) {
            joinEhrStatus(selectQuery, compositionAttributeQuery);
        }

        return selectQuery;
    }

    private void joinComposition(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {
        if (compositionJoined)
            return;
        selectQuery.addJoin(compositionRecordTable, JoinType.RIGHT_OUTER_JOIN, DSL.field(compositionRecordTable.field(COMPOSITION.ID)).eq(ENTRY.COMPOSITION_ID));
        compositionJoined = true;
    }

    private void joinSystem(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {
        if (systemJoined)
            return;
        selectQuery.addJoin(systemRecordTable, JoinType.RIGHT_OUTER_JOIN, DSL.field(systemRecordTable.field(SYSTEM.ID)).eq(DSL.field(ehrRecordTable.field(EHR_.SYSTEM_ID.getName(), UUID.class))));
        systemJoined = true;
    }

    private void joinEhrStatus(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {
        if (statusJoined) return;
        if (compositionAttributeQuery.isJoinComposition() || compositionAttributeQuery.useFromEntry()) {
            joinComposition(selectQuery, compositionAttributeQuery);
            selectQuery.addJoin(statusRecordTable,
                    DSL.field(statusRecordTable.field(STATUS.EHR_ID.getName(), UUID.class))
                            .eq(DSL.field(compositionRecordTable.field(COMPOSITION.EHR_ID.getName(), UUID.class))));
            statusJoined = true;
        } else {//assume it is joined on EHR
            joinEhr(selectQuery, compositionAttributeQuery);
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

    private void joinEventContext(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {
        if (eventContextJoined) return;
        selectQuery.addJoin(EVENT_CONTEXT, EVENT_CONTEXT.COMPOSITION_ID.eq(ENTRY.COMPOSITION_ID));
        eventContextJoined = true;
    }

    private void joinContextFacility(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {
        if (facilityJoined) return;
        joinEventContext(selectQuery, compositionAttributeQuery);
        Table<PartyIdentifiedRecord> facilityTable = facilityRef;
        selectQuery.addJoin(facilityTable,
                EVENT_CONTEXT.FACILITY
                        .eq(DSL.field(facilityTable.field(PARTY_IDENTIFIED.ID.getName(), UUID.class))));
        facilityJoined = true;
    }

    private void joinComposer(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {
        if (composerJoined) return;
        joinComposition(selectQuery, compositionAttributeQuery);
        Table<PartyIdentifiedRecord> composerTable = composerRef;
        selectQuery.addJoin(composerTable,
                DSL.field(compositionRecordTable.field(COMPOSITION.COMPOSER.getName(), UUID.class))
                        .eq(DSL.field(composerTable.field(PARTY_IDENTIFIED.ID.getName(), UUID.class))));
        composerJoined = true;
    }

    private void joinEhr(SelectQuery<?> selectQuery, CompositionAttributeQuery compositionAttributeQuery) {
        if (ehrJoined) return;
        joinComposition(selectQuery, compositionAttributeQuery);
        selectQuery.addJoin(ehrRecordTable,
                JoinType.RIGHT_OUTER_JOIN,
                DSL.field(ehrRecordTable.field(EHR_.ID.getName(), UUID.class))
                        .eq(DSL.field(compositionRecordTable.field(COMPOSITION.EHR_ID.getName(), UUID.class))));
        ehrJoined = true;
    }

}
