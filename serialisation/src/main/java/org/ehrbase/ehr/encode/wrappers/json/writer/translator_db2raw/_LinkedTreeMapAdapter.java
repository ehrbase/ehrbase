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
import org.ehrbase.ehr.encode.wrappers.SnakeCase;
import org.ehrbase.ehr.encode.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.serialisation.CompositionSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GSON adapter for LinkedTreeMap
 */
public class _LinkedTreeMapAdapter extends TypeAdapter<LinkedTreeMap> implements I_DvTypeAdapter {


    protected AdapterType adapterType = AdapterType._DBJSON2RAWJSON;
    protected String parentArchetypeNodeId = null;
    protected String nodeName = null;
    boolean isRoot = true;
    int depth = 0;

    public _LinkedTreeMapAdapter(AdapterType adapterType) {
        super();
        this.adapterType = adapterType;
        isRoot = true;
    }

    public _LinkedTreeMapAdapter() {
        super();
        this.adapterType = AdapterType._DBJSON2RAWJSON;
        isRoot = true;
    }

    public _LinkedTreeMapAdapter(String parentArchetypeNodeId, String nodeName) {
        super();
        this.adapterType = AdapterType._DBJSON2RAWJSON;
        this.parentArchetypeNodeId = parentArchetypeNodeId;
        this.nodeName = nodeName;
        isRoot = false;
    }

    //	@Override
    public LinkedTreeMap read(JsonReader arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    //	@Override
    private void writeInternal(JsonWriter writer, LinkedTreeMap map) throws IOException {

        boolean inArray = false;

        for (Object entry : map.entrySet()) {
            if (entry instanceof Map.Entry) {
                String key = (String) ((Map.Entry) entry).getKey();
                String jsonKey = new RawJsonKey(key).toRawJson();
                String archetypeNodeId = new NodeId(key).predicate();

                Object value = ((Map.Entry) entry).getValue();

                if (value == null)
                    continue;

                if (value instanceof ArrayList) {
                    if (((List) value).size() == 0)
                        continue;
                    //lookahead to check if this embeds another array or terminal leaves
                    boolean terminalNode = isTerminal((ArrayList) value);
//                    System.out.println("ARRAY items:" + key + " isTerminal:" + terminalNode);
                    if (terminalNode) {
                        if (!inArray)
                            writer.name(jsonKey);
                        if (jsonKey.equals(ITEMS) && !inArray) {
                            inArray = true;
//                            System.out.println("begin array -----------------------------");
                            writer.beginArray();
                        }

                        if (key.matches(matchNodePredicate)) {
                            //grab the archetype_node_id and add it to all array value instance
                            for (Object item : (ArrayList) value) {
                                if (item instanceof LinkedTreeMap) {
                                    ((LinkedTreeMap) item).put(ARCHETYPE_NODE_ID, archetypeNodeId);
                                }
                            }
                        }

                        new _LinkedTreeMapAdapter().write(writer, (LinkedTreeMap) ((List) value).get(0));
                    } else {
                        //check if children is an element
//                        if (new ArrayChildren((ArrayList) value).hasElement()) {
//                            writer.name(AT_CLASS).value("ELEMENT");
//                        }
                        writer.name(jsonKey);
                        new _ArrayListAdapter(archetypeNodeId, key).write(writer, (ArrayList) value);
                    }
                } else if (value instanceof LinkedTreeMap) {
//                    check for the children level (f.e. items...)
//                    if (((LinkedTreeMap) value).containsKey(CompositionSerializer.TAG_PATH)) {
//                        writer.name(AT_CLASS).value("ELEMENT");
//                    }
                    if (new Children((LinkedTreeMap) value).isItemsOnly()) {
                        ArrayList<Object> mapAsList = new ArrayList<>();
                        for (Object itemKey : ((LinkedTreeMap) value).keySet()) {
                            LinkedTreeMap<String, Object> asMap = new LinkedTreeMap<>();
                            asMap.put((String) itemKey, ((LinkedTreeMap) value).get(itemKey));
                            mapAsList.add(asMap);
                        }
                        //embed the list
                        writer.name(jsonKey);
                        new _ArrayListAdapter(archetypeNodeId, null).write(writer, mapAsList);
                    } else {
                        writer.name(new RawJsonKey(key).toRawJson());
                        LinkedTreeMap valueMap = (LinkedTreeMap) value;
                        new _LinkedTreeMapAdapter().write(writer, valueMap);
                    }
                } else if (value instanceof String) {
                    if (key.equals(CompositionSerializer.TAG_CLASS)) {
//                        System.out.println("name:" + AT_CLASS);
                        writer.name(AT_CLASS).value(new SnakeCase(((String) value)).camelToUpperSnake());
                    } else if (key.equals(CompositionSerializer.TAG_PATH)) {
                        writer.name(AT_CLASS).value(ELEMENT);
                    } else if (key.equals(CompositionSerializer.TAG_NAME)) {
//                        System.out.println("name:" + NAME);
                        writeNameAsValue(writer, value.toString());
//                        writer.name(NAME).value((String) value);
                    } else {
//                        System.out.println("name:" + key);
                        writer.name(key).value((String) value);
                    }
                } else if (value instanceof Double) {
//                    System.out.println("name:" + new SnakeCase(key).camelToSnake());
                    writer.name(new SnakeCase(key).camelToSnake()).value((Double) value);
                } else if (value instanceof Long) {
//                    System.out.println("name:" + new SnakeCase(key).camelToSnake());
                    writer.name(new SnakeCase(key).camelToSnake()).value((Long) value);
                } else if (value instanceof Number) {
//                    System.out.println("name:" + new SnakeCase(key).camelToSnake());
                    writer.name(new SnakeCase(key).camelToSnake()).value((Number) value);
                } else if (value instanceof Boolean) {
//                    System.out.println("name:" + new SnakeCase(key).camelToSnake());
                    writer.name(new SnakeCase(key).camelToSnake()).value((Boolean) value);
                } else
                    throw new IllegalArgumentException("Could not handle value type for key:" + key + ", value:" + value);
            } else
                throw new IllegalArgumentException("Entry is not a map:" + entry);
        }

        if (inArray) {
//            System.out.println("end array -----------------------------");
            writer.endArray();
        }

        return;
    }

    //	@Override
    public void write(JsonWriter writer, LinkedTreeMap map) throws IOException {

//        MapUtils.debugPrint(System.out, "begin object"+(depth++), map);

        writer.beginObject();
        if (isRoot) {
            //grab the matching node id for this node
            String nodeKey = new PathAttribute().structuralNodeKey(map);
            String path = new PathAttribute().findPath(map);
            if (path != null && nodeKey != null) {
                String archetypeNodeId = new PathAttribute(path).parentArchetypeNodeId(nodeKey);
//            String parentNodeName = new PathAttribute(path).parentNodeName(nodeKey);
                if (archetypeNodeId != null) {
//                    writer.name(ARCHETYPE_NODE_ID).value(archetypeNodeId);
                    new ArchetypeNodeId(writer, archetypeNodeId).write();
                }
            }
//            if (parentNodeName != null)
//                writeNameAsValue(writer, parentNodeName);
            isRoot = false;
        } else {
            if (parentArchetypeNodeId != null) {
//                writer.name(ARCHETYPE_NODE_ID).value(parentArchetypeNodeId);
                new ArchetypeNodeId(writer, parentArchetypeNodeId).write();
            } else {
                String nodeKey = new PathAttribute().structuralNodeKey(map);
                String path = new PathAttribute().findPath(map);
                if (path != null && nodeKey != null) {
                    String archetypeNodeId = new PathAttribute(path).parentArchetypeNodeId(nodeKey);
//            String parentNodeName = new PathAttribute(path).parentNodeName(nodeKey);
                    if (archetypeNodeId != null) {
//                        writer.name(ARCHETYPE_NODE_ID).value(archetypeNodeId);
                        new ArchetypeNodeId(writer, archetypeNodeId).write();
                    }
                }
            }


//            if (nodeName != null)
//                writeNameAsValue(writer, nodeName);

        }
        writeInternal(writer, map);
//        System.out.println("end object ============================================="+(depth--));
        writer.endObject();
        return;
    }

    boolean isTerminal(ArrayList list) {
        for (Object object : list) {
//            if (object instanceof ArrayList)
//                return true;
//            else if (object.getClass().getCanonicalName().contains("java.lang"))
//                return true;
            if (object instanceof LinkedTreeMap) {
                for (Object entry : ((LinkedTreeMap) object).entrySet()) {
//                    String itemKey = ((Map.Entry) entry).getKey().toString();
//                    if (itemKey.equals(CompositionSerializer.TAG_NAME))
//                        continue;
////                    if (!(itemKey.contains(ITEMS)) && !(itemKey.equals(CompositionSerializer.TAG_NAME)))
////                        return true;
                    Object mapValue = ((Map.Entry) entry).getValue();
//                    if (mapValue instanceof LinkedTreeMap) {
//                        Map values = (Map) mapValue;
//                        if (values.containsKey(CompositionSerializer.TAG_CLASS))
//                            return true;
//                    }
                    if (mapValue instanceof ArrayList) {
                        return false;
                    }
//                    else {
//                        throw new IllegalArgumentException("Unhandled type:"+mapValue.getClass());
//                    }

                }
            }
//            else {
//                throw new IllegalArgumentException("Unknown type:"+object.getClass());
//            }
        }
        return true;
    }

    void writeNameAsValue(JsonWriter writer, String value) throws IOException {
        if (value == null || value.isEmpty())
            return;
        writer.name(NAME);
        writer.beginObject();
        writer.name(VALUE).value(value);
        writer.endObject();
    }

}
