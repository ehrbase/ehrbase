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
import org.ehrbase.jooq.pg.tables.System;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * system table for reference
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class SystemRecord extends UpdatableRecordImpl<SystemRecord> implements Record3<UUID, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>ehr.system.id</code>.
     */
    public void setId(UUID value) {
        set(0, value);
    }

    /**
     * Getter for <code>ehr.system.id</code>.
     */
    public UUID getId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>ehr.system.description</code>.
     */
    public void setDescription(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>ehr.system.description</code>.
     */
    public String getDescription() {
        return (String) get(1);
    }

    /**
     * Setter for <code>ehr.system.settings</code>.
     */
    public void setSettings(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>ehr.system.settings</code>.
     */
    public String getSettings() {
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
        return System.SYSTEM.ID;
    }

    @Override
    public Field<String> field2() {
        return System.SYSTEM.DESCRIPTION;
    }

    @Override
    public Field<String> field3() {
        return System.SYSTEM.SETTINGS;
    }

    @Override
    public UUID component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getDescription();
    }

    @Override
    public String component3() {
        return getSettings();
    }

    @Override
    public UUID value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getDescription();
    }

    @Override
    public String value3() {
        return getSettings();
    }

    @Override
    public SystemRecord value1(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public SystemRecord value2(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public SystemRecord value3(String value) {
        setSettings(value);
        return this;
    }

    @Override
    public SystemRecord values(UUID value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SystemRecord
     */
    public SystemRecord() {
        super(System.SYSTEM);
    }

    /**
     * Create a detached, initialised SystemRecord
     */
    public SystemRecord(UUID id, String description, String settings) {
        super(System.SYSTEM);

        setId(id);
        setDescription(description);
        setSettings(settings);
    }
}
