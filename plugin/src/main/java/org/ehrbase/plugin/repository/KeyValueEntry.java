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
package org.ehrbase.plugin.repository;

import java.util.UUID;
import org.ehrbase.jooq.pg.tables.records.PluginRecord;

public class KeyValueEntry {
    public static KeyValueEntry of(String pluginId, String key, String value) {
        return KeyValueEntry.of(UUID.randomUUID(), pluginId, key, value);
    }

    public static KeyValueEntry of(UUID id, String pluginId, String key, String value) {
        return new KeyValueEntry(id, pluginId, key, value);
    }

    static KeyValueEntry of(PluginRecord rec) {
        return new KeyValueEntry(rec.getId(), rec.getPluginid(), rec.getKey(), rec.getValue());
    }

    private final UUID id;
    private final String pluginId;
    private final String key;
    private final String value;

    private KeyValueEntry(UUID id, String pluginId, String key, String value) {
        this.id = id;
        this.pluginId = pluginId;
        this.key = key;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
