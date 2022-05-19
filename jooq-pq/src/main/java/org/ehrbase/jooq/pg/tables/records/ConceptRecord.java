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
import org.ehrbase.jooq.pg.tables.Concept;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * openEHR common concepts (e.g. terminology) used in the system
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class ConceptRecord extends UpdatableRecordImpl<ConceptRecord>
        implements Record4<UUID, Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>ehr.concept.id</code>.
     */
    public void setId(UUID value) {
        set(0, value);
    }

    /**
     * Getter for <code>ehr.concept.id</code>.
     */
    public UUID getId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>ehr.concept.conceptid</code>.
     */
    public void setConceptid(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>ehr.concept.conceptid</code>.
     */
    public Integer getConceptid() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>ehr.concept.language</code>.
     */
    public void setLanguage(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>ehr.concept.language</code>.
     */
    public String getLanguage() {
        return (String) get(2);
    }

    /**
     * Setter for <code>ehr.concept.description</code>.
     */
    public void setDescription(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>ehr.concept.description</code>.
     */
    public String getDescription() {
        return (String) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UUID> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<UUID, Integer, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<UUID, Integer, String, String> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<UUID> field1() {
        return Concept.CONCEPT.ID;
    }

    @Override
    public Field<Integer> field2() {
        return Concept.CONCEPT.CONCEPTID;
    }

    @Override
    public Field<String> field3() {
        return Concept.CONCEPT.LANGUAGE;
    }

    @Override
    public Field<String> field4() {
        return Concept.CONCEPT.DESCRIPTION;
    }

    @Override
    public UUID component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getConceptid();
    }

    @Override
    public String component3() {
        return getLanguage();
    }

    @Override
    public String component4() {
        return getDescription();
    }

    @Override
    public UUID value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getConceptid();
    }

    @Override
    public String value3() {
        return getLanguage();
    }

    @Override
    public String value4() {
        return getDescription();
    }

    @Override
    public ConceptRecord value1(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public ConceptRecord value2(Integer value) {
        setConceptid(value);
        return this;
    }

    @Override
    public ConceptRecord value3(String value) {
        setLanguage(value);
        return this;
    }

    @Override
    public ConceptRecord value4(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public ConceptRecord values(UUID value1, Integer value2, String value3, String value4) {
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
     * Create a detached ConceptRecord
     */
    public ConceptRecord() {
        super(Concept.CONCEPT);
    }

    /**
     * Create a detached, initialised ConceptRecord
     */
    public ConceptRecord(UUID id, Integer conceptid, String language, String description) {
        super(Concept.CONCEPT);

        setId(id);
        setConceptid(conceptid);
        setLanguage(language);
        setDescription(description);
    }
}
