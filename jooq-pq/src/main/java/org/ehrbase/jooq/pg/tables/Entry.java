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
import org.ehrbase.jooq.pg.enums.EntryType;
import org.ehrbase.jooq.pg.tables.records.EntryRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row13;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * this table hold the actual archetyped data values (fromBinder a template)
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Entry extends TableImpl<EntryRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>ehr.entry</code>
     */
    public static final Entry ENTRY = new Entry();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<EntryRecord> getRecordType() {
        return EntryRecord.class;
    }

    /**
     * The column <code>ehr.entry.id</code>.
     */
    public final TableField<EntryRecord, UUID> ID = createField(
            DSL.name("id"),
            SQLDataType.UUID.nullable(false).defaultValue(DSL.field("uuid_generate_v4()", SQLDataType.UUID)),
            this,
            "");

    /**
     * The column <code>ehr.entry.composition_id</code>.
     */
    public final TableField<EntryRecord, UUID> COMPOSITION_ID =
            createField(DSL.name("composition_id"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.entry.sequence</code>.
     */
    public final TableField<EntryRecord, Integer> SEQUENCE =
            createField(DSL.name("sequence"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>ehr.entry.item_type</code>.
     */
    public final TableField<EntryRecord, EntryType> ITEM_TYPE = createField(
            DSL.name("item_type"),
            SQLDataType.VARCHAR.asEnumDataType(org.ehrbase.jooq.pg.enums.EntryType.class),
            this,
            "");

    /**
     * The column <code>ehr.entry.template_id</code>.
     */
    public final TableField<EntryRecord, String> TEMPLATE_ID =
            createField(DSL.name("template_id"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>ehr.entry.template_uuid</code>.
     */
    public final TableField<EntryRecord, UUID> TEMPLATE_UUID =
            createField(DSL.name("template_uuid"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.entry.archetype_id</code>.
     */
    public final TableField<EntryRecord, String> ARCHETYPE_ID =
            createField(DSL.name("archetype_id"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>ehr.entry.category</code>.
     */
    public final TableField<EntryRecord, DvCodedTextRecord> CATEGORY = createField(
            DSL.name("category"), org.ehrbase.jooq.pg.udt.DvCodedText.DV_CODED_TEXT.getDataType(), this, "");

    /**
     * The column <code>ehr.entry.entry</code>.
     */
    public final TableField<EntryRecord, JSONB> ENTRY_ = createField(DSL.name("entry"), SQLDataType.JSONB, this, "");

    /**
     * The column <code>ehr.entry.sys_transaction</code>.
     */
    public final TableField<EntryRecord, Timestamp> SYS_TRANSACTION =
            createField(DSL.name("sys_transaction"), SQLDataType.TIMESTAMP(6).nullable(false), this, "");

    /**
     * The column <code>ehr.entry.sys_period</code>.
     */
    public final TableField<EntryRecord, SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>> SYS_PERIOD =
            createField(
                    DSL.name("sys_period"),
                    org.jooq.impl.DefaultDataType.getDefaultDataType("\"pg_catalog\".\"tstzrange\"")
                            .nullable(false),
                    this,
                    "",
                    new SysPeriodBinder());

    /**
     * The column <code>ehr.entry.rm_version</code>.
     */
    public final TableField<EntryRecord, String> RM_VERSION = createField(
            DSL.name("rm_version"),
            SQLDataType.CLOB.nullable(false).defaultValue(DSL.field("'1.0.4'::text", SQLDataType.CLOB)),
            this,
            "");

    /**
     * The column <code>ehr.entry.name</code>.
     */
    public final TableField<EntryRecord, DvCodedTextRecord> NAME =
            createField(DSL.name("name"), org.ehrbase.jooq.pg.udt.DvCodedText.DV_CODED_TEXT.getDataType(), this, "");

    private Entry(Name alias, Table<EntryRecord> aliased) {
        this(alias, aliased, null);
    }

    private Entry(Name alias, Table<EntryRecord> aliased, Field<?>[] parameters) {
        super(
                alias,
                null,
                aliased,
                parameters,
                DSL.comment("this table hold the actual archetyped data values (fromBinder a template)"),
                TableOptions.table());
    }

    /**
     * Create an aliased <code>ehr.entry</code> table reference
     */
    public Entry(String alias) {
        this(DSL.name(alias), ENTRY);
    }

    /**
     * Create an aliased <code>ehr.entry</code> table reference
     */
    public Entry(Name alias) {
        this(alias, ENTRY);
    }

    /**
     * Create a <code>ehr.entry</code> table reference
     */
    public Entry() {
        this(DSL.name("entry"), null);
    }

    public <O extends Record> Entry(Table<O> child, ForeignKey<O, EntryRecord> key) {
        super(child, key, ENTRY);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Ehr.EHR;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.GIN_ENTRY_PATH_IDX, Indexes.TEMPLATE_ENTRY_IDX);
    }

    @Override
    public UniqueKey<EntryRecord> getPrimaryKey() {
        return Keys.ENTRY_PKEY;
    }

    @Override
    public List<UniqueKey<EntryRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.ENTRY_COMPOSITION_ID_KEY);
    }

    @Override
    public List<ForeignKey<EntryRecord, ?>> getReferences() {
        return Arrays.asList(Keys.ENTRY__ENTRY_COMPOSITION_ID_FKEY);
    }

    private transient Composition _composition;

    /**
     * Get the implicit join path to the <code>ehr.composition</code> table.
     */
    public Composition composition() {
        if (_composition == null) _composition = new Composition(this, Keys.ENTRY__ENTRY_COMPOSITION_ID_FKEY);

        return _composition;
    }

    @Override
    public Entry as(String alias) {
        return new Entry(DSL.name(alias), this);
    }

    @Override
    public Entry as(Name alias) {
        return new Entry(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Entry rename(String name) {
        return new Entry(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Entry rename(Name name) {
        return new Entry(name, null);
    }

    // -------------------------------------------------------------------------
    // Row13 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row13<
                    UUID,
                    UUID,
                    Integer,
                    EntryType,
                    String,
                    UUID,
                    String,
                    DvCodedTextRecord,
                    JSONB,
                    Timestamp,
                    SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>,
                    String,
                    DvCodedTextRecord>
            fieldsRow() {
        return (Row13) super.fieldsRow();
    }
}
