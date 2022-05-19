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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.ehrbase.jooq.pg.Ehr;
import org.ehrbase.jooq.pg.Indexes;
import org.ehrbase.jooq.pg.Keys;
import org.ehrbase.jooq.pg.tables.records.ConceptRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
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
 * openEHR common concepts (e.g. terminology) used in the system
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Concept extends TableImpl<ConceptRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>ehr.concept</code>
     */
    public static final Concept CONCEPT = new Concept();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ConceptRecord> getRecordType() {
        return ConceptRecord.class;
    }

    /**
     * The column <code>ehr.concept.id</code>.
     */
    public final TableField<ConceptRecord, UUID> ID = createField(
            DSL.name("id"),
            SQLDataType.UUID.nullable(false).defaultValue(DSL.field("uuid_generate_v4()", SQLDataType.UUID)),
            this,
            "");

    /**
     * The column <code>ehr.concept.conceptid</code>.
     */
    public final TableField<ConceptRecord, Integer> CONCEPTID =
            createField(DSL.name("conceptid"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>ehr.concept.language</code>.
     */
    public final TableField<ConceptRecord, String> LANGUAGE =
            createField(DSL.name("language"), SQLDataType.VARCHAR(5), this, "");

    /**
     * The column <code>ehr.concept.description</code>.
     */
    public final TableField<ConceptRecord, String> DESCRIPTION =
            createField(DSL.name("description"), SQLDataType.CLOB, this, "");

    private Concept(Name alias, Table<ConceptRecord> aliased) {
        this(alias, aliased, null);
    }

    private Concept(Name alias, Table<ConceptRecord> aliased, Field<?>[] parameters) {
        super(
                alias,
                null,
                aliased,
                parameters,
                DSL.comment("openEHR common concepts (e.g. terminology) used in the system"),
                TableOptions.table());
    }

    /**
     * Create an aliased <code>ehr.concept</code> table reference
     */
    public Concept(String alias) {
        this(DSL.name(alias), CONCEPT);
    }

    /**
     * Create an aliased <code>ehr.concept</code> table reference
     */
    public Concept(Name alias) {
        this(alias, CONCEPT);
    }

    /**
     * Create a <code>ehr.concept</code> table reference
     */
    public Concept() {
        this(DSL.name("concept"), null);
    }

    public <O extends Record> Concept(Table<O> child, ForeignKey<O, ConceptRecord> key) {
        super(child, key, CONCEPT);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Ehr.EHR;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.EHR_CONCEPT_ID_LANGUAGE_IDX);
    }

    @Override
    public UniqueKey<ConceptRecord> getPrimaryKey() {
        return Keys.CONCEPT_PKEY;
    }

    @Override
    public List<ForeignKey<ConceptRecord, ?>> getReferences() {
        return Arrays.asList(Keys.CONCEPT__CONCEPT_LANGUAGE_FKEY);
    }

    private transient Language _language;

    /**
     * Get the implicit join path to the <code>ehr.language</code> table.
     */
    public Language language() {
        if (_language == null) _language = new Language(this, Keys.CONCEPT__CONCEPT_LANGUAGE_FKEY);

        return _language;
    }

    @Override
    public Concept as(String alias) {
        return new Concept(DSL.name(alias), this);
    }

    @Override
    public Concept as(Name alias) {
        return new Concept(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Concept rename(String name) {
        return new Concept(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Concept rename(Name name) {
        return new Concept(name, null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<UUID, Integer, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }
}
