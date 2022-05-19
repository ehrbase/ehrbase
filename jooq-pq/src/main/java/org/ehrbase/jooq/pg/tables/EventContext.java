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
package org.ehrbase.jooq.pg.tables;

import java.sql.Timestamp;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.ehrbase.jooq.binding.SysPeriodBinder;
import org.ehrbase.jooq.pg.Ehr;
import org.ehrbase.jooq.pg.Indexes;
import org.ehrbase.jooq.pg.Keys;
import org.ehrbase.jooq.pg.tables.records.EventContextRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row12;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * defines the context of an event (time, who, where... see openEHR IM 5.2
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class EventContext extends TableImpl<EventContextRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>ehr.event_context</code>
     */
    public static final EventContext EVENT_CONTEXT = new EventContext();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<EventContextRecord> getRecordType() {
        return EventContextRecord.class;
    }

    /**
     * The column <code>ehr.event_context.id</code>.
     */
    public final TableField<EventContextRecord, UUID> ID = createField(
            DSL.name("id"),
            SQLDataType.UUID.nullable(false).defaultValue(DSL.field("uuid_generate_v4()", SQLDataType.UUID)),
            this,
            "");

    /**
     * The column <code>ehr.event_context.composition_id</code>.
     */
    public final TableField<EventContextRecord, UUID> COMPOSITION_ID =
            createField(DSL.name("composition_id"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.event_context.start_time</code>.
     */
    public final TableField<EventContextRecord, Timestamp> START_TIME =
            createField(DSL.name("start_time"), SQLDataType.TIMESTAMP(6).nullable(false), this, "");

    /**
     * The column <code>ehr.event_context.start_time_tzid</code>.
     */
    public final TableField<EventContextRecord, String> START_TIME_TZID =
            createField(DSL.name("start_time_tzid"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>ehr.event_context.end_time</code>.
     */
    public final TableField<EventContextRecord, Timestamp> END_TIME =
            createField(DSL.name("end_time"), SQLDataType.TIMESTAMP(6), this, "");

    /**
     * The column <code>ehr.event_context.end_time_tzid</code>.
     */
    public final TableField<EventContextRecord, String> END_TIME_TZID =
            createField(DSL.name("end_time_tzid"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>ehr.event_context.facility</code>.
     */
    public final TableField<EventContextRecord, UUID> FACILITY =
            createField(DSL.name("facility"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.event_context.location</code>.
     */
    public final TableField<EventContextRecord, String> LOCATION =
            createField(DSL.name("location"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>ehr.event_context.other_context</code>.
     */
    public final TableField<EventContextRecord, JSONB> OTHER_CONTEXT =
            createField(DSL.name("other_context"), SQLDataType.JSONB, this, "");

    /**
     * The column <code>ehr.event_context.setting</code>.
     */
    public final TableField<EventContextRecord, DvCodedTextRecord> SETTING =
            createField(DSL.name("setting"), org.ehrbase.jooq.pg.udt.DvCodedText.DV_CODED_TEXT.getDataType(), this, "");

    /**
     * The column <code>ehr.event_context.sys_transaction</code>.
     */
    public final TableField<EventContextRecord, Timestamp> SYS_TRANSACTION =
            createField(DSL.name("sys_transaction"), SQLDataType.TIMESTAMP(6).nullable(false), this, "");

    /**
     * The column <code>ehr.event_context.sys_period</code>.
     */
    public final TableField<EventContextRecord, SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>>
            SYS_PERIOD = createField(
                    DSL.name("sys_period"),
                    org.jooq.impl.DefaultDataType.getDefaultDataType("\"pg_catalog\".\"tstzrange\"")
                            .nullable(false),
                    this,
                    "",
                    new SysPeriodBinder());

    private EventContext(Name alias, Table<EventContextRecord> aliased) {
        this(alias, aliased, null);
    }

    private EventContext(Name alias, Table<EventContextRecord> aliased, Field<?>[] parameters) {
        super(
                alias,
                null,
                aliased,
                parameters,
                DSL.comment("defines the context of an event (time, who, where... see openEHR IM 5.2"),
                TableOptions.table());
    }

    /**
     * Create an aliased <code>ehr.event_context</code> table reference
     */
    public EventContext(String alias) {
        this(DSL.name(alias), EVENT_CONTEXT);
    }

    /**
     * Create an aliased <code>ehr.event_context</code> table reference
     */
    public EventContext(Name alias) {
        this(alias, EVENT_CONTEXT);
    }

    /**
     * Create a <code>ehr.event_context</code> table reference
     */
    public EventContext() {
        this(DSL.name("event_context"), null);
    }

    public <O extends Record> EventContext(Table<O> child, ForeignKey<O, EventContextRecord> key) {
        super(child, key, EVENT_CONTEXT);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Ehr.EHR;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.CONTEXT_COMPOSITION_ID_IDX, Indexes.CONTEXT_FACILITY_IDX);
    }

    @Override
    public UniqueKey<EventContextRecord> getPrimaryKey() {
        return Keys.EVENT_CONTEXT_PKEY;
    }

    @Override
    public List<ForeignKey<EventContextRecord, ?>> getReferences() {
        return Arrays.asList(
                Keys.EVENT_CONTEXT__EVENT_CONTEXT_COMPOSITION_ID_FKEY, Keys.EVENT_CONTEXT__EVENT_CONTEXT_FACILITY_FKEY);
    }

    private transient Composition _composition;
    private transient PartyIdentified _partyIdentified;

    /**
     * Get the implicit join path to the <code>ehr.composition</code> table.
     */
    public Composition composition() {
        if (_composition == null)
            _composition = new Composition(this, Keys.EVENT_CONTEXT__EVENT_CONTEXT_COMPOSITION_ID_FKEY);

        return _composition;
    }

    /**
     * Get the implicit join path to the <code>ehr.party_identified</code>
     * table.
     */
    public PartyIdentified partyIdentified() {
        if (_partyIdentified == null)
            _partyIdentified = new PartyIdentified(this, Keys.EVENT_CONTEXT__EVENT_CONTEXT_FACILITY_FKEY);

        return _partyIdentified;
    }

    @Override
    public EventContext as(String alias) {
        return new EventContext(DSL.name(alias), this);
    }

    @Override
    public EventContext as(Name alias) {
        return new EventContext(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public EventContext rename(String name) {
        return new EventContext(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public EventContext rename(Name name) {
        return new EventContext(name, null);
    }

    // -------------------------------------------------------------------------
    // Row12 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row12<
                    UUID,
                    UUID,
                    Timestamp,
                    String,
                    Timestamp,
                    String,
                    UUID,
                    String,
                    JSONB,
                    DvCodedTextRecord,
                    Timestamp,
                    SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>>
            fieldsRow() {
        return (Row12) super.fieldsRow();
    }
}
