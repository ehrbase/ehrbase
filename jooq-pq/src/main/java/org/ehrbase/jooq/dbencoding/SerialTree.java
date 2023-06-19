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
package org.ehrbase.jooq.dbencoding;

import static org.ehrbase.jooq.dbencoding.CompositionSerializer.TAG_CLASS;
import static org.ehrbase.jooq.dbencoding.CompositionSerializer.TAG_NAME;
import static org.ehrbase.jooq.dbencoding.CompositionSerializer.TAG_PATH;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.archetyped.Locatable;
import java.util.Map;
import org.apache.commons.collections4.map.PredicatedMap;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * insert the attributes of a structural object into the DB encoded structure
 */
public class SerialTree {

    Map<String, Object> map;

    Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    public SerialTree(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * put a key=value pair in a map and detects duplicates.
     *
     * @param key
     * @param addStructure
     * @return
     * @throws Exception
     */
    public Map<String, Object> insert(String clazz, Object node, String key, Object addStructure) {
        if (addStructure == null) {
            return map;
        }

        if (addStructure instanceof Map
                && ((Map) addStructure).size() == 0
                && !clazz.equalsIgnoreCase(RmConstants.COMPOSITION)) {
            return map;
        }

        if (key.equals(TAG_NAME)) {
            if (addStructure instanceof Map) {
                return new NameInMap(map, (Map) addStructure).toMap();
            } else {
                throw new IllegalStateException(
                        "INTERNAL: addStructure is not a map, found:" + addStructure.getClass());
            }
        }

        try {
            if (addStructure instanceof RMObject) {
                map.put(key, new RmObjectEncoding((RMObject) addStructure).toMap());
            } else {
                map.put(key, addStructure);
            }
            // add explicit name
            if (node instanceof Locatable && map instanceof PredicatedMap && !map.containsKey(TAG_NAME)) {
                new NameInMap(map, new NameAsDvText(((Locatable) node).getName()).toMap()).toMap();
            }

        } catch (IllegalArgumentException e) {
            log.error("Ignoring duplicate key in path detected: {}", key, e);
        }

        if (clazz != null && !key.equals(TAG_PATH) && !map.containsKey(TAG_CLASS)) {
            map.put(CompositionSerializer.TAG_CLASS, clazz);
        } else {
            log.debug(map.containsKey(TAG_CLASS) ? "duplicate TAG_CLASS" : "null clazz");
        }

        return map;
    }

    public Map<String, Object> insert(Object node, String key, Object addStructure) {

        String clazz = new SimpleClassName(node).toString();

        return insert(clazz, node, key, addStructure);
    }
}
