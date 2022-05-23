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
package org.ehrbase.aql.sql.binding;

import static org.ehrbase.jooq.pg.Tables.*;

import java.util.List;
import java.util.UUID;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.tables.records.*;
import org.jooq.Field;
import org.jooq.JoinType;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.impl.DSL;

/**
 * Created by christian on 10/31/2016.
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
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

    private final I_DomainAccess domainAccess;
    private final JoinSetup joinSetup;

    public JoinBinder(I_DomainAccess domainAccess, JoinSetup joinSetup) {
        this.domainAccess = domainAccess;
        this.joinSetup = joinSetup;
    }

    /**
     * Warning: JOIN sequence is important!
     *
     * @param  selectQuery
     */
    public SelectQuery<?> addJoinClause(SelectQuery<?> selectQuery) {

        if (joinSetup == null) return selectQuery;

        if (!joinSetup.isUseEntry() && noJoinRequired(joinSetup)) {
            selectQuery = simpleFromClause(selectQuery, joinSetup);
        } else {
            if (joinSetup.isJoinSubject()) {
                joinSubject(selectQuery, joinSetup);
            }
            if (joinSetup.isJoinComposition()) {
                joinComposition(selectQuery);
            }
            if (joinSetup.isJoinEventContext()) {
                joinEventContext(selectQuery);
            }
            if (joinSetup.isJoinContextFacility()) {
                joinContextFacility(selectQuery);
            }
            if (joinSetup.isJoinComposer()) {
                joinComposer(selectQuery);
            }
            if (joinSetup.isJoinEhr()) {
                joinEhr(selectQuery);
            }
            if (joinSetup.isJoinSystem()) {
                joinSystem(selectQuery);
            }
            if (joinSetup.isJoinEhrStatus()) {
                joinEhrStatus(selectQuery, joinSetup);
            }
        }

        return selectQuery;
    }

    private boolean noJoinRequired(JoinSetup joinSetup) {
        return (joinSetup.isJoinComposer() ? 2 : 0)
                        + (joinSetup.isJoinEventContext() ? 1 : 0)
                        + (joinSetup.isJoinComposition() ? 2 : 0)
                        + (joinSetup.isJoinContextFacility() ? 2 : 0)
                        + (joinSetup.isJoinEhr() ? 1 : 0)
                        + (joinSetup.isJoinEhrStatus() ? 2 : 0)
                        + (joinSetup.isJoinSubject() ? 2 : 0)
                        + (joinSetup.isJoinSystem() ? 1 : 0)
                == 1;
    }

    /**
     * identify the initial from table to use (ENTRY or EHR)
     * @return
     */
    public Table initialFrom() {
        if (joinSetup.isUseEntry()) return ENTRY;
        else {
            if (joinSetup.isJoinEhrStatus() || joinSetup.isJoinSubject()) {
                joinSetup.setJoinEhr(false); // since this is the initial table
                return EHR_.as(EHR_JOIN); // we keep the logic re ref table ids
            } else return ENTRY;
        }
    }

    private SelectQuery<?> simpleFromClause(SelectQuery<?> selectQuery, JoinSetup joinSetup) {

        List<Field<?>> selectFields = selectQuery.getSelect();
        SelectQuery<?> selectQuery1 = domainAccess.getContext().selectQuery();
        selectQuery1.addSelect(selectFields);

        if (joinSetup.isJoinEhr()) {
            selectQuery1.addFrom(EHR_.as(EHR_JOIN));
        } else if (joinSetup.isJoinComposition()) {
            selectQuery1.addFrom(COMPOSITION.as(COMPOSITION_JOIN));
        } else if (joinSetup.isJoinSystem()) {
            selectQuery1.addFrom(SYSTEM.as(SYSTEM_JOIN));
        }
        return selectQuery1;
    }

    private void joinComposition(SelectQuery<?> selectQuery) {
        if (compositionJoined) return;
        selectQuery.addJoin(
                compositionRecordTable,
                JoinType.RIGHT_OUTER_JOIN,
                DSL.field(compositionRecordTable.field(COMPOSITION.ID)).eq(ENTRY.COMPOSITION_ID));
        compositionJoined = true;
    }

    private void joinSystem(SelectQuery<?> selectQuery) {
        if (systemJoined) return;
        selectQuery.addJoin(
                systemRecordTable,
                JoinType.JOIN,
                DSL.field(systemRecordTable.field(SYSTEM.ID))
                        .eq(DSL.field(ehrRecordTable.field(EHR_.SYSTEM_ID.getName(), UUID.class))));
        systemJoined = true;
    }

    private void joinEhrStatus(SelectQuery<?> selectQuery, JoinSetup joinSetup) {
        if (statusJoined) return;
        if (joinSetup.isJoinComposition() || joinSetup.isUseEntry()) {
            joinComposition(selectQuery);
            selectQuery.addJoin(
                    statusRecordTable,
                    DSL.field(statusRecordTable.field(STATUS.EHR_ID.getName(), UUID.class))
                            .eq(DSL.field(compositionRecordTable.field(COMPOSITION.EHR_ID.getName(), UUID.class))));
            statusJoined = true;
        } else { // assume it is joined on EHR
            if (joinSetup.isJoinEhr()) joinEhr(selectQuery);
            selectQuery.addJoin(
                    statusRecordTable,
                    DSL.field(statusRecordTable.field(STATUS.EHR_ID.getName(), UUID.class))
                            .eq(DSL.field(ehrRecordTable.field(EHR_.ID.getName(), UUID.class))));
            statusJoined = true;
        }
    }

    private void joinSubject(SelectQuery<?> selectQuery, JoinSetup joinSetup) {
        if (subjectJoin) return;
        joinEhrStatus(selectQuery, joinSetup);
        Table<PartyIdentifiedRecord> subjectTable = subjectRef;
        selectQuery.addJoin(
                subjectTable,
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
        selectQuery.addJoin(
                facilityTable,
                JoinType.LEFT_OUTER_JOIN,
                EVENT_CONTEXT.FACILITY.eq(DSL.field(facilityTable.field(PARTY_IDENTIFIED.ID.getName(), UUID.class))));
        facilityJoined = true;
    }

    private void joinComposer(SelectQuery<?> selectQuery) {
        if (composerJoined) return;
        joinComposition(selectQuery);
        Table<PartyIdentifiedRecord> composerTable = composerRef;
        selectQuery.addJoin(
                composerTable,
                DSL.field(compositionRecordTable.field(COMPOSITION.COMPOSER.getName(), UUID.class))
                        .eq(DSL.field(composerTable.field(PARTY_IDENTIFIED.ID.getName(), UUID.class))));
        composerJoined = true;
    }

    private void joinEhr(SelectQuery<?> selectQuery) {
        if (ehrJoined) return;
        joinComposition(selectQuery);
        selectQuery.addJoin(
                ehrRecordTable,
                JoinType.RIGHT_OUTER_JOIN,
                DSL.field(ehrRecordTable.field(EHR_.ID.getName(), UUID.class))
                        .eq(DSL.field(compositionRecordTable.field(COMPOSITION.EHR_ID.getName(), UUID.class))));
        ehrJoined = true;
    }
}
