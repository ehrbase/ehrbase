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
package org.ehrbase.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;

/**
 * Helps with analyzing the structure of a Composition
 */
public final class CompositionCleanup {

    private CompositionCleanup() {
        throw new IllegalStateException();
    }

    public static String cleanup(String compositionStr, boolean removeStructureNodes, boolean removeDataValues)
            throws IOException {
        JsonNode comp = new ObjectMapper().readTree(compositionStr);
        cleanup(comp, removeStructureNodes, removeDataValues);

        StringBuilder sb = new StringBuilder();
        try (var w = new StringBuilderWriter(sb)) {
            new ObjectMapper()
                    .configure(SerializationFeature.INDENT_OUTPUT, true)
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                    .writeValue(w, comp);
        }
        return sb.toString();
    }

    private static void cleanup(JsonNode node, boolean removeStructureNodes, boolean removeDataValues) {
        if (node.isArray()) {
            node.iterator().forEachRemaining(n -> cleanup(n, removeStructureNodes, removeDataValues));

            // flatten simple child locatables
            for (int i = 0; i < node.size(); i++) {
                JsonNode child = node.get(i);
                if (child.size() == 1 && child.has("_class")) {
                    ((ArrayNode) node).set(i, child.get("_class"));
                }
            }

        } else if (node instanceof ObjectNode oNode) {

            // remove archetype_details, keep template_id
            if (oNode.has("archetype_details")) {
                JsonNode archetyped = oNode.remove("archetype_details");
                if (archetyped.get("archetype_id").get("value").textValue().startsWith("openEHR-EHR-COMPOSITION")) {
                    oNode.put("template_id", archetyped.get("template_id"));
                }
            }

            {
                Iterator<Map.Entry<String, JsonNode>> it = oNode.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> f = it.next();

                    switch (f.getKey()) {
                        case "language",
                                "territory",
                                "category",
                                "composer",
                                "context",
                                "subject",
                                "rm_version",
                                "encoding",
                                "origin",
                                "time",
                                "math_function",
                                "uid",
                                "width" -> it.remove();
                        case "name", "template_id", "archetype_id" ->
                            f.setValue(f.getValue().get("value"));

                        case "data", "state", "description", "wfDetails" -> {
                            if (removeStructureNodes) {
                                it.remove();
                            } else {
                                cleanup(f.getValue(), removeStructureNodes, removeDataValues);
                            }
                        }
                        case "archetype_details" -> {
                            if (!f.getValue()
                                    .get("archetype_id")
                                    .get("value")
                                    .textValue()
                                    .startsWith("openEHR-EHR-COMPOSITION")) {
                                it.remove();
                                // ((ObjectNode) f.getValue()).remove("template_id");
                            } else {
                                cleanup(f.getValue(), removeStructureNodes, removeDataValues);
                            }
                        }
                        default -> cleanup(f.getValue(), removeStructureNodes, removeDataValues);
                    }
                }
            }

            removeDataValues(oNode, removeDataValues);
            consolidateToClassExp(oNode);
            reorderAttributes(oNode);
        }
    }

    private static void consolidateToClassExp(ObjectNode oNode) {
        if (oNode.has("archetype_node_id")) {
            String type = Optional.of(oNode)
                    .map(n -> n.remove("_type"))
                    .map(t -> t.textValue())
                    .orElse("");
            JsonNode ani = oNode.remove("archetype_node_id");
            JsonNode name = oNode.remove("name");
            StringBuilder sb = new StringBuilder();

            sb.append(type);

            sb.append("[");
            if (ani != null) {
                sb.append(StringUtils.removeStart(StringUtils.removeStart(ani.textValue(), "openEHR-EHR-"), type));
            }
            if (name != null) {
                if (ani != null) {
                    sb.append(",");
                }
                sb.append("'").append(name.textValue()).append("'");
            }
            sb.append("]");
            oNode.put("_class", sb.toString());
        }
    }

    private static void removeDataValues(ObjectNode oNode, boolean removeWholeAttribute) {
        Iterator<Map.Entry<String, JsonNode>> it = oNode.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> f = it.next();
            if (Optional.of(f.getValue())
                    .map(n -> n.get("_type"))
                    .map(JsonNode::textValue)
                    .filter(t -> t.startsWith("DV_"))
                    .isPresent()) {
                if (removeWholeAttribute) {
                    it.remove();
                } else {
                    f.setValue(f.getValue().get("_type"));
                }
            }
        }
    }

    private static void reorderAttributes(ObjectNode oNode) {
        // reorder children
        LinkedHashMap<String, JsonNode> fieldOrder = Stream.of("_class", "archetype_node_id", "name", "_type")
                .filter(oNode::has)
                .collect(Collectors.toMap(k -> k, oNode::remove, (a, b) -> null, LinkedHashMap::new));

        {
            Iterator<Map.Entry<String, JsonNode>> it = oNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (!e.getValue().isContainerNode()) {
                    fieldOrder.put(e.getKey(), e.getValue());
                    it.remove();
                }
            }
        }
        {
            Iterator<Map.Entry<String, JsonNode>> it = oNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (e.getValue().isObject()) {
                    fieldOrder.put(e.getKey(), e.getValue());
                    it.remove();
                }
            }
        }
        {
            Iterator<Map.Entry<String, JsonNode>> it = oNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                fieldOrder.put(e.getKey(), e.getValue());
                it.remove();
            }
        }
        fieldOrder.forEach(oNode::set);
    }
}
