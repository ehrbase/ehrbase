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

import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datavalues.DvText;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositeClassName;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.ItemStack;
import org.ehrbase.jooq.dbencoding.PathItem;
import org.ehrbase.jooq.dbencoding.PathMap;
import org.ehrbase.jooq.dbencoding.SerialTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * populate the attributes for RM Elements
 */
public class ElementAttributes extends ItemAttributes {

    private static final String INITIAL_DUMMY_PREFIX = "$*>";

    private boolean allElements = false;
    private Logger log = LoggerFactory.getLogger(ElementAttributes.class.getSimpleName());

    public ElementAttributes(
            CompositionSerializer compositionSerializer, ItemStack itemStack, Map<String, Object> map) {
        super(compositionSerializer, itemStack, map);
    }

    /**
     * map the value or null_flavour of an Element
     * @param element
     * @return
     */
    public Map<String, Object> toMap(Element element) {
        Map<String, Object> ltree = map;

        // to deal with ITEM_SINGLE initial value
        if (element.getName().getValue().startsWith(INITIAL_DUMMY_PREFIX)) {
            if (allElements) { // strip the prefix since it is for an example
                DvText elementName = element.getName();
                elementName.setValue(elementName.getValue().substring(INITIAL_DUMMY_PREFIX.length()));
                element.setName(elementName);
            } else return ltree;
        }

        // contains value or null_flavor
        Map<String, Object> valuemap = buildValueMap(element);
        ltree.put(TAG_VALUE, valuemap);

        return ltree;
    }

    private Map<String, Object> buildValueMap(Element element) {

        Map<String, Object> valueMap = PathMap.getInstance();

        if (element.getValue() != null && !element.getValue().toString().isEmpty()) {
            log.debug(itemStack.pathStackDump() + "=" + element.getValue());

            if (element.getValue() != null && !element.getValue().toString().isEmpty())
                valueMap = new SerialTree(valueMap)
                        .insert(
                                new CompositeClassName(element.getValue()).toString(),
                                element,
                                TAG_VALUE,
                                element.getValue());
        } else if (element.getNullFlavour() != null) {
            valueMap = new SerialTree(valueMap).insert(null, element, TAG_NULL_FLAVOUR, element.getNullFlavour());
        }
        if (element.getNullReason() != null) {
            valueMap = new SerialTree(valueMap).insert(null, element, "/null_reason", element.getNullReason());
        }

        if (element.getFeederAudit() != null) {
            valueMap = new SerialTree(valueMap).insert(null, element, TAG_FEEDER_AUDIT, element.getFeederAudit());
        }
        if (element.getLinks() != null) {
            valueMap = new SerialTree(valueMap).insert(null, element, TAG_LINKS, element.getLinks());
        }

        if (element.getUid() != null) {
            valueMap = new SerialTree(valueMap).insert(null, element, TAG_UID, element.getUid());
        }

        // set path
        valueMap = new PathItem(valueMap, tagMode, itemStack).encode(null);

        // set archetype_node_id
        valueMap.put(TAG_ARCHETYPE_NODE_ID, element.getArchetypeNodeId());
        return valueMap;
    }
}
