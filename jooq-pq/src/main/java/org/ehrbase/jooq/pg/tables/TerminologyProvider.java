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

import org.ehrbase.jooq.pg.Ehr;
import org.ehrbase.jooq.pg.Keys;
import org.ehrbase.jooq.pg.tables.records.TerminologyProviderRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * openEHR identified terminology provider
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class TerminologyProvider extends TableImpl<TerminologyProviderRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>ehr.terminology_provider</code>
     */
    public static final TerminologyProvider TERMINOLOGY_PROVIDER = new TerminologyProvider();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TerminologyProviderRecord> getRecordType() {
        return TerminologyProviderRecord.class;
    }

    /**
     * The column <code>ehr.terminology_provider.code</code>.
     */
    public final TableField<TerminologyProviderRecord, String> CODE =
            createField(DSL.name("code"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>ehr.terminology_provider.source</code>.
     */
    public final TableField<TerminologyProviderRecord, String> SOURCE =
            createField(DSL.name("source"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>ehr.terminology_provider.authority</code>.
     */
    public final TableField<TerminologyProviderRecord, String> AUTHORITY =
            createField(DSL.name("authority"), SQLDataType.CLOB, this, "");

    private TerminologyProvider(Name alias, Table<TerminologyProviderRecord> aliased) {
        this(alias, aliased, null);
    }

    private TerminologyProvider(Name alias, Table<TerminologyProviderRecord> aliased, Field<?>[] parameters) {
        super(
                alias,
                null,
                aliased,
                parameters,
                DSL.comment("openEHR identified terminology provider"),
                TableOptions.table());
    }

    /**
     * Create an aliased <code>ehr.terminology_provider</code> table reference
     */
    public TerminologyProvider(String alias) {
        this(DSL.name(alias), TERMINOLOGY_PROVIDER);
    }

    /**
     * Create an aliased <code>ehr.terminology_provider</code> table reference
     */
    public TerminologyProvider(Name alias) {
        this(alias, TERMINOLOGY_PROVIDER);
    }

    /**
     * Create a <code>ehr.terminology_provider</code> table reference
     */
    public TerminologyProvider() {
        this(DSL.name("terminology_provider"), null);
    }

    public <O extends Record> TerminologyProvider(Table<O> child, ForeignKey<O, TerminologyProviderRecord> key) {
        super(child, key, TERMINOLOGY_PROVIDER);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Ehr.EHR;
    }

    @Override
    public UniqueKey<TerminologyProviderRecord> getPrimaryKey() {
        return Keys.TERMINOLOGY_PROVIDER_PKEY;
    }

    @Override
    public TerminologyProvider as(String alias) {
        return new TerminologyProvider(DSL.name(alias), this);
    }

    @Override
    public TerminologyProvider as(Name alias) {
        return new TerminologyProvider(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public TerminologyProvider rename(String name) {
        return new TerminologyProvider(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public TerminologyProvider rename(Name name) {
        return new TerminologyProvider(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<String, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
