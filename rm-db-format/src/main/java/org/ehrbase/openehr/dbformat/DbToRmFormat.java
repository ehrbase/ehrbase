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
package org.ehrbase.openehr.dbformat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.directory.Folder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.openehr.dbformat.json.RmDbJson;
import org.ehrbase.openehr.sdk.util.CharSequenceHelper;

/**
 * Reconstructs RM objects from the db format JSON.
 * For aliasing see {@link VersionedObjectDataStructure::applyRmAliases}.
 */
public final class DbToRmFormat {

    private DbToRmFormat() {}

    public static final String FOLDER_ITEMS_UUID_ARRAY_ALIAS = "IA";

    public static Object reconstructFromDbFormat(Class<? extends RMObject> rmType, String dbJsonStr) {
        JsonNode jsonNode = parseJson(dbJsonStr);

        return switch (jsonNode.getNodeType()) {
            case JsonNodeType.OBJECT -> reconstructRmObject(rmType, (ObjectNode) jsonNode);
            case JsonNodeType.STRING -> jsonNode.textValue();
            case JsonNodeType.NUMBER -> jsonNode.numberValue();
            case JsonNodeType.BOOLEAN -> jsonNode.booleanValue();
            case JsonNodeType.NULL -> null;
            case JsonNodeType.ARRAY -> throw new IllegalArgumentException("Unexpected JSON root array");
            case JsonNodeType.BINARY, JsonNodeType.MISSING, JsonNodeType.POJO -> throw new IllegalArgumentException(
                    "Unexpected JSON node type %s".formatted(jsonNode.getNodeType()));
        };
    }

    private static JsonNode parseJson(String dbJsonStr) {
        try {
            return RmDbJson.MARSHAL_OM.readTree(dbJsonStr);
        } catch (JsonProcessingException e) {
            throw new InternalServerException(e.getMessage(), e);
        }
    }

    /**
     * Constructs an RM object from the JSON object obtained via
     *
     * <code><pre>
     * SELECT jsonb_object_agg(
     *   substring(d.entity_idx, (char_length(p.entity_idx) + 1)),
     *   d.data)
     * FROM comp_data as d
     * WHERE (
     *   p.vo_id = d.vo_id
     *   AND p.num <= d.num
     *   AND p.num_cap >= d.entity_idx
     * )
     * GROUP BY d.vo_id
     * </pre></code>
     */
    public static <R extends RMObject> R reconstructRmObject(Class<R> rmType, String dbJsonStr) {
        JsonNode jsonNode = DbToRmFormat.parseJson(dbJsonStr);
        if (jsonNode.isObject()) {
            return reconstructRmObject(rmType, (ObjectNode) jsonNode);
        } else {
            throw new IllegalArgumentException("Unexpected JSON root array");
        }
    }

    /**
     * Constructs an RM object from the JSON object obtained via
     *
     * <code><pre>
     * SELECT jsonb_object_agg(
     *   substring(d.entity_idx, (char_length(p.entity_idx) + 1)),
     *   d.data)
     * FROM comp_data as d
     * WHERE (
     *   p.vo_id = d.vo_id
     *   AND p.num <= d.num
     *   AND p.num_cap > d.num
     * )
     * GROUP BY d.vo_id
     * </pre></code>
     */
    public static <R extends RMObject> R reconstructRmObject(Class<R> rmType, ObjectNode jsonObject) {

        ObjectNode dbRoot;
        if (jsonObject.has(RmAttribute.OBJ_TYPE.alias())) {
            // plain object
            dbRoot = jsonObject;

        } else {
            final int rootPathLength = calcRootPathLength(jsonObject);
            Map<DbJsonPath, ObjectNode> entries = new LinkedHashMap<>((jsonObject.size() * 4 / 3) + 1, 0.75f);

            jsonObject
                    .fields()
                    .forEachRemaining(e -> entries.put(
                            remainingPath(rootPathLength, e.getKey()), standardizeObjectNode(e.getValue())));

            dbRoot = entries.remove(DbJsonPath.EMPTY_PATH);

            entries.forEach((k, v) -> insertJsonEntry(dbRoot, k, v));
        }

        ObjectNode decoded = decodeKeys(dbRoot);

        R rmObject = RmDbJson.MARSHAL_OM.convertValue(decoded, rmType);

        // prevent empty items array
        if (rmObject instanceof Folder folder && folder.getItems().isEmpty()) {
            folder.setItems(null);
        }

        return rmObject;
    }

    private static int calcRootPathLength(ObjectNode jsonObject) {
        Iterator<String> it = jsonObject.fieldNames();
        int l = it.next().length();
        while (it.hasNext() && l != 0) {
            l = Math.min(l, it.next().length());
        }
        return l;
    }

    private static ObjectNode standardizeObjectNode(JsonNode node) {
        ObjectNode objectNode = (ObjectNode) node;

        // revert folder items magic; folderItemsNode is reused
        ArrayNode folderItemsNode = (ArrayNode) objectNode.remove(FOLDER_ITEMS_UUID_ARRAY_ALIAS);
        if (folderItemsNode != null) {
            int folderSize = folderItemsNode.size();
            if (folderSize != 0) {
                for (int i = 0; i < folderSize; i++) {
                    restoreFolderItemObject(folderItemsNode, i);
                }
                objectNode.set(RmAttributeAlias.getAlias("items"), folderItemsNode);
            }
        }

        return objectNode;
    }

    private static void restoreFolderItemObject(ArrayNode folderItemsNode, int idx) {
        JsonNode srcNode = folderItemsNode.get(idx);

        ObjectNode dstNode = folderItemsNode.objectNode();
        dstNode.put(RmAttributeAlias.getAlias("namespace"), "local");
        dstNode.put(RmAttributeAlias.getAlias("type"), "VERSIONED_COMPOSITION");
        ObjectNode idNode = dstNode.putObject(RmAttributeAlias.getAlias("id"));
        idNode.put(TYPE_ALIAS, RmTypeAlias.getAlias("HIER_OBJECT_ID"));
        idNode.set(RmAttributeAlias.getAlias("value"), srcNode);

        folderItemsNode.set(idx, dstNode);
    }

    static DbJsonPath remainingPath(int prefixLen, String fullPathStr) {
        int pos;
        if (fullPathStr.length() > prefixLen && fullPathStr.charAt(prefixLen) == '.') {
            pos = prefixLen + 1;
        } else {
            pos = prefixLen;
        }
        if (pos == 0) {
            return DbJsonPath.parse(fullPathStr);
        } else {
            return DbJsonPath.parse(CharSequenceHelper.subSequence(fullPathStr, pos));
        }
    }

    private static void insertJsonEntry(ObjectNode dbRoot, DbJsonPath path, ObjectNode v) {
        // create target path
        ObjectNode parentObject = dbRoot;

        PathComponent leaf = path.components.get(path.components.size() - 1);

        for (var p : path.components) {
            boolean isLeaf = p == leaf;
            ObjectNode child = v;
            String fieldName = p.attribute();
            if (p.index() == null) {
                JsonNode ch = parentObject.get(fieldName);
                ObjectNode existing = (ObjectNode) ch;
                if (existing != null) {
                    if (isLeaf) {
                        throw new IllegalArgumentException(
                                "parent already has child %s (%s)".formatted(fieldName, path));
                    }
                    parentObject = (ObjectNode) parentObject.get(fieldName);
                } else if (!isLeaf) {
                    throw new IllegalArgumentException("missing ancestor %s (%s)".formatted(fieldName, path));
                } else {
                    parentObject.set(fieldName, child);
                    parentObject = child;
                }

            } else {
                ArrayNode arrayNode = (ArrayNode) parentObject.get(fieldName);
                if (arrayNode == null) {
                    arrayNode = parentObject.arrayNode();
                    parentObject.set(fieldName, arrayNode);
                }
                if (arrayNode.size() <= p.index()) {
                    while (p.index() > arrayNode.size()) {
                        arrayNode.add((JsonNode) null);
                    }
                    parentObject = null;
                    arrayNode.add(child);
                } else if (arrayNode.get(p.index()).isNull()) {
                    parentObject = null;
                    arrayNode.set(p.index(), child);
                } else {
                    parentObject = (ObjectNode) arrayNode.get(p.index());
                }
            }
        }
    }

    private static ObjectNode decodeKeys(ObjectNode dbJson) {
        if (dbJson.has(RmAttribute.OBJ_TYPE.alias())) {
            revertNodeAliasesInPlace(dbJson);
        } else {
            dbJson.forEach(DbToRmFormat::revertNodeAliasesInPlace);
        }
        return dbJson;
    }

    private static void revertNodeAliasesInPlace(JsonNode dbJson) {
        switch (dbJson.getNodeType()) {
            case JsonNodeType.OBJECT -> {
                ObjectNode dbObject = (ObjectNode) dbJson;

                List<Map.Entry<String, JsonNode>> nodes = new ArrayList<>(dbObject.size());
                dbObject.fields().forEachRemaining(nodes::add);
                dbObject.removeAll();

                // replace attribute aliases
                for (Map.Entry<String, JsonNode> property : nodes) {
                    String alias = property.getKey();
                    String attribute = RmAttribute.getAttribute(alias);
                    JsonNode value;
                    if (RmAttribute.OBJ_TYPE.attribute().equals(attribute)) {
                        // revert type aliases
                        String rmType = RmType.getRmType(property.getValue().textValue());
                        value = TextNode.valueOf(rmType);
                    } else {
                        value = property.getValue();
                        revertNodeAliasesInPlace(value);
                    }
                    dbObject.set(attribute, value);
                }
            }
            case JsonNodeType.ARRAY -> {
                ArrayNode dbArray = (ArrayNode) dbJson;
                dbArray.elements().forEachRemaining(DbToRmFormat::revertNodeAliasesInPlace);
            }
            default -> {
                /*NOOP*/
            }
        }
    }

    record PathComponent(String attribute, Integer index) {}

    /**
     * Path in a JSON object.
     *
     * e.g.
     * <ul>
     * <li> "" -> root</li>
     * <li> "x" context element<li>
     * <li> "c1" 2nd element of content<li>
     * <li> "c1" 2nd element of content<li>
     * <li> "c2.j1.j0" 1st element of items of 2nd element of items of 3rd element of content<li>
     * </ul>
     */
    protected static final class DbJsonPath {

        public static final DbJsonPath EMPTY_PATH = new DbJsonPath("", List.of());

        private final CharSequence path;
        private final List<PathComponent> components;

        private DbJsonPath(CharSequence path, List<PathComponent> components) {
            this.path = path;
            this.components = components;
        }

        public List<PathComponent> components() {
            return components;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DbJsonPath && components.equals(((DbJsonPath) obj).components);
        }

        @Override
        public int hashCode() {
            return components.hashCode();
        }

        @Override
        public String toString() {
            return "DbJsonPath{" + "path=" + path + '}';
        }

        public static DbJsonPath parse(CharSequence path) {
            if (path.isEmpty()) {
                return EMPTY_PATH;

            } else {
                int len = path.length();
                // for short paths: at least one character element + '.'
                int expectedCount = len / 2;
                if (expectedCount > 10) {
                    // For larger paths: calculate exact count
                    expectedCount = StringUtils.countMatches(path, '.');
                }
                List<PathComponent> list = new ArrayList<>(expectedCount);
                StringBuilder sb = new StringBuilder();
                int nr = -1;
                // requires trailing '.'
                for (int i = 0; i < len; i++) {
                    char ch = path.charAt(i);
                    if (ch == '.') {
                        list.add(new PathComponent(sb.toString(), nr < 0 ? null : nr));
                        nr = -1;
                        sb.setLength(0);
                    } else if (Character.isDigit(ch)) {
                        int d = Character.digit(ch, 10);
                        if (nr < 0) {
                            nr = d;
                        } else {
                            nr = 10 * nr + d;
                        }
                    } else {
                        sb.append(ch);
                    }
                }
                return new DbJsonPath(path, Collections.unmodifiableList(list));
            }
        }
    }
}
