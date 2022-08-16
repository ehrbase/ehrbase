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

import java.util.UUID;
import org.ehrbase.jooq.pg.Ehr;
import org.ehrbase.jooq.pg.Keys;
import org.ehrbase.jooq.pg.tables.records.PluginRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * key value store for plugin sub system
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Plugin extends TableImpl<PluginRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>ehr.plugin</code>
     */
    public static final Plugin PLUGIN = new Plugin();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PluginRecord> getRecordType() {
        return PluginRecord.class;
    }

    /**
     * The column <code>ehr.plugin.id</code>.
     */
    public final TableField<PluginRecord, UUID> ID = createField(
            DSL.name("id"),
            SQLDataType.UUID.nullable(false).defaultValue(DSL.field("uuid_generate_v4()", SQLDataType.UUID)),
            this,
            "");

    /**
     * The column <code>ehr.plugin.pluginid</code>.
     */
    public final TableField<PluginRecord, String> PLUGINID =
            createField(DSL.name("pluginid"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>ehr.plugin.key</code>.
     */
    public final TableField<PluginRecord, String> KEY =
            createField(DSL.name("key"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>ehr.plugin.value</code>.
     */
    public final TableField<PluginRecord, String> VALUE = createField(DSL.name("value"), SQLDataType.CLOB, this, "");

    private Plugin(Name alias, Table<PluginRecord> aliased) {
        this(alias, aliased, null);
    }

    private Plugin(Name alias, Table<PluginRecord> aliased, Field<?>[] parameters) {
        super(
                alias,
                null,
                aliased,
                parameters,
                DSL.comment("key value store for plugin sub system"),
                TableOptions.table());
    }

    /**
     * Create an aliased <code>ehr.plugin</code> table reference
     */
    public Plugin(String alias) {
        this(DSL.name(alias), PLUGIN);
    }

    /**
     * Create an aliased <code>ehr.plugin</code> table reference
     */
    public Plugin(Name alias) {
        this(alias, PLUGIN);
    }

    /**
     * Create a <code>ehr.plugin</code> table reference
     */
    public Plugin() {
        this(DSL.name("plugin"), null);
    }

    public <O extends Record> Plugin(Table<O> child, ForeignKey<O, PluginRecord> key) {
        super(child, key, PLUGIN);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Ehr.EHR;
    }

    @Override
    public UniqueKey<PluginRecord> getPrimaryKey() {
        return Keys.PLUGIN_PKEY;
    }

    @Override
    public Plugin as(String alias) {
        return new Plugin(DSL.name(alias), this);
    }

    @Override
    public Plugin as(Name alias) {
        return new Plugin(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Plugin rename(String name) {
        return new Plugin(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Plugin rename(Name name) {
        return new Plugin(name, null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<UUID, String, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }
}
