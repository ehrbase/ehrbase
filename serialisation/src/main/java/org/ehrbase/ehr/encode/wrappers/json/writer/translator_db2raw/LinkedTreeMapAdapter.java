/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.ehr.encode.wrappers.json.writer.translator_db2raw;

import com.google.gson.TypeAdapter;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.RMTypeInfo;
import org.ehrbase.ehr.encode.wrappers.SnakeCase;
import org.ehrbase.ehr.encode.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.serialisation.CompositionSerializer;

import java.io.IOException;
import java.util.*;

/**
 * GSON adapter for LinkedTreeMap
 */
public class LinkedTreeMapAdapter extends TypeAdapter<LinkedTreeMap> implements I_DvTypeAdapter {

    String[] structuralClasses = {"PointEvent", "Instruction", "Evaluation", "Observation", "Action", "AdminEntry", "IntervalEvent"};

    protected AdapterType adapterType;
    boolean isRoot;

    public LinkedTreeMapAdapter(AdapterType adapterType) {
        super();
        this.adapterType = adapterType;
        isRoot = true;
    }


    public LinkedTreeMapAdapter() {
        super();
        this.adapterType = AdapterType.DBJSON2RAWJSON;
        isRoot = false;
    }

    //	@Override
    public LinkedTreeMap read(JsonReader arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    //	@Override
    private void writeInternal(JsonWriter writer, LinkedTreeMap map) throws IOException {

        boolean isItemsOnly = new Children(map).isItemsOnly();
        boolean isMultiEvents = new Children(map).isEvents();
        boolean isMultiContent = new Children(map).isMultiContent();

        String parentItemsArchetypeNodeId = null;
        String parentItemsType = null;

        if (isItemsOnly || isMultiEvents) {
            //promote archetype node id and type at parent level
            //get the archetype node id
            if (map.containsKey(I_DvTypeAdapter.ARCHETYPE_NODE_ID)) {
                parentItemsArchetypeNodeId = (String) map.get(I_DvTypeAdapter.ARCHETYPE_NODE_ID);
                map.remove(I_DvTypeAdapter.ARCHETYPE_NODE_ID);
            }
            if (map.containsKey(AT_TYPE)) {
                parentItemsType = (String) map.get(AT_TYPE);
                map.remove(AT_TYPE);
            }
            if (map.containsKey(CompositionSerializer.TAG_CLASS)) {
                parentItemsType = new SnakeCase((String) ((ArrayList) map.get(CompositionSerializer.TAG_CLASS)).get(0)).camelToUpperSnake();
                map.remove(CompositionSerializer.TAG_CLASS);
            }
        } else if (isMultiContent) {
            if (map.containsKey(I_DvTypeAdapter.ARCHETYPE_NODE_ID)) {
                parentItemsArchetypeNodeId = (String) map.get(I_DvTypeAdapter.ARCHETYPE_NODE_ID);
                map.remove(I_DvTypeAdapter.ARCHETYPE_NODE_ID);
            }
        }

        if (isItemsOnly) {
            //CHC 20191003: Removed archetype_node_id writer since it is serviced by closing the array.
            ArrayList items = new Children(map).items();
            writeItemInArray(ITEMS, items, writer, parentItemsArchetypeNodeId, parentItemsType);
        } else if (isMultiEvents) {
            //assumed sorted (LinkedTreeMap preserve input order)
            ArrayList events = new Children(map).events();
            writeItemInArray(EVENTS, events, writer, parentItemsArchetypeNodeId, parentItemsType);
        } else if (isMultiContent) {
//            Iterator iterator = map.keySet().iterator();
            String key = map.keySet().iterator().next().toString();
            while (key != null){
                if (!key.startsWith(CompositionSerializer.TAG_CONTENT)){
                    if (map.get(key) instanceof LinkedTreeMap) {
                        writer.name(key);
                        writer.beginObject();
                        writeNode((LinkedTreeMap) map.get(key), writer);
                        writer.endObject();
                    }
                    else
                        writer.name(key).value((String)map.get(key));
                    map.remove(key);
                }
                else {
                    Children children = new Children(map);
                    ArrayList contents = children.contents();
                    if (isNodePredicate(key)) {
                        String archetypeNodeId = new PathAttribute(key).archetypeNodeId();
                        contents
                                .stream()
                                .filter(o -> List.class.isAssignableFrom(o.getClass()))
                                .flatMap(o -> ((List) o).stream())
                                .filter(o -> Map.class.isAssignableFrom(o.getClass()))
                                .forEach(m -> ((Map) m).put(I_DvTypeAdapter.ARCHETYPE_NODE_ID, archetypeNodeId));
                    }
                    writeContent(contents, writer);
                    map = children.removeContents();
                }
                if (map.size() == 0) {
                    return;
                }
                else
                    key = map.keySet().iterator().next().toString();
            }
        } else {
            writeNode(map, writer);
        }

        return;
    }

    private LinkedTreeMap reformatEmbeddedValue(LinkedTreeMap instructionMap, String tag) {

        if (instructionMap.containsKey(tag)) {
            LinkedTreeMap<String, Object> narrative = (LinkedTreeMap) instructionMap.get(tag);
            //get the value
            LinkedTreeMap narrativeValue = (LinkedTreeMap) narrative.get(CompositionSerializer.TAG_VALUE);
            narrative.replace(CompositionSerializer.TAG_VALUE, narrativeValue.get("value"));
        }

        return instructionMap;
    }

    private LinkedTreeMap promoteActivities(LinkedTreeMap instructionMap) {

        if (instructionMap.containsKey(CompositionSerializer.TAG_ACTIVITIES)) {
            LinkedTreeMap<String, Object> activities = (LinkedTreeMap) instructionMap.get(CompositionSerializer.TAG_ACTIVITIES);
            for (Map.Entry<String, Object> activityItem : activities.entrySet()) {
                if (activityItem.getKey().startsWith(CompositionSerializer.TAG_ACTIVITIES)) {
                    instructionMap.put(activityItem.getKey(), activityItem.getValue());
                }
            }
            instructionMap.remove(CompositionSerializer.TAG_ACTIVITIES);
        }
        return instructionMap;
    }

    private LinkedTreeMap reformatMapForCanonical(LinkedTreeMap map) {
        if (map.containsKey(CompositionSerializer.TAG_ACTIVITIES))
            map = promoteActivities(map);
        if (map.containsKey(CompositionSerializer.TAG_NARRATIVE))
            map = reformatEmbeddedValue(map, CompositionSerializer.TAG_NARRATIVE);
        if (map.containsKey(CompositionSerializer.TAG_MATH_FUNCTION))
            map = reformatEmbeddedValue(map, CompositionSerializer.TAG_MATH_FUNCTION);
        if (map.containsKey(CompositionSerializer.TAG_WIDTH))
            map = reformatEmbeddedValue(map, CompositionSerializer.TAG_WIDTH);
        if (map.containsKey(CompositionSerializer.TAG_UID))
            map = reformatEmbeddedValue(map, CompositionSerializer.TAG_UID);
        return map;
    }


    //	@Override
    public void write(JsonWriter writer, LinkedTreeMap map) throws IOException {
        if (map.isEmpty()) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writeInternal(writer, map);
        writer.endObject();
        return;
    }

    void writeNameAsValue(JsonWriter writer, String value) throws IOException {
        if (value == null || value.isEmpty())
            return;
        writer.name(NAME);
        writer.beginObject();
        writer.name(VALUE).value(value);
        writer.endObject();
    }

    void writeNameAsValue(JsonWriter writer, ArrayList value) throws IOException {
//        return;
        //get the name value
        //protective against old entries in the DB...
        if (value == null)
            return;
        Object nameDefinition = ((Map) (value.get(0))).get("value");
        if (nameDefinition != null) {
            writeNameAsValue(writer, nameDefinition.toString());
        }

//        writeNameAsValue(writer, );
    }


    private boolean isNodePredicate(String key) {
        //a key in the form '/xyz[atNNNN]'
        if (key.startsWith("/") && key.contains("[") && key.contains("]"))
            return true;
        else
            return false;
    }

    private LinkedTreeMap compactTimeMap(LinkedTreeMap valueMap) {
        LinkedTreeMap compactMap = new LinkedTreeMap();
        for (Object item : valueMap.entrySet()) {
            String key = (String) ((Map.Entry) item).getKey();
            if (key.equals(CompositionSerializer.TAG_VALUE)) {
                String value = (String) ((LinkedTreeMap) ((Map.Entry) item).getValue()).get("value");
                compactMap.put(CompositionSerializer.TAG_VALUE, value);
            } else {
                compactMap.put(((Map.Entry) item).getKey(), ((Map.Entry) item).getValue());
            }

        }
        return compactMap;
    }

    /**
     * this method perform a factorization of items as an array. When serialized, items are presented as an array list
     * in the form:
     * <br>
     * <code>
     * /items[openEHR-EHR-...] { item 1 }
     * /items[openEHR-EHR-...] { item 2 }
     * ...
     * </code>
     * <br>
     * The expected result is
     * <code>
     * /items : { item 1 }{item 2}
     * </code>
     * with the node predicate passed as archetype node id inside its respective item content
     *
     * @param value
     * @param writer
     * @param parentItemsArchetypeNodeId
     * @param parentItemsType
     * @return
     * @throws IOException
     */
    private void writeItemInArray(String heading, ArrayList value, JsonWriter writer, String parentItemsArchetypeNodeId, String parentItemsType) throws IOException {
        new ArrayClosure(writer, parentItemsArchetypeNodeId, parentItemsType).start();
        if (value.isEmpty()) {
            return;
        }
        for (int cursor = 0; cursor < value.size(); cursor++) {
            if (cursor == 0) { //initial
                writer.name(heading); //header of items list
                writer.beginArray();
            }
            if (value.get(cursor) instanceof ArrayList) {
                new ArrayListAdapter().write(writer, (ArrayList) value.get(cursor));
            } else { //next siblings
                new LinkedTreeMapAdapter().write(writer, (LinkedTreeMap) ((ArrayList) value).get(cursor));

            }
        }
        writer.endArray();
    }

    private void writeContent(ArrayList value, JsonWriter writer) throws IOException {

        for (int cursor = 0; cursor < value.size(); cursor++) {
            if (cursor == 0) { //initial
                //insert archetype node id
                writer.name("content");
                writer.beginArray();

            }
            if (value.get(cursor) instanceof ArrayList)
                new ArrayListAdapter().write(writer, (ArrayList) value.get(cursor));
            else
                new LinkedTreeMapAdapter().write(writer, (LinkedTreeMap) value.get(cursor));
        }
        writer.endArray();
    }

    private void writeNode(LinkedTreeMap map, JsonWriter writer) throws IOException {

        ArrayList nodeNameValue;

        //some hacking for some specific entries...
        map = reformatMapForCanonical(map);

        for (Object entry : map.entrySet()) {
            Object value = ((Map.Entry) entry).getValue();

            if (value == null)
                continue;

            String key = (String) ((Map.Entry) entry).getKey();
            String jsonKey = new RawJsonKey(key).toRawJson();
            final String archetypeNodeId = new NodeId(key).predicate();

            //required to deal with DV_MULTIMEDIA embedded document in data
            if (value instanceof ArrayList && key.equals("data") && map.get("_type").equals(ArchieRMInfoLookup.getInstance().getTypeInfo(DvMultimedia.class).getRmName())){
                //prepare a store for the value
                Double[] dataStore = new Double[((ArrayList) value).size()];
                value = ((ArrayList) value).toArray(dataStore);

            }

            if (value instanceof ArrayList) {
                if (key.equals(CompositionSerializer.TAG_NAME)) {
//                            writeNameAsValue(writer, (ArrayList) value);
                    nodeNameValue = (ArrayList) value;
                    writeNameAsValue(writer, nodeNameValue);
                } else if (key.equals(CompositionSerializer.TAG_CLASS)) {
                    writer.name(AT_TYPE).value(new SnakeCase((String) ((ArrayList) value).get(0)).camelToUpperSnake());
                } else {
                    writer.name(jsonKey);
                    writer.beginArray();
                    if (isNodePredicate(key)) {
                        ((ArrayList) value).stream()
                                .filter(o -> Map.class.isAssignableFrom(o.getClass()))
                                .forEach(m -> ((Map) m).put(I_DvTypeAdapter.ARCHETYPE_NODE_ID, archetypeNodeId));
                    }
                    new ArrayListAdapter().write(writer, (ArrayList) value);
                    writer.endArray();
                }
            } else if (value instanceof LinkedTreeMap) {
                LinkedTreeMap valueMap = (LinkedTreeMap) value;
                String elementType = new ElementType(valueMap).type();

                if (elementType.equals("History")) {
                    //promote events[...]
                    LinkedTreeMap eventMap = (LinkedTreeMap) valueMap.get(CompositionSerializer.TAG_EVENTS);
                    valueMap.remove(CompositionSerializer.TAG_EVENTS);
                    valueMap.putAll(eventMap);
                } else if (archetypeNodeId.equals(CompositionSerializer.TAG_TIMING) && elementType.equals("DvParsable")) {
                    //promote value and formalism
                    Map timingValueMap = (LinkedTreeMap) valueMap.get(CompositionSerializer.TAG_VALUE);
                    if (timingValueMap != null) {
                        valueMap.put(CompositionSerializer.TAG_VALUE, timingValueMap.get("value"));
                        valueMap.put("/formalism", timingValueMap.get("formalism"));
                    }
                }

                if (key.equals(CompositionSerializer.TAG_VALUE)) {
                    //get the class and add it to the value map
                    String type = (String) map.get(CompositionSerializer.TAG_CLASS);
                    if (type != null && !type.isEmpty())
                        //pushed into the value map for the next recursion
                        valueMap.put(AT_TYPE, new SnakeCase(type).camelToUpperSnake());
//                            writer.name(AT_TYPE).value(new SnakeCase(type).camelToUpperSnake());
                }
                //get the value point type and add it to the value map
                if (valueMap.containsKey(CompositionSerializer.TAG_CLASS)) {
                    valueMap.put(AT_TYPE, new SnakeCase(elementType).camelToUpperSnake());
                    valueMap.remove(CompositionSerializer.TAG_CLASS);
                    //TODO: CHC, 180426 temporary fix, modify DB encoding to not include name for attribute.
                    if (key.contains("/time") && valueMap.containsKey(CompositionSerializer.TAG_NAME)) {
                        valueMap.remove(CompositionSerializer.TAG_NAME);
                    }
                }
                if (isNodePredicate(key)) //contains an archetype node predicate
                    valueMap.put(ARCHETYPE_NODE_ID, archetypeNodeId);
                else if (key.equals(CompositionSerializer.TAG_ORIGIN) || key.equals(CompositionSerializer.TAG_TIME)) {
                    //compact time expression
                    valueMap = compactTimeMap(valueMap);
                }
                writer.name(jsonKey);
                new LinkedTreeMapAdapter().write(writer, valueMap);
            } else if (value instanceof String) {
                switch (key) {
                    case CompositionSerializer.TAG_CLASS:
                        if (Arrays.asList(structuralClasses).contains(value))
                            writer.name(AT_TYPE).value(new SnakeCase(((String) value)).camelToUpperSnake());
                        break;
                    case CompositionSerializer.TAG_PATH:  //this is an element
                        String archetypeNodeId2 = new PathAttribute((String) value).archetypeNodeId();
                        if (archetypeNodeId2 != null)
                            writer.name(AT_TYPE).value(ELEMENT);
                        //CHC 20191003: removed writer for archetype_node_id as it was not applicable here
                        break;
                    case CompositionSerializer.TAG_NAME:
                        writeNameAsValue(writer, value.toString());
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
                for (Double pix: (Double[])value){
                    writer.value(pix.byteValue());
                }
                writer.endArray();
            } else
                throw new IllegalArgumentException("Could not handle value type for key:" + key + ", value:" + value);
        }
    }

}
