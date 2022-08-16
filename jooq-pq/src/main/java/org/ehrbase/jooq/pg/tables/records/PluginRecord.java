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
import org.ehrbase.jooq.pg.tables.Plugin;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * key value store for plugin sub system
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class PluginRecord extends UpdatableRecordImpl<PluginRecord> implements Record4<UUID, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>ehr.plugin.id</code>.
     */
    public void setId(UUID value) {
        set(0, value);
    }

    /**
     * Getter for <code>ehr.plugin.id</code>.
     */
    public UUID getId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>ehr.plugin.pluginid</code>.
     */
    public void setPluginid(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>ehr.plugin.pluginid</code>.
     */
    public String getPluginid() {
        return (String) get(1);
    }

    /**
     * Setter for <code>ehr.plugin.key</code>.
     */
    public void setKey(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>ehr.plugin.key</code>.
     */
    public String getKey() {
        return (String) get(2);
    }

    /**
     * Setter for <code>ehr.plugin.value</code>.
     */
    public void setValue(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>ehr.plugin.value</code>.
     */
    public String getValue() {
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
    public Row4<UUID, String, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<UUID, String, String, String> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<UUID> field1() {
        return Plugin.PLUGIN.ID;
    }

    @Override
    public Field<String> field2() {
        return Plugin.PLUGIN.PLUGINID;
    }

    @Override
    public Field<String> field3() {
        return Plugin.PLUGIN.KEY;
    }

    @Override
    public Field<String> field4() {
        return Plugin.PLUGIN.VALUE;
    }

    @Override
    public UUID component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getPluginid();
    }

    @Override
    public String component3() {
        return getKey();
    }

    @Override
    public String component4() {
        return getValue();
    }

    @Override
    public UUID value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getPluginid();
    }

    @Override
    public String value3() {
        return getKey();
    }

    @Override
    public String value4() {
        return getValue();
    }

    @Override
    public PluginRecord value1(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public PluginRecord value2(String value) {
        setPluginid(value);
        return this;
    }

    @Override
    public PluginRecord value3(String value) {
        setKey(value);
        return this;
    }

    @Override
    public PluginRecord value4(String value) {
        setValue(value);
        return this;
    }

    @Override
    public PluginRecord values(UUID value1, String value2, String value3, String value4) {
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
     * Create a detached PluginRecord
     */
    public PluginRecord() {
        super(Plugin.PLUGIN);
    }

    /**
     * Create a detached, initialised PluginRecord
     */
    public PluginRecord(UUID id, String pluginid, String key, String value) {
        super(Plugin.PLUGIN);

        setId(id);
        setPluginid(pluginid);
        setKey(key);
        setValue(value);
    }
}
