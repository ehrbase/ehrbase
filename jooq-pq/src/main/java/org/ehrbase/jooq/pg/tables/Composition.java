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
import org.ehrbase.jooq.pg.tables.records.CompositionRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row14;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * Composition table
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Composition extends TableImpl<CompositionRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>ehr.composition</code>
     */
    public static final Composition COMPOSITION = new Composition();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<CompositionRecord> getRecordType() {
        return CompositionRecord.class;
    }

    /**
     * The column <code>ehr.composition.id</code>.
     */
    public final TableField<CompositionRecord, UUID> ID = createField(
            DSL.name("id"),
            SQLDataType.UUID.nullable(false).defaultValue(DSL.field("uuid_generate_v4()", SQLDataType.UUID)),
            this,
            "");

    /**
     * The column <code>ehr.composition.ehr_id</code>.
     */
    public final TableField<CompositionRecord, UUID> EHR_ID =
            createField(DSL.name("ehr_id"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.composition.in_contribution</code>.
     */
    public final TableField<CompositionRecord, UUID> IN_CONTRIBUTION =
            createField(DSL.name("in_contribution"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.composition.active</code>.
     */
    public final TableField<CompositionRecord, Boolean> ACTIVE = createField(
            DSL.name("active"), SQLDataType.BOOLEAN.defaultValue(DSL.field("true", SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>ehr.composition.is_persistent</code>.
     */
    public final TableField<CompositionRecord, Boolean> IS_PERSISTENT = createField(
            DSL.name("is_persistent"),
            SQLDataType.BOOLEAN.defaultValue(DSL.field("true", SQLDataType.BOOLEAN)),
            this,
            "");

    /**
     * The column <code>ehr.composition.language</code>.
     */
    public final TableField<CompositionRecord, String> LANGUAGE =
            createField(DSL.name("language"), SQLDataType.VARCHAR(5), this, "");

    /**
     * The column <code>ehr.composition.territory</code>.
     */
    public final TableField<CompositionRecord, Integer> TERRITORY =
            createField(DSL.name("territory"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>ehr.composition.composer</code>.
     */
    public final TableField<CompositionRecord, UUID> COMPOSER =
            createField(DSL.name("composer"), SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>ehr.composition.sys_transaction</code>.
     */
    public final TableField<CompositionRecord, Timestamp> SYS_TRANSACTION =
            createField(DSL.name("sys_transaction"), SQLDataType.TIMESTAMP(6).nullable(false), this, "");

    /**
     * The column <code>ehr.composition.sys_period</code>.
     */
    public final TableField<CompositionRecord, SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>>
            SYS_PERIOD = createField(
                    DSL.name("sys_period"),
                    org.jooq.impl.DefaultDataType.getDefaultDataType("\"pg_catalog\".\"tstzrange\"")
                            .nullable(false),
                    this,
                    "",
                    new SysPeriodBinder());

    /**
     * The column <code>ehr.composition.has_audit</code>.
     */
    public final TableField<CompositionRecord, UUID> HAS_AUDIT =
            createField(DSL.name("has_audit"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.composition.attestation_ref</code>.
     */
    public final TableField<CompositionRecord, UUID> ATTESTATION_REF =
            createField(DSL.name("attestation_ref"), SQLDataType.UUID, this, "");

    /**
     * The column <code>ehr.composition.feeder_audit</code>.
     */
    public final TableField<CompositionRecord, JSONB> FEEDER_AUDIT =
            createField(DSL.name("feeder_audit"), SQLDataType.JSONB, this, "");

    /**
     * The column <code>ehr.composition.links</code>.
     */
    public final TableField<CompositionRecord, JSONB> LINKS =
            createField(DSL.name("links"), SQLDataType.JSONB, this, "");

    private Composition(Name alias, Table<CompositionRecord> aliased) {
        this(alias, aliased, null);
    }

    private Composition(Name alias, Table<CompositionRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment("Composition table"), TableOptions.table());
    }

    /**
     * Create an aliased <code>ehr.composition</code> table reference
     */
    public Composition(String alias) {
        this(DSL.name(alias), COMPOSITION);
    }

    /**
     * Create an aliased <code>ehr.composition</code> table reference
     */
    public Composition(Name alias) {
        this(alias, COMPOSITION);
    }

    /**
     * Create a <code>ehr.composition</code> table reference
     */
    public Composition() {
        this(DSL.name("composition"), null);
    }

    public <O extends Record> Composition(Table<O> child, ForeignKey<O, CompositionRecord> key) {
        super(child, key, COMPOSITION);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Ehr.EHR;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.COMPOSITION_COMPOSER_IDX, Indexes.COMPOSITION_EHR_IDX);
    }

    @Override
    public UniqueKey<CompositionRecord> getPrimaryKey() {
        return Keys.COMPOSITION_PKEY;
    }

    @Override
    public List<ForeignKey<CompositionRecord, ?>> getReferences() {
        return Arrays.asList(
                Keys.COMPOSITION__COMPOSITION_EHR_ID_FKEY,
                Keys.COMPOSITION__COMPOSITION_IN_CONTRIBUTION_FKEY,
                Keys.COMPOSITION__COMPOSITION_LANGUAGE_FKEY,
                Keys.COMPOSITION__COMPOSITION_TERRITORY_FKEY,
                Keys.COMPOSITION__COMPOSITION_COMPOSER_FKEY,
                Keys.COMPOSITION__COMPOSITION_HAS_AUDIT_FKEY,
                Keys.COMPOSITION__COMPOSITION_ATTESTATION_REF_FKEY);
    }

    private transient org.ehrbase.jooq.pg.tables.Ehr _ehr;
    private transient Contribution _contribution;
    private transient Language _language;
    private transient Territory _territory;
    private transient PartyIdentified _partyIdentified;
    private transient AuditDetails _auditDetails;
    private transient AttestationRef _attestationRef;

    /**
     * Get the implicit join path to the <code>ehr.ehr</code> table.
     */
    public org.ehrbase.jooq.pg.tables.Ehr ehr() {
        if (_ehr == null) _ehr = new org.ehrbase.jooq.pg.tables.Ehr(this, Keys.COMPOSITION__COMPOSITION_EHR_ID_FKEY);

        return _ehr;
    }

    /**
     * Get the implicit join path to the <code>ehr.contribution</code> table.
     */
    public Contribution contribution() {
        if (_contribution == null)
            _contribution = new Contribution(this, Keys.COMPOSITION__COMPOSITION_IN_CONTRIBUTION_FKEY);

        return _contribution;
    }

    /**
     * Get the implicit join path to the <code>ehr.language</code> table.
     */
    public Language language() {
        if (_language == null) _language = new Language(this, Keys.COMPOSITION__COMPOSITION_LANGUAGE_FKEY);

        return _language;
    }

    /**
     * Get the implicit join path to the <code>ehr.territory</code> table.
     */
    public Territory territory() {
        if (_territory == null) _territory = new Territory(this, Keys.COMPOSITION__COMPOSITION_TERRITORY_FKEY);

        return _territory;
    }

    /**
     * Get the implicit join path to the <code>ehr.party_identified</code>
     * table.
     */
    public PartyIdentified partyIdentified() {
        if (_partyIdentified == null)
            _partyIdentified = new PartyIdentified(this, Keys.COMPOSITION__COMPOSITION_COMPOSER_FKEY);

        return _partyIdentified;
    }

    /**
     * Get the implicit join path to the <code>ehr.audit_details</code> table.
     */
    public AuditDetails auditDetails() {
        if (_auditDetails == null) _auditDetails = new AuditDetails(this, Keys.COMPOSITION__COMPOSITION_HAS_AUDIT_FKEY);

        return _auditDetails;
    }

    /**
     * Get the implicit join path to the <code>ehr.attestation_ref</code> table.
     */
    public AttestationRef attestationRef() {
        if (_attestationRef == null)
            _attestationRef = new AttestationRef(this, Keys.COMPOSITION__COMPOSITION_ATTESTATION_REF_FKEY);

        return _attestationRef;
    }

    @Override
    public Composition as(String alias) {
        return new Composition(DSL.name(alias), this);
    }

    @Override
    public Composition as(Name alias) {
        return new Composition(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Composition rename(String name) {
        return new Composition(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Composition rename(Name name) {
        return new Composition(name, null);
    }

    // -------------------------------------------------------------------------
    // Row14 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row14<
                    UUID,
                    UUID,
                    UUID,
                    Boolean,
                    Boolean,
                    String,
                    Integer,
                    UUID,
                    Timestamp,
                    SimpleEntry<java.time.OffsetDateTime, java.time.OffsetDateTime>,
                    UUID,
                    UUID,
                    JSONB,
                    JSONB>
            fieldsRow() {
        return (Row14) super.fieldsRow();
    }
}
