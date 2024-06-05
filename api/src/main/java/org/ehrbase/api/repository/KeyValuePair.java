/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.api.repository;

import java.util.UUID;

public interface KeyValuePair {
	
    public static KeyValuePair of(String pluginId, String key, String value) {
        return KeyValuePair.of(UUID.randomUUID(), pluginId, key, value);
    }

    public static KeyValuePair of(UUID id, String pluginId, String key, String value) {
        return new KeyValueEntry(id, pluginId, key, value);
    }	

    public UUID getId();

    public String getContext();

    public String getKey();

    public String getValue();
}

class KeyValueEntry implements KeyValuePair {

    private final UUID id;
    private final String context;
    private final String key;
    private final String value;

    KeyValueEntry(UUID id, String pluginId, String key, String value) {
        this.id = id;
        this.context = pluginId;
        this.key = key;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public String getContext() {
        return context;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
