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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.directory.Folder;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import org.apache.commons.io.input.CharSequenceReader;
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

    private static final Comparator<CharSequence> SIMPLE_CHAR_SEQUENCE_COMPARATOR = (cs1, cs2)  -> {
        if (cs1 instanceof String s1 && cs2 instanceof String s2) {
            return s1.compareTo(s2);
        }

        int len1 = cs1.length();
        int len2 = cs2.length();
        int lim = Math.min(len1, len2);

        for (int k = 0; k < lim; k++) {
            char c1 = cs1.charAt(k);
            char c2 = cs2.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    };

    public static Object reconstructFromDbFormat(Class<? extends RMObject> rmType, String dbJsonStr) {
        return reconstructFromDbFormat(rmType, parseJson(dbJsonStr, RmDbJson.MARSHAL_OM));
    }

    public static Object reconstructFromDbFormat(Class<? extends RMObject> rmType, JsonNode jsonNode) {
        return switch (jsonNode.getNodeType()) {
            case JsonNodeType.OBJECT -> reconstructRmObject(rmType, (ObjectNode) jsonNode);
            case JsonNodeType.STRING -> jsonNode.textValue();
            case JsonNodeType.NUMBER -> jsonNode.numberValue();
            case JsonNodeType.BOOLEAN -> jsonNode.booleanValue();
            case JsonNodeType.NULL -> null;
            case JsonNodeType.ARRAY -> throw new IllegalArgumentException("Unexpected JSON root array");
            case JsonNodeType.BINARY, JsonNodeType.MISSING, JsonNodeType.POJO ->
                throw new IllegalArgumentException("Unexpected JSON node type %s".formatted(jsonNode.getNodeType()));
        };
    }

    private static JsonNode parseJson(String dbJsonStr, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(dbJsonStr);
        } catch (JsonProcessingException e) {
            throw new InternalServerException(e.getMessage(), e);
        }
    }

    private static JsonNode parseJson(Reader dbJsonReader, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(dbJsonReader);
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage(), e);
        }
    }

    /**
     * Second value needs to be JSONB
     * @param rec
     * @return
     */
    private static <T> JsonNode parseJsonData(T rec, Function<T, ?> jsonExtractor, ObjectMapper objectMapper) {
        Object v = jsonExtractor.apply(rec);
        return switch (v) {
            case String s  -> parseJson(s, objectMapper);
            case JSONB j  -> parseJson(j.data(), objectMapper);
            case CharSequence cs  -> parseJson(new CharSequenceReader(cs), objectMapper);
            default -> {
                    throw new IllegalArgumentException("Unexpected JSON data type %s".formatted(v.getClass()));
                    }
            };
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
        JsonNode jsonNode = DbToRmFormat.parseJson(dbJsonStr, RmDbJson.MARSHAL_OM);
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
            Entry<String, JsonNode>[] children = new Entry[childCount];
            Iterator<Entry<String, JsonNode>> fieldIt = jsonObject.fields();

            int rootPathLength = Integer.MAX_VALUE;
            for (int i = 0; i < childCount; i++) {
                Entry<String, JsonNode> next = fieldIt.next();
                children[i] = next;
                rootPathLength = Math.min(rootPathLength, next.getKey().length());
            }
            Arrays.sort(children, Entry.comparingByKey());

            dbRoot = standardizeObjectNode(children[0].getValue());

            for (int i = 1; i < childCount; i++) {
                Entry<String, JsonNode> child = children[i];
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
    public static <R extends RMObject> R reconstructRmObject(Class<R> rmType, Record2<String, ?>[] jsonObjects) {

        ObjectNode decoded = decodeKeys(reconstructRmObjectTree(jsonObjects, RmDbJson.MARSHAL_OM));

        R rmObject = RmDbJson.MARSHAL_OM.convertValue(decoded, rmType);

        // prevent empty items array
        if (rmObject instanceof Folder folder
                && folder.getItems() != null
                && folder.getItems().isEmpty()) {
            folder.setItems(null);
        }

        return rmObject;
    }

    public static ObjectNode reconstructRmObjectTree(final Record2<String, ?>[] jsonObjects, ObjectMapper objectMapper) {
        return reconstructRmObjectTree(jsonObjects, Record2::value1, Record2::value2, objectMapper);
    }

    public static <T> ObjectNode reconstructRmObjectTree(
            final T[] jsonObjects,
            Function<T, ? extends CharSequence> idxExtractor,
            Function<T, Object> jsonExtractor,
            ObjectMapper objectMapper) {
        int childCount = jsonObjects.length;
        // Or Record2<String, JsonNode>[] dbRecords

        Arrays.sort(jsonObjects, Comparator.comparing(idxExtractor, SIMPLE_CHAR_SEQUENCE_COMPARATOR));
        int rootPathLength = calcRootPathLength(jsonObjects, idxExtractor, childCount);

        ObjectNode dbRoot = standardizeObjectNode(parseJsonData(jsonObjects[0], jsonExtractor, objectMapper));

        for (int i = 1; i < childCount; i++) {
            T child = jsonObjects[i];
            insertJsonEntry(
                    dbRoot,
                    remainingPath(rootPathLength, idxExtractor.apply(child)),
                    standardizeObjectNode(parseJsonData(child, jsonExtractor, objectMapper)));
        }
        return dbRoot;
    }

    private static <T> int calcRootPathLength(T[] jsonObjects, Function<T, ?> idxExtractor, int childCount) {
        int l = Integer.MAX_VALUE;
        for (int i = 0; i < childCount && l > 0; i++) {
            T next = jsonObjects[i];
            l = Math.min(l, idxExtractor.apply(next).toString().length());
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

    static DbJsonPath remainingPath(int prefixLen, CharSequence fullPathStr) {
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

    private static void insertJsonEntry(ObjectNode dbRoot, DbJsonPath path, ObjectNode childToAdd) {
        ObjectNode parentObject = dbRoot;

        List<PathComponent> components = path.components;
        int leafIdx = components.size() - 1;
        for (int i = 0; i <= leafIdx; i++) {
            var p = components.get(i);
            if (p.index() == null) {
                parentObject = insertJsonEntryIntoObject(parentObject, p, childToAdd, i == leafIdx, path);

            } else {
                parentObject = insertJsonEntryIntoArray(parentObject, p, childToAdd, i == leafIdx, path);
            }
        }
    }

    private static ObjectNode insertJsonEntryIntoObject(ObjectNode parentObject, PathComponent pc, ObjectNode childToAdd, boolean isLeaf, DbJsonPath path) {
        String fieldName = pc.attribute();
        ObjectNode objectNode = (ObjectNode) parentObject.get(fieldName);
        if (isLeaf) {
            /* In VersionedObjectDataStructure::handleSubObject a copy of FEEDER_AUDIT
             * is stored in ELEMENT, so we can treat ELEMENT as leaf.
             * ELEMENT/feeder_audit is replaced because it would be more complicated to skip all possible descendants
             */
            if (objectNode != null && !FEEDER_AUDIT_ATTRIBUTE_ALIAS.equals(fieldName)) {
                throw new IllegalArgumentException(
                        "parent already has child %s (%s)".formatted(fieldName, path));
            }
            parentObject.set(fieldName, childToAdd);
            return childToAdd;
        } else if (objectNode == null) {
            throw new IllegalArgumentException("missing ancestor %s (%s)".formatted(fieldName, path));
        } else {
            return objectNode;
        }
    }

    private static ObjectNode insertJsonEntryIntoArray(ObjectNode parentObject, PathComponent pc, ObjectNode childToAdd, boolean isLeaf, DbJsonPath path) {
        String fieldName = pc.attribute();
        ArrayNode arrayNode = (ArrayNode) parentObject.get(fieldName);

        if (isLeaf) {
            if (arrayNode == null) {
                arrayNode = parentObject.arrayNode();
                parentObject.set(fieldName, arrayNode);
            }

            int arraySize = arrayNode.size();
            int arrayIdx = pc.index();
            if (arraySize == arrayIdx) {
                arrayNode.add(childToAdd);
                return null;
            } else if (arraySize < arrayIdx) {
                for (int j = arraySize; j < arrayIdx; j++) {
                    arrayNode.add((JsonNode) null);
                }
                arrayNode.add(childToAdd);
                return null;
            } else {
                JsonNode oldEntry = arrayNode.set(arrayIdx, childToAdd);
                if (!oldEntry.isNull()) {
                    throw new IllegalArgumentException("duplicate entry for %s".formatted(path));
                }
                return null;
            }
        } else {
            return (ObjectNode) arrayNode.get(pc.index());
        }
    }

    public static ObjectNode decodeKeys(ObjectNode dbJson) {
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

                List<Entry<String, JsonNode>> nodes = new ArrayList<>(dbObject.size());
                dbObject.fields().forEachRemaining(nodes::add);
                dbObject.removeAll();

                // replace attribute aliases
                for (Entry<String, JsonNode> property : nodes) {
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


    public record PathComponent(String attribute, Integer index) {}

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
                char ch0 = 0;
                char ch1 = 0;
                int nr = -1;
                // requires trailing '.'
                for (int i = 0; i < len; i++) {
                    final char ch = path.charAt(i);
                    
                    switch (ch) {
                        case '.' -> {
                            list.add(new PathComponent(
                                    ch1 == 0 ? Character.toString(ch0) : "" + ch0 + ch1,
                                    nr < 0 ? null : nr));
                            ch0 = ch1 = 0;
                            nr = -1;
                        }
                        case '1' -> nr = pushNr(nr, 1);
                        case '2' -> nr = pushNr(nr, 2);
                        case '3' -> nr = pushNr(nr, 3);
                        case '4' -> nr = pushNr(nr, 4);
                        case '5' -> nr = pushNr(nr, 5);
                        case '6' -> nr = pushNr(nr, 6);
                        case '7' -> nr = pushNr(nr, 7);
                        case '8' -> nr = pushNr(nr, 8);
                        case '9' -> nr = pushNr(nr, 9);
                        case '0' -> nr = pushNr(nr, 0);
                        default -> {
                            if (ch0 == 0) {
                                ch0 = ch;
                            } else if (ch1 == 0) {
                                ch1 = ch;
                            } else {
                                throw new IllegalArgumentException("Attribute alias too long: %s".formatted(path));
                            }
                        }
                    }
                }

                return new DbJsonPath(path, Collections.unmodifiableList(list));
            }
        }

        private static int pushNr(int nr, int d) {
            if (nr <= 0) {
                nr = d;
            } else {
                nr = 10 * nr + d;
            }
            return nr;
        }
    }
}
