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

import com.nedap.archie.rm.archetyped.Locatable;
import java.util.Map;
import org.apache.commons.collections.map.MultiValueMap;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.ItemStack;
import org.ehrbase.jooq.dbencoding.NameAsDvText;
import org.ehrbase.jooq.dbencoding.NameInMap;
import org.ehrbase.jooq.dbencoding.RmObjectEncoding;

/**
 * populate the attributes for RM Locatable Most RM object in a Composition inherit from this class
 */
public abstract class LocatableAttributes extends RMAttributes {

    protected LocatableAttributes(
            CompositionSerializer compositionSerializer, ItemStack itemStack, Map<String, Object> map) {
        super(compositionSerializer, itemStack, map);
    }

    /**
     * map Locatable attributes in a queryable (AQL) form, that is the key follows openEHR RM UML
     * conventions lower case, snake_case
     *
     * @param locatable
     * @return
     */
    protected Map<String, Object> toMap(Locatable locatable) {
        // add complementary attributes

        if (locatable.getArchetypeNodeId() != null) {
            map.put(TAG_ARCHETYPE_NODE_ID, locatable.getArchetypeNodeId());
        }
        if (locatable.getArchetypeDetails() != null) {
            map.put(TAG_ARCHETYPE_DETAILS, new RmObjectEncoding(locatable.getArchetypeDetails()).toMap());
        }
        if (locatable.getFeederAudit() != null) {
            map.put(TAG_FEEDER_AUDIT, new FeederAuditAttributes(locatable.getFeederAudit()).toMap());
        }
        if (locatable.getUid() != null) {
            map = toMap(TAG_UID, locatable.getUid(), NO_NAME);
        }
        if (locatable.getLinks() != null && !locatable.getLinks().isEmpty()) {
            map.put(TAG_LINKS, new LinksAttributes(locatable.getLinks()).toMap());
        }
        if (!map.containsKey(TAG_NAME)
                && locatable.getName() != null) { // since name maybe resolved from the archetype node id
            if (map instanceof MultiValueMap) map.put(TAG_NAME, new NameAsDvText(locatable.getName()).toMap());
            else new NameInMap(map, new NameAsDvText(locatable.getName()).toMap()).toMap();
        }

        return map;
    }

    public static boolean isLocatableAttribute(String key) {
        return (key.equals(TAG_ARCHETYPE_NODE_ID)
                || key.equals(TAG_ARCHETYPE_DETAILS)
                || key.equals(TAG_FEEDER_AUDIT)
                || key.equals(TAG_UID)
                || key.equals(TAG_LINKS)
                || key.equals(TAG_NAME));
    }
}
