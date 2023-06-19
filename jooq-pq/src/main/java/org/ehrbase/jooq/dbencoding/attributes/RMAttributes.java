/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.dbencoding.attributes;

import static org.ehrbase.jooq.dbencoding.CompositionSerializer.*;

import com.nedap.archie.rm.datavalues.DvText;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.ItemStack;
import org.ehrbase.jooq.dbencoding.NameAsDvText;
import org.ehrbase.jooq.dbencoding.PathMap;
import org.ehrbase.jooq.dbencoding.SerialTree;
import org.ehrbase.jooq.dbencoding.SimpleClassName;

/**
 * populate the attributes for RM Observation. Root class for all attribute mapping
 */
public abstract class RMAttributes {

    protected final WalkerOutputMode tagMode;
    protected final ItemStack itemStack;
    protected Map<String, Object> map;
    protected CompositionSerializer compositionSerializer;

    public RMAttributes(CompositionSerializer compositionSerializer, ItemStack itemStack, Map<String, Object> map) {
        this.tagMode = compositionSerializer.tagMode();
        this.itemStack = itemStack;
        this.map = map;
        this.compositionSerializer = compositionSerializer;
    }

    /**
     * encode a single value for example activity timing
     * @param tag
     * @param value
     */
    protected Map<String, Object> toMap(String tag, Object value, DvText name) {
        Map<String, Object> valuemap;
        // CHC: 160317 make name optional ex: timing
        if (name != null && !map.containsKey(TAG_NAME)) {
            valuemap = PathMap.getInstance();
            map.putAll(new SerialTree(valuemap).insert(null, value, TAG_NAME, new NameAsDvText(name).toMap()));
        }

        // CHC: 160317 make value optional ex. simple name for activity
        if (value != null) {
            valuemap = PathMap.getInstance();
            valuemap = new SerialTree(valuemap).insert(new SimpleClassName(value).toString(), value, TAG_VALUE, value);
            map.put(tag, valuemap);
        }

        return map;
    }
}
