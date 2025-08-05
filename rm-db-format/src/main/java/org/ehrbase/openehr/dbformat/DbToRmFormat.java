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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.openehr.dbformat.json.RmDbJson;
import org.ehrbase.openehr.sdk.util.CharSequenceHelper;
import org.jooq.JSONB;
import org.jooq.Record2;

/**
 * Reconstructs RM objects from the db format JSON.
 * For aliasing see {@link VersionedObjectDataStructure::applyRmAliases}.
 */
public final class DbToRmFormat {

    private DbToRmFormat() {}

    public static final String TYPE_ALIAS = "T";

    public static final String TYPE_ATTRIBUTE = "_type";

    public static final String FOLDER_ITEMS_UUID_ARRAY_ALIAS = "IA";

    public static final String FEEDER_AUDIT_ATTRIBUTE_ALIAS = "f";

    public static Object reconstructFromDbFormat(Class<? extends RMObject> rmType, String dbJsonStr) {
        return reconstructFromDbFormat(rmType, parseJson(dbJsonStr));
    }

    public static Object reconstructFromDbFormat(Class<? extends RMObject> rmType, JsonNode jsonNode) {
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
     * Second value needs to be JSONB
     * @param rec
     * @return
     */
    private static JsonNode parseJsonData(Record2<?, ?> rec) {
        JSONB jsonb = ((JSONB) rec.value2());
        return parseJson(jsonb.data());
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
            throw new IllegalArgumentException("Unexpected JSON root type");
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
        if (jsonObject.has(TYPE_ALIAS)) {
            // plain object
            dbRoot = jsonObject;

        } else {
            int childCount = jsonObject.size();
            Map.Entry<String, JsonNode>[] children = new Map.Entry[childCount];
            Iterator<Map.Entry<String, JsonNode>> fieldIt = jsonObject.fields();

            int rootPathLength = calcRootPathLength(childCount, fieldIt, children);
            Arrays.sort(children, Map.Entry.comparingByKey());

            dbRoot = standardizeObjectNode(children[0].getValue());

            for (int i = 1; i < childCount; i++) {
                Map.Entry<String, JsonNode> child = children[i];
                insertJsonEntry(
                        dbRoot, remainingPath(rootPathLength, child.getKey()), standardizeObjectNode(child.getValue()));
            }
        }

        return dbToToRmObject(rmType, dbRoot);
    }

    private static <R extends RMObject> R dbToToRmObject(Class<R> rmType, ObjectNode dbObject) {
        ObjectNode decoded = decodeKeys(dbObject);

        R rmObject = RmDbJson.MARSHAL_OM.convertValue(decoded, rmType);

        // prevent empty items array
        if (rmObject instanceof Folder folder && folder.getItems().isEmpty()) {
            folder.setItems(null);
        }
        return rmObject;
    }

    /**
     * Constructs an RM object from the JSON object obtained via
     *
     * <code><pre>
     * SELECT array_agg(
     *   (substring(d.entity_idx, (char_length(p.entity_idx) + 1)),
     *   d.data))
     * FROM comp_data as d
     * WHERE (
     *   p.vo_id = d.vo_id
     *   AND p.num <= d.num
     *   AND p.num_cap > d.num
     * )
     * GROUP BY d.vo_id
     * </pre></code>
     */
    public static <R extends RMObject> R reconstructRmObject(Class<R> rmType, Record2<?, ?>[] jsonObjects) {

        int childCount = jsonObjects.length;
        // Or Record2<String, JsonNode>[] dbRecords

        int rootPathLength = calcRootPathLength(jsonObjects, childCount);
        Arrays.sort(jsonObjects, Comparator.comparing(r -> r.value1().toString()));

        ObjectNode dbRoot = standardizeObjectNode(parseJsonData(jsonObjects[0]));

        for (int i = 1; i < childCount; i++) {
            Record2<String, JsonNode> child = (Record2<String, JsonNode>) jsonObjects[i];
            insertJsonEntry(
                    dbRoot, remainingPath(rootPathLength, child.value1()), standardizeObjectNode(parseJsonData(child)));
        }

        ObjectNode decoded = decodeKeys(dbRoot);

        R rmObject = RmDbJson.MARSHAL_OM.convertValue(decoded, rmType);

        // prevent empty items array
        if (rmObject instanceof Folder folder
                && folder.getItems() != null
                && folder.getItems().isEmpty()) {
            folder.setItems(null);
        }

        return rmObject;
    }

    private static int calcRootPathLength(
            int childCount, Iterator<Map.Entry<String, JsonNode>> fieldIt, Map.Entry<String, JsonNode>[] children) {
        int l = Integer.MAX_VALUE;
        for (int i = 0; i < childCount; i++) {
            Map.Entry<String, JsonNode> next = fieldIt.next();
            children[i] = next;
            l = Math.min(l, next.getKey().length());
        }
        return l;
    }

    private static int calcRootPathLength(Record2<?, ?>[] jsonObjects, int childCount) {
        int l = Integer.MAX_VALUE;
        for (int i = 0; i < childCount; i++) {
            Record2<?, ?> next = jsonObjects[i];
            l = Math.min(l, next.value1().toString().length());
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
            JsonNode ch = parentObject.get(fieldName);
            if (p.index() == null) {
                ObjectNode existing = (ObjectNode) ch;
                if (isLeaf) {
                    /* In VersionedObjectDataStructure::handleSubObject a copy of FEEDER_AUDIT
                     * is stored in ELEMENT, so we can treat ELEMENT as leaf.
                     * ELEMENT/feeder_audit is replaced because it would be more complicated to skip all possible descendants
                     */
                    if (existing != null && !FEEDER_AUDIT_ATTRIBUTE_ALIAS.equals(fieldName)) {
                        throw new IllegalArgumentException(
                                "parent already has child %s (%s)".formatted(fieldName, path));
                    }
                    parentObject.set(fieldName, child);
                    parentObject = child;
                } else if (existing == null) {
                    throw new IllegalArgumentException("missing ancestor %s (%s)".formatted(fieldName, path));
                } else {
                    parentObject = (ObjectNode) parentObject.get(fieldName);
                }

            } else {
                ArrayNode arrayNode = (ArrayNode) ch;
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
        if (dbJson.has(RmAttributeAlias.getAlias(TYPE_ATTRIBUTE))) {
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
                    String attribute = RmAttributeAlias.getAttribute(alias);
                    JsonNode value;
                    if (TYPE_ATTRIBUTE.equals(attribute)) {
                        // revert type aliases
                        String rmType =
                                RmTypeAlias.getRmType(property.getValue().textValue());
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
    public static final class DbJsonPath {

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
            return parse(path, false);
        }

        public static DbJsonPath parse(CharSequence path, boolean revertAliases) {
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
                        list.add(new PathComponent(revertAliases ? RmAttributeAlias.getAttribute(sb.toString()) : sb.toString(), nr < 0 ? null : nr));
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
