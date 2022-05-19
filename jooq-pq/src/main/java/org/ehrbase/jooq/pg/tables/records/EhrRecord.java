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
import java.util.UUID;
import org.ehrbase.jooq.pg.tables.Ehr;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * EHR itself
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class EhrRecord extends UpdatableRecordImpl<EhrRecord>
        implements Record6<UUID, Timestamp, String, UUID, UUID, UUID> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>ehr.ehr.id</code>.
     */
    public void setId(UUID value) {
        set(0, value);
    }

    /**
     * Getter for <code>ehr.ehr.id</code>.
     */
    public UUID getId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>ehr.ehr.date_created</code>.
     */
    public void setDateCreated(Timestamp value) {
        set(1, value);
    }

    /**
     * Getter for <code>ehr.ehr.date_created</code>.
     */
    public Timestamp getDateCreated() {
        return (Timestamp) get(1);
    }

    /**
     * Setter for <code>ehr.ehr.date_created_tzid</code>.
     */
    public void setDateCreatedTzid(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>ehr.ehr.date_created_tzid</code>.
     */
    public String getDateCreatedTzid() {
        return (String) get(2);
    }

    /**
     * Setter for <code>ehr.ehr.access</code>.
     */
    public void setAccess(UUID value) {
        set(3, value);
    }

    /**
     * Getter for <code>ehr.ehr.access</code>.
     */
    public UUID getAccess() {
        return (UUID) get(3);
    }

    /**
     * Setter for <code>ehr.ehr.system_id</code>.
     */
    public void setSystemId(UUID value) {
        set(4, value);
    }

    /**
     * Getter for <code>ehr.ehr.system_id</code>.
     */
    public UUID getSystemId() {
        return (UUID) get(4);
    }

    /**
     * Setter for <code>ehr.ehr.directory</code>.
     */
    public void setDirectory(UUID value) {
        set(5, value);
    }

    /**
     * Getter for <code>ehr.ehr.directory</code>.
     */
    public UUID getDirectory() {
        return (UUID) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UUID> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<UUID, Timestamp, String, UUID, UUID, UUID> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<UUID, Timestamp, String, UUID, UUID, UUID> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<UUID> field1() {
        return Ehr.EHR_.ID;
    }

    @Override
    public Field<Timestamp> field2() {
        return Ehr.EHR_.DATE_CREATED;
    }

    @Override
    public Field<String> field3() {
        return Ehr.EHR_.DATE_CREATED_TZID;
    }

    @Override
    public Field<UUID> field4() {
        return Ehr.EHR_.ACCESS;
    }

    @Override
    public Field<UUID> field5() {
        return Ehr.EHR_.SYSTEM_ID;
    }

    @Override
    public Field<UUID> field6() {
        return Ehr.EHR_.DIRECTORY;
    }

    @Override
    public UUID component1() {
        return getId();
    }

    @Override
    public Timestamp component2() {
        return getDateCreated();
    }

    @Override
    public String component3() {
        return getDateCreatedTzid();
    }

    @Override
    public UUID component4() {
        return getAccess();
    }

    @Override
    public UUID component5() {
        return getSystemId();
    }

    @Override
    public UUID component6() {
        return getDirectory();
    }

    @Override
    public UUID value1() {
        return getId();
    }

    @Override
    public Timestamp value2() {
        return getDateCreated();
    }

    @Override
    public String value3() {
        return getDateCreatedTzid();
    }

    @Override
    public UUID value4() {
        return getAccess();
    }

    @Override
    public UUID value5() {
        return getSystemId();
    }

    @Override
    public UUID value6() {
        return getDirectory();
    }

    @Override
    public EhrRecord value1(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public EhrRecord value2(Timestamp value) {
        setDateCreated(value);
        return this;
    }

    @Override
    public EhrRecord value3(String value) {
        setDateCreatedTzid(value);
        return this;
    }

    @Override
    public EhrRecord value4(UUID value) {
        setAccess(value);
        return this;
    }

    @Override
    public EhrRecord value5(UUID value) {
        setSystemId(value);
        return this;
    }

    @Override
    public EhrRecord value6(UUID value) {
        setDirectory(value);
        return this;
    }

    @Override
    public EhrRecord values(UUID value1, Timestamp value2, String value3, UUID value4, UUID value5, UUID value6) {
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
     * Create a detached EhrRecord
     */
    public EhrRecord() {
        super(Ehr.EHR_);
    }

    /**
     * Create a detached, initialised EhrRecord
     */
    public EhrRecord(
            UUID id, Timestamp dateCreated, String dateCreatedTzid, UUID access, UUID systemId, UUID directory) {
        super(Ehr.EHR_);

        setId(id);
        setDateCreated(dateCreated);
        setDateCreatedTzid(dateCreatedTzid);
        setAccess(access);
        setSystemId(systemId);
        setDirectory(directory);
    }
}
