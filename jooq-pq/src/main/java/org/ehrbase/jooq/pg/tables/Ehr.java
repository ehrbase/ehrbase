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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.ehrbase.jooq.pg.Indexes;
import org.ehrbase.jooq.pg.Keys;
import org.ehrbase.jooq.pg.tables.records.EhrRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row6;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * EHR itself
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Ehr extends TableImpl<EhrRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>ehr.ehr</code>
     */
    public static final Ehr EHR_ = new Ehr();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<EhrRecord> getRecordType() {
        return EhrRecord.class;
    }

    /**
     * The column <code>ehr.ehr.id</code>.
     */
    public final TableField<EhrRecord, UUID> ID = createField(
            DSL.name("id"),
            SQLDataType.UUID.nullable(false).defaultValue(DSL.field("uuid_generate_v4()", SQLDataType.UUID)),
            this,
            "");

    /**
     * The column <code>ehr.ehr.date_created</code>.
     */
    public final TableField<EhrRecord, Timestamp> DATE_CREATED = createField(
            DSL.name("date_created"),
            SQLDataType.TIMESTAMP(6).defaultValue(DSL.field("CURRENT_DATE", SQLDataType.TIMESTAMP)),
            this,
            "");

    /**
     * The column <code>ehr.ehr.date_created_tzid</code>.
     */
    public final TableField<EhrRecord, String> DATE_CREATED_TZID =
            createField(DSL.name("date_created_tzid"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>ehr.ehr.access</code>.
     */
    public final TableField<EhrRecord, UUID> ACCESS = createField(DSL.name("access"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.ehr.system_id</code>.
     */
    public final TableField<EhrRecord, UUID> SYSTEM_ID = createField(DSL.name("system_id"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.ehr.directory</code>.
     */
    public final TableField<EhrRecord, UUID> DIRECTORY = createField(DSL.name("directory"), SQLDataType.UUID, this, "");

    private Ehr(Name alias, Table<EhrRecord> aliased) {
        this(alias, aliased, null);
    }

    private Ehr(Name alias, Table<EhrRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment("EHR itself"), TableOptions.table());
    }

    /**
     * Create an aliased <code>ehr.ehr</code> table reference
     */
    public Ehr(String alias) {
        this(DSL.name(alias), EHR_);
    }

    /**
     * Create an aliased <code>ehr.ehr</code> table reference
     */
    public Ehr(Name alias) {
        this(alias, EHR_);
    }

    /**
     * Create a <code>ehr.ehr</code> table reference
     */
    public Ehr() {
        this(DSL.name("ehr"), null);
    }

    public <O extends Record> Ehr(Table<O> child, ForeignKey<O, EhrRecord> key) {
        super(child, key, EHR_);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : org.ehrbase.jooq.pg.Ehr.EHR;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.EHR_FOLDER_IDX);
    }

    @Override
    public UniqueKey<EhrRecord> getPrimaryKey() {
        return Keys.EHR_PKEY;
    }

    @Override
    public List<ForeignKey<EhrRecord, ?>> getReferences() {
        return Arrays.asList(Keys.EHR__EHR_ACCESS_FKEY, Keys.EHR__EHR_SYSTEM_ID_FKEY, Keys.EHR__EHR_DIRECTORY_FKEY);
    }

    private transient Access _access;
    private transient System _system;
    private transient Folder _folder;

    /**
     * Get the implicit join path to the <code>ehr.access</code> table.
     */
    public Access access() {
        if (_access == null) _access = new Access(this, Keys.EHR__EHR_ACCESS_FKEY);

        return _access;
    }

    /**
     * Get the implicit join path to the <code>ehr.system</code> table.
     */
    public System system() {
        if (_system == null) _system = new System(this, Keys.EHR__EHR_SYSTEM_ID_FKEY);

        return _system;
    }

    /**
     * Get the implicit join path to the <code>ehr.folder</code> table.
     */
    public Folder folder() {
        if (_folder == null) _folder = new Folder(this, Keys.EHR__EHR_DIRECTORY_FKEY);

        return _folder;
    }

    @Override
    public Ehr as(String alias) {
        return new Ehr(DSL.name(alias), this);
    }

    @Override
    public Ehr as(Name alias) {
        return new Ehr(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Ehr rename(String name) {
        return new Ehr(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Ehr rename(Name name) {
        return new Ehr(name, null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<UUID, Timestamp, String, UUID, UUID, UUID> fieldsRow() {
        return (Row6) super.fieldsRow();
    }
}
