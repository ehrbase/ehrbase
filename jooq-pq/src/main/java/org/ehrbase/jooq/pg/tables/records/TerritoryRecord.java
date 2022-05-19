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

import org.ehrbase.jooq.pg.tables.Territory;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * ISO 3166-1 countries codeset
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class TerritoryRecord extends UpdatableRecordImpl<TerritoryRecord>
        implements Record4<Integer, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>ehr.territory.code</code>.
     */
    public void setCode(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>ehr.territory.code</code>.
     */
    public Integer getCode() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>ehr.territory.twoletter</code>.
     */
    public void setTwoletter(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>ehr.territory.twoletter</code>.
     */
    public String getTwoletter() {
        return (String) get(1);
    }

    /**
     * Setter for <code>ehr.territory.threeletter</code>.
     */
    public void setThreeletter(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>ehr.territory.threeletter</code>.
     */
    public String getThreeletter() {
        return (String) get(2);
    }

    /**
     * Setter for <code>ehr.territory.text</code>.
     */
    public void setText(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>ehr.territory.text</code>.
     */
    public String getText() {
        return (String) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, String, String, String> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Territory.TERRITORY.CODE;
    }

    @Override
    public Field<String> field2() {
        return Territory.TERRITORY.TWOLETTER;
    }

    @Override
    public Field<String> field3() {
        return Territory.TERRITORY.THREELETTER;
    }

    @Override
    public Field<String> field4() {
        return Territory.TERRITORY.TEXT;
    }

    @Override
    public Integer component1() {
        return getCode();
    }

    @Override
    public String component2() {
        return getTwoletter();
    }

    @Override
    public String component3() {
        return getThreeletter();
    }

    @Override
    public String component4() {
        return getText();
    }

    @Override
    public Integer value1() {
        return getCode();
    }

    @Override
    public String value2() {
        return getTwoletter();
    }

    @Override
    public String value3() {
        return getThreeletter();
    }

    @Override
    public String value4() {
        return getText();
    }

    @Override
    public TerritoryRecord value1(Integer value) {
        setCode(value);
        return this;
    }

    @Override
    public TerritoryRecord value2(String value) {
        setTwoletter(value);
        return this;
    }

    @Override
    public TerritoryRecord value3(String value) {
        setThreeletter(value);
        return this;
    }

    @Override
    public TerritoryRecord value4(String value) {
        setText(value);
        return this;
    }

    @Override
    public TerritoryRecord values(Integer value1, String value2, String value3, String value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TerritoryRecord
     */
    public TerritoryRecord() {
        super(Territory.TERRITORY);
    }

    /**
     * Create a detached, initialised TerritoryRecord
     */
    public TerritoryRecord(Integer code, String twoletter, String threeletter, String text) {
        super(Territory.TERRITORY);

        setCode(code);
        setTwoletter(twoletter);
        setThreeletter(threeletter);
        setText(text);
    }
}
