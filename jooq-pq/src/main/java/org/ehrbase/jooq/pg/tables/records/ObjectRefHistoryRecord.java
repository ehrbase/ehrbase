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
package org.ehrbase.jooq.pg.tables.records;

import java.sql.Timestamp;
import java.util.AbstractMap.SimpleEntry;
import java.util.UUID;
import org.ehrbase.jooq.pg.tables.ObjectRefHistory;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * *implements
 * https://specifications.openehr.org/releases/RM/Release-1.0.3/support.html#_object_ref_history_class*id
 * implemented as native UID from postgres instead of a separate table.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class ObjectRefHistoryRecord extends UpdatableRecordImpl<ObjectRefHistoryRecord>
        implements Record6<
                String,
                String,
                UUID,
                UUID,
                Timestamp,
                SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>ehr.object_ref_history.id_namespace</code>.
     */
    public void setIdNamespace(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>ehr.object_ref_history.id_namespace</code>.
     */
    public String getIdNamespace() {
        return (String) get(0);
    }

    /**
     * Setter for <code>ehr.object_ref_history.type</code>.
     */
    public void setType(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>ehr.object_ref_history.type</code>.
     */
    public String getType() {
        return (String) get(1);
    }

    /**
     * Setter for <code>ehr.object_ref_history.id</code>.
     */
    public void setId(UUID value) {
        set(2, value);
    }

    /**
     * Getter for <code>ehr.object_ref_history.id</code>.
     */
    public UUID getId() {
        return (UUID) get(2);
    }

    /**
     * Setter for <code>ehr.object_ref_history.in_contribution</code>.
     */
    public void setInContribution(UUID value) {
        set(3, value);
    }

    /**
     * Getter for <code>ehr.object_ref_history.in_contribution</code>.
     */
    public UUID getInContribution() {
        return (UUID) get(3);
    }

    /**
     * Setter for <code>ehr.object_ref_history.sys_transaction</code>.
     */
    public void setSysTransaction(Timestamp value) {
        set(4, value);
    }

    /**
     * Getter for <code>ehr.object_ref_history.sys_transaction</code>.
     */
    public Timestamp getSysTransaction() {
        return (Timestamp) get(4);
    }

    /**
     * Setter for <code>ehr.object_ref_history.sys_period</code>.
     */
    public void setSysPeriod(SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime> value) {
        set(5, value);
    }

    /**
     * Getter for <code>ehr.object_ref_history.sys_period</code>.
     */
    public SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime> getSysPeriod() {
        return (SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<UUID, UUID> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<String, String, UUID, UUID, Timestamp, SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>>
            fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<String, String, UUID, UUID, Timestamp, SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>>
            valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return ObjectRefHistory.OBJECT_REF_HISTORY.ID_NAMESPACE;
    }

    @Override
    public Field<String> field2() {
        return ObjectRefHistory.OBJECT_REF_HISTORY.TYPE;
    }

    @Override
    public Field<UUID> field3() {
        return ObjectRefHistory.OBJECT_REF_HISTORY.ID;
    }

    @Override
    public Field<UUID> field4() {
        return ObjectRefHistory.OBJECT_REF_HISTORY.IN_CONTRIBUTION;
    }

    @Override
    public Field<Timestamp> field5() {
        return ObjectRefHistory.OBJECT_REF_HISTORY.SYS_TRANSACTION;
    }

    @Override
    public Field<SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>> field6() {
        return ObjectRefHistory.OBJECT_REF_HISTORY.SYS_PERIOD;
    }

    @Override
    public String component1() {
        return getIdNamespace();
    }

    @Override
    public String component2() {
        return getType();
    }

    @Override
    public UUID component3() {
        return getId();
    }

    @Override
    public UUID component4() {
        return getInContribution();
    }

    @Override
    public Timestamp component5() {
        return getSysTransaction();
    }

    @Override
    public SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime> component6() {
        return getSysPeriod();
    }

    @Override
    public String value1() {
        return getIdNamespace();
    }

    @Override
    public String value2() {
        return getType();
    }

    @Override
    public UUID value3() {
        return getId();
    }

    @Override
    public UUID value4() {
        return getInContribution();
    }

    @Override
    public Timestamp value5() {
        return getSysTransaction();
    }

    @Override
    public SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime> value6() {
        return getSysPeriod();
    }

    @Override
    public ObjectRefHistoryRecord value1(String value) {
        setIdNamespace(value);
        return this;
    }

    @Override
    public ObjectRefHistoryRecord value2(String value) {
        setType(value);
        return this;
    }

    @Override
    public ObjectRefHistoryRecord value3(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public ObjectRefHistoryRecord value4(UUID value) {
        setInContribution(value);
        return this;
    }

    @Override
    public ObjectRefHistoryRecord value5(Timestamp value) {
        setSysTransaction(value);
        return this;
    }

    @Override
    public ObjectRefHistoryRecord value6(SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime> value) {
        setSysPeriod(value);
        return this;
    }

    @Override
    public ObjectRefHistoryRecord values(
            String value1,
            String value2,
            UUID value3,
            UUID value4,
            Timestamp value5,
            SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime> value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ObjectRefHistoryRecord
     */
    public ObjectRefHistoryRecord() {
        super(ObjectRefHistory.OBJECT_REF_HISTORY);
    }

    /**
     * Create a detached, initialised ObjectRefHistoryRecord
     */
    public ObjectRefHistoryRecord(
            String idNamespace,
            String type,
            UUID id,
            UUID inContribution,
            Timestamp sysTransaction,
            SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime> sysPeriod) {
        super(ObjectRefHistory.OBJECT_REF_HISTORY);

        setIdNamespace(idNamespace);
        setType(type);
        setId(id);
        setInContribution(inContribution);
        setSysTransaction(sysTransaction);
        setSysPeriod(sysPeriod);
    }
}
