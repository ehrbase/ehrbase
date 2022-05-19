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

import java.util.UUID;
import org.ehrbase.jooq.pg.tables.Access;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * defines the modality for accessing an com.ethercis.ehr (security strategy
 * implementation)
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class AccessRecord extends UpdatableRecordImpl<AccessRecord> implements Record3<UUID, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>ehr.access.id</code>.
     */
    public void setId(UUID value) {
        set(0, value);
    }

    /**
     * Getter for <code>ehr.access.id</code>.
     */
    public UUID getId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>ehr.access.settings</code>.
     */
    public void setSettings(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>ehr.access.settings</code>.
     */
    public String getSettings() {
        return (String) get(1);
    }

    /**
     * Setter for <code>ehr.access.scheme</code>.
     */
    public void setScheme(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>ehr.access.scheme</code>.
     */
    public String getScheme() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UUID> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<UUID, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<UUID, String, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<UUID> field1() {
        return Access.ACCESS.ID;
    }

    @Override
    public Field<String> field2() {
        return Access.ACCESS.SETTINGS;
    }

    @Override
    public Field<String> field3() {
        return Access.ACCESS.SCHEME;
    }

    @Override
    public UUID component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getSettings();
    }

    @Override
    public String component3() {
        return getScheme();
    }

    @Override
    public UUID value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getSettings();
    }

    @Override
    public String value3() {
        return getScheme();
    }

    @Override
    public AccessRecord value1(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public AccessRecord value2(String value) {
        setSettings(value);
        return this;
    }

    @Override
    public AccessRecord value3(String value) {
        setScheme(value);
        return this;
    }

    @Override
    public AccessRecord values(UUID value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AccessRecord
     */
    public AccessRecord() {
        super(Access.ACCESS);
    }

    /**
     * Create a detached, initialised AccessRecord
     */
    public AccessRecord(UUID id, String settings, String scheme) {
        super(Access.ACCESS);

        setId(id);
        setSettings(settings);
        setScheme(scheme);
    }
}
