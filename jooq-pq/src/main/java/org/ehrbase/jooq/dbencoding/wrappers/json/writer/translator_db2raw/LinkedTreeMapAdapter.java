/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw;

import com.google.gson.TypeAdapter;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.openehr.sdk.aql.webtemplatepath.AqlPath;
import org.ehrbase.openehr.sdk.util.SnakeCase;

/**
 * GSON adapter for LinkedTreeMap
 *
 * <p>NB. @SuppressWarnings("unchecked") is used to deal with generics
 */
public class LinkedTreeMapAdapter extends TypeAdapter<LinkedTreeMap<String, Object>> implements I_DvTypeAdapter {

    private static final Set<String> STRUCTURAL_CLASSES = Set.of(
            "ItemTree",
            "ItemTable",
            "ItemSingle",
            "PointEvent",
            "Instruction",
            "Evaluation",
            "Observation",
            "Action",
            "AdminEntry",
            "IntervalEvent");

    protected AdapterType adapterType;

    public LinkedTreeMapAdapter(AdapterType adapterType) {
        super();
        this.adapterType = adapterType;
    }

    public LinkedTreeMapAdapter() {
        super();
        this.adapterType = AdapterType.DBJSON2RAWJSON;
    }

    //	@Override
    public LinkedTreeMap read(JsonReader arg0) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private void writeInternal(JsonWriter writer, LinkedTreeMap<String, Object> map) throws IOException {

        map = new Children(map).removeDuplicateArchetypeNodeId();

        boolean isItemsOnly = new Children(map).isItemsOnly();
        boolean isMultiEvents = new Children(map).isEvents();
        boolean isMultiContent = new Children(map).isMultiContent();

        fixWrongDbEncoding(map);

        String parentItemsArchetypeNodeId = null;
        String parentItemsType = null;

        if (isItemsOnly || isMultiEvents) {
            // promote archetype node id and type at parent level
            // get the archetype node id
            if (map.containsKey(I_DvTypeAdapter.ARCHETYPE_NODE_ID)) {
                parentItemsArchetypeNodeId = (String) map.get(I_DvTypeAdapter.ARCHETYPE_NODE_ID);
                map.remove(I_DvTypeAdapter.ARCHETYPE_NODE_ID);
            }
            if (map.containsKey(AT_TYPE)) {
                parentItemsType = (String) map.get(AT_TYPE);
                map.remove(AT_TYPE);
            }
            if (map.containsKey(CompositionSerializer.TAG_CLASS)) {
                if (map.get(CompositionSerializer.TAG_CLASS) instanceof ArrayList)
                    parentItemsType = new SnakeCase(
                                    (String) ((ArrayList) map.get(CompositionSerializer.TAG_CLASS)).get(0))
                            .camelToUpperSnake();
                else if (map.get(CompositionSerializer.TAG_CLASS) instanceof String)
                    parentItemsType =
                            new SnakeCase((String) map.get(CompositionSerializer.TAG_CLASS)).camelToUpperSnake();

                map.remove(CompositionSerializer.TAG_CLASS);
            }
        }

        if (isItemsOnly) {
            // CHC 20191003: Removed archetype_node_id writer since it is serviced by closing the array.
            ArrayList<?> items = new Children(map).items();

            if (map.containsKey(CompositionSerializer.TAG_NAME)) {
                if (map.get(CompositionSerializer.TAG_NAME) instanceof ArrayList)
                    new ValueArrayList(writer, map.get(CompositionSerializer.TAG_NAME), CompositionSerializer.TAG_NAME)
                            .write();
                else if (map.get(CompositionSerializer.TAG_NAME) instanceof String)
                    new NameValue(writer, (String) map.get(CompositionSerializer.TAG_NAME)).write();
                else if (map.get(CompositionSerializer.TAG_NAME) instanceof LinkedTreeMap) {
                    new NameValue(writer, (LinkedTreeMap) map.get(CompositionSerializer.TAG_NAME)).write();
                }
            }

            if (map.containsKey(CompositionSerializer.TAG_ARCHETYPE_NODE_ID)) {
                if (map.get(CompositionSerializer.TAG_ARCHETYPE_NODE_ID) instanceof ArrayList)
                    new ValueArrayList(
                                    writer,
                                    map.get(CompositionSerializer.TAG_ARCHETYPE_NODE_ID),
                                    CompositionSerializer.TAG_ARCHETYPE_NODE_ID)
                            .write();
                else
                    writer.name(ARCHETYPE_NODE_ID)
                            .value(map.get(CompositionSerializer.TAG_ARCHETYPE_NODE_ID)
                                    .toString());
            }

            writeItemInArray(ITEMS, items, writer, parentItemsArchetypeNodeId, parentItemsType);
        } else if (isMultiEvents) {
            // assumed sorted (LinkedTreeMap preserve input order)
            ArrayList<?> events = new Children(map).events();
            writeItemInArray(EVENTS, events, writer, parentItemsArchetypeNodeId, parentItemsType);
        } else if (isMultiContent) {
            while (map.keySet().iterator().hasNext()) {
                String key = map.keySet().iterator().next();
                if (!key.startsWith(CompositionSerializer.TAG_CONTENT)) {
                    if (map.get(key) instanceof LinkedTreeMap) {
                        writer.name(key);
                        writer.beginObject();
                        writeNode((LinkedTreeMap) map.get(key), writer);
                        writer.endObject();
                    } else if (map.get(key) instanceof ArrayList) { // due to using multimap
                        if (!key.equals(CompositionSerializer.TAG_CLASS)) { // ignore it
                            ArrayList<?> arrayList = (ArrayList<?>) map.get(key);
                            if (!arrayList.isEmpty())
                                writer.name(key).value(arrayList.get(0).toString());
                        }
                    } else writer.name(key).value((String) map.get(key));
                    map.remove(key);
                } else {
                    if (isNodePredicate(key)) {
                        // set the archetype node id in each children
                        for (Map.Entry<String, Object> kv : map.entrySet()) {
                            for (Object valueMap : (ArrayList<?>) kv.getValue()) {
                                if (valueMap instanceof LinkedTreeMap) {
                                    LinkedTreeMap<String, Object> vm = (LinkedTreeMap<String, Object>) valueMap;
                                    vm.put(
                                            CompositionSerializer.TAG_ARCHETYPE_NODE_ID,
                                            AqlPath.parse(kv.getKey())
                                                    .getLastNode()
                                                    .getAtCode());
                                }
                            }
                        }
                    }
                    Children children = new Children(map);
                    ArrayList<?> contents = children.contents();
                    writeContent(contents, writer);
                    map = children.removeContents();
                }
                if (map.size() == 1) {
                    if (map.get(CompositionSerializer.TAG_CLASS) != null) // the only remaining key is CLASS
                    {
                        return;
                    } else
                        throw new IllegalStateException("Inconsistent encoding of composition, found:" + map.keySet());
                }
            }
        } else {
            writeNode(map, writer);
        }
    }

    /**
     * fix worryingly encoded {@link com.nedap.archie.rm.archetyped.Locatable} attributes {@link
     * Locatable#getLinks()}, {@link Locatable#getUid()} and {@link Locatable#getFeederAudit()} with
     * extra []
     *
     * @param map
     */
    private void fixWrongDbEncoding(LinkedTreeMap<String, Object> map) {
        if (map.containsKey(CompositionSerializer.TAG_UID)
                && map.get(CompositionSerializer.TAG_UID) instanceof List
                && !((List<?>) map.get(CompositionSerializer.TAG_UID)).isEmpty()) {
            map.put(CompositionSerializer.TAG_UID, ((List<?>) map.get(CompositionSerializer.TAG_UID)).get(0));
        }

        if (map.containsKey(CompositionSerializer.TAG_FEEDER_AUDIT)
                && map.get(CompositionSerializer.TAG_FEEDER_AUDIT) instanceof List
                && !((List<?>) map.get(CompositionSerializer.TAG_FEEDER_AUDIT)).isEmpty()) {
            map.put(
                    CompositionSerializer.TAG_FEEDER_AUDIT,
                    ((List<?>) map.get(CompositionSerializer.TAG_FEEDER_AUDIT)).get(0));
        }

        if (map.containsKey(CompositionSerializer.TAG_LINKS)
                && map.get(CompositionSerializer.TAG_LINKS) instanceof List
                && !((List<?>) map.get(CompositionSerializer.TAG_LINKS)).isEmpty()
                && ((List<?>) map.get(CompositionSerializer.TAG_LINKS)).get(0) instanceof List) {
            map.put(CompositionSerializer.TAG_LINKS, ((List<?>) map.get(CompositionSerializer.TAG_LINKS)).get(0));
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedTreeMap<String, Object> reformatMapForCanonical(LinkedTreeMap<String, Object> map) {
        map = new IterativeItemStructure(map).promoteIterations();
        map = new EmbeddedValue(map).formatForEmbeddedTag();
        return map;
    }

    //	@Override
    @SuppressWarnings("unchecked")
    public void write(JsonWriter writer, LinkedTreeMap map) throws IOException {
        if (map.isEmpty()) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writeInternal(writer, map);
        writer.endObject();
    }

    private boolean isNodePredicate(String key) {
        // a key in the form '/xyz[atNNNN]'
        return key.startsWith("/") && key.contains("[") && key.contains("]");
    }

    @SuppressWarnings("unchecked")
    private LinkedTreeMap<String, Object> compactTimeMap(LinkedTreeMap<String, Object> valueMap) {
        LinkedTreeMap<String, Object> compactMap = new LinkedTreeMap<>();
        for (Map.Entry<String, Object> item : valueMap.entrySet()) {
            String key = item.getKey();
            if (key.equals(CompositionSerializer.TAG_VALUE)) {
                String value = (String) ((LinkedTreeMap) (item).getValue()).get("value");
                compactMap.put(CompositionSerializer.TAG_VALUE, value);
            } else {
                compactMap.put(item.getKey(), item.getValue());
            }
        }
        return compactMap;
    }

    /**
     * this method perform a factorization of items as an array. When serialized, items are presented
     * as an array list in the form: <br>
     * <code>
     * /items[openEHR-EHR-...] { item 1 }
     * /items[openEHR-EHR-...] { item 2 }
     * ...
     * </code> <br>
     * The expected result is <code>
     * /items : { item 1 }{item 2}
     * </code> with the node predicate passed as archetype node id inside its respective item content
     *
     * @param heading String the heading of this node (f.e. 'items')
     * @param value ArrayList the content of this node as a list of json structures
     * @param writer {@link JsonWriter} the writer used to create the json translation
     * @param parentItemsArchetypeNodeId String the id of the parent node
     * @param parentItemsType String the type of the parent node (f.e. SECTION)
     * @throws IOException whenever a json writing issue occurs
     */
    private void writeItemInArray(
            String heading,
            ArrayList<?> value,
            JsonWriter writer,
            String parentItemsArchetypeNodeId,
            String parentItemsType)
            throws IOException {
        new ArrayClosure(writer, parentItemsArchetypeNodeId, parentItemsType).start();
        if (value.isEmpty()) {
            return;
        }
        for (int cursor = 0; cursor < value.size(); cursor++) {
            if (cursor == 0) { // initial
                writer.name(heading); // header of items list
                writer.beginArray();
            }
            if (value.get(cursor) instanceof ArrayList) {
                new ArrayListAdapter().write(writer, (ArrayList) value.get(cursor));
            } else { // next siblings
                new LinkedTreeMapAdapter().write(writer, (LinkedTreeMap) value.get(cursor));
            }
        }
        writer.endArray();
    }

    private void writeContent(ArrayList<?> value, JsonWriter writer) throws IOException {

        for (int cursor = 0; cursor < value.size(); cursor++) {
            if (cursor == 0) { // initial
                // insert archetype node id
                writer.name("content");
                writer.beginArray();
            }
            if (value.get(cursor) instanceof ArrayList)
                new ArrayListAdapter().write(writer, (ArrayList) value.get(cursor));
            else new LinkedTreeMapAdapter().write(writer, (LinkedTreeMap) value.get(cursor));
        }
        writer.endArray();
    }

    @SuppressWarnings("unchecked")
    private void writeNode(LinkedTreeMap<String, Object> map, JsonWriter writer) throws IOException {

        // some hacking for some specific entries...
        reformatMapForCanonical(map);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();

            if (value == null) continue;

            String key = entry.getKey();

            if (new OptOut(key).skipIt()) continue;

            String jsonKey = new RawJsonKey(key).toRawJson();
            final String archetypeNodeId = new NodeId(key).predicate();

            // required to deal with DV_MULTIMEDIA embedded document in data
            if (value instanceof ArrayList
                    && key.equals("data")
                    && map.get("_type")
                            .equals(ArchieRMInfoLookup.getInstance()
                                    .getTypeInfo(DvMultimedia.class)
                                    .getRmName())) {
                // prepare a store for the value
                Double[] dataStore = new Double[((ArrayList) value).size()];
                value = ((ArrayList<Double>) value).toArray(dataStore);
            }

            if (value instanceof ArrayList) {
                if (key.equals(CompositionSerializer.TAG_NAME)) {
                    new ValueArrayList(writer, value, key).write();
                } else if (key.equals(CompositionSerializer.TAG_CLASS)) {
                    writer.name(AT_TYPE)
                            .value(new SnakeCase((String) ((ArrayList<?>) value).get(0)).camelToUpperSnake());
                } else if (key.equals(CompositionSerializer.TAG_ARCHETYPE_NODE_ID)) {
                    // same as name above, this is due to usage of MultiValueMap which is backed by ArrayList
                    new ValueArrayList(writer, value, key).write();
                } else {
                    // make sure we service a non empty array list value
                    if (!new ArrayChildren((ArrayList<?>) value).isNull()) {
                        writer.name(jsonKey);
                        writer.beginArray();
                        if (isNodePredicate(key)) {
                            ((ArrayList<?>) value)
                                    .stream()
                                            .filter(o -> Map.class.isAssignableFrom(o.getClass()))
                                            .forEach(m -> ((Map<String, Object>) m)
                                                    .put(I_DvTypeAdapter.ARCHETYPE_NODE_ID, archetypeNodeId));
                        }
                        new ArrayListAdapter().write(writer, (ArrayList<?>) value);
                        writer.endArray();
                    }
                }
            } else if (value instanceof LinkedTreeMap) {
                LinkedTreeMap<String, Object> valueMap = (LinkedTreeMap<String, Object>) value;
                String elementType = new ElementType(valueMap).type();

                if (elementType.equals("History")) {
                    // promote events[...]
                    LinkedTreeMap<String, Object> eventMap =
                            (LinkedTreeMap<String, Object>) valueMap.get(CompositionSerializer.TAG_EVENTS);
                    valueMap.remove(CompositionSerializer.TAG_EVENTS);
                    valueMap.putAll(eventMap);
                    valueMap.put(AT_TYPE, new SnakeCase(elementType).camelToUpperSnake());
                } else if (archetypeNodeId.equals(CompositionSerializer.TAG_TIMING)
                        && elementType.equals("DvParsable")) {
                    // promote value and formalism
                    LinkedTreeMap<String, Object> timingValueMap =
                            (LinkedTreeMap<String, Object>) valueMap.get(CompositionSerializer.TAG_VALUE);
                    if (timingValueMap != null) {
                        valueMap.put(CompositionSerializer.TAG_VALUE, timingValueMap.get("value"));
                        valueMap.put("/formalism", timingValueMap.get("formalism"));
                    }
                }

                if (key.equals(CompositionSerializer.TAG_VALUE)) {
                    // get the class and add it to the value map
                    String type = (String) map.get(CompositionSerializer.TAG_CLASS);
                    if (type != null && !type.isEmpty()) {
                        // pushed into the value map for the next recursion
                        valueMap.put(AT_TYPE, new SnakeCase(type).camelToUpperSnake());
                        // check if this type is composite (DV_INTERVAL<DV_DATE>) to push the actual type down
                        // the value structure
                        if (new GenericRmType(type).isSpecialized()) { // composite
                            valueMap = new GenericRmType(new SnakeCase(type).camelToUpperSnake())
                                    .inferSpecialization(valueMap);
                        }
                    }
                }
                // get the value point type and add it to the value map
                if (valueMap.containsKey(CompositionSerializer.TAG_CLASS)) {
                    valueMap.put(AT_TYPE, new SnakeCase(elementType).camelToUpperSnake());
                    valueMap.remove(CompositionSerializer.TAG_CLASS);
                    if (key.contains("/time")) {
                        valueMap.remove(CompositionSerializer.TAG_NAME);
                    }
                }
                if (isNodePredicate(key)) // contains an archetype node predicate
                {
                    valueMap.put(ARCHETYPE_NODE_ID, archetypeNodeId);
                } else if (key.equals(CompositionSerializer.TAG_ORIGIN) || key.equals(CompositionSerializer.TAG_TIME)) {
                    // compact time expression
                    valueMap = compactTimeMap(valueMap);
                }
                writer.name(jsonKey);
                new LinkedTreeMapAdapter().write(writer, valueMap);
            } else if (value instanceof String) {
                switch (key) {
                    case CompositionSerializer.TAG_CLASS:
                        if (STRUCTURAL_CLASSES.contains(value))
                            writer.name(AT_TYPE).value(new SnakeCase(((String) value)).camelToUpperSnake());
                        break;
                    case CompositionSerializer.TAG_PATH: // this is an element
                        String archetypeNodeId2 =
                                AqlPath.parse((String) value).getLastNode().getAtCode();
                        if (archetypeNodeId2 != null) writer.name(AT_TYPE).value(ELEMENT);
                        // CHC 20191003: removed writer for archetype_node_id as it was not applicable here
                        break;
                    case CompositionSerializer.TAG_NAME:
                        new NameValue(writer, value.toString()).write();
                        break;
                    default:
                        writer.name(jsonKey).value((String) value);
                        break;
                }
            } else if (value instanceof Double) {
                writer.name(new SnakeCase(key).camelToSnake()).value((Double) value);
            } else if (value instanceof Long) {
                writer.name(new SnakeCase(key).camelToSnake()).value((Long) value);
            } else if (value instanceof Number) {
                writer.name(new SnakeCase(key).camelToSnake()).value((Number) value);
            } else if (value instanceof Boolean) {
                writer.name(new SnakeCase(key).camelToSnake()).value((Boolean) value);
            } else if (value instanceof Double[]) {
                writer.name(new SnakeCase(key).camelToSnake());
                writer.beginArray();
                for (Double pix : (Double[]) value) {
                    writer.value(pix.byteValue());
                }
                writer.endArray();
            } else
                throw new IllegalArgumentException("Could not handle value type for key:" + key + ", value:" + value);
        }
    }
}
