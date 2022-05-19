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

import org.ehrbase.jooq.pg.tables.TerminologyProvider;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * openEHR identified terminology provider
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class TerminologyProviderRecord extends UpdatableRecordImpl<TerminologyProviderRecord>
        implements Record3<String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>ehr.terminology_provider.code</code>.
     */
    public void setCode(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>ehr.terminology_provider.code</code>.
     */
    public String getCode() {
        return (String) get(0);
    }

    /**
     * Setter for <code>ehr.terminology_provider.source</code>.
     */
    public void setSource(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>ehr.terminology_provider.source</code>.
     */
    public String getSource() {
        return (String) get(1);
    }

    /**
     * Setter for <code>ehr.terminology_provider.authority</code>.
     */
    public void setAuthority(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>ehr.terminology_provider.authority</code>.
     */
    public String getAuthority() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<String, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<String, String, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return TerminologyProvider.TERMINOLOGY_PROVIDER.CODE;
    }

    @Override
    public Field<String> field2() {
        return TerminologyProvider.TERMINOLOGY_PROVIDER.SOURCE;
    }

    @Override
    public Field<String> field3() {
        return TerminologyProvider.TERMINOLOGY_PROVIDER.AUTHORITY;
    }

    @Override
    public String component1() {
        return getCode();
    }

    @Override
    public String component2() {
        return getSource();
    }

    @Override
    public String component3() {
        return getAuthority();
    }

    @Override
    public String value1() {
        return getCode();
    }

    @Override
    public String value2() {
        return getSource();
    }

    @Override
    public String value3() {
        return getAuthority();
    }

    @Override
    public TerminologyProviderRecord value1(String value) {
        setCode(value);
        return this;
    }

    @Override
    public TerminologyProviderRecord value2(String value) {
        setSource(value);
        return this;
    }

    @Override
    public TerminologyProviderRecord value3(String value) {
        setAuthority(value);
        return this;
    }

    @Override
    public TerminologyProviderRecord values(String value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TerminologyProviderRecord
     */
    public TerminologyProviderRecord() {
        super(TerminologyProvider.TERMINOLOGY_PROVIDER);
    }

    /**
     * Create a detached, initialised TerminologyProviderRecord
     */
    public TerminologyProviderRecord(String code, String source, String authority) {
        super(TerminologyProvider.TERMINOLOGY_PROVIDER);

        setCode(code);
        setSource(source);
        setAuthority(authority);
    }
}
