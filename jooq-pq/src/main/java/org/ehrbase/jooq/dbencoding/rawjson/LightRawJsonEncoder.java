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
package org.ehrbase.jooq.dbencoding.rawjson;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.internal.LinkedTreeMap;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.ehrbase.jooq.dbencoding.EncodeUtilArchie;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw.ArchieCompositionProlog;
import org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw.CompositionRoot;

/**
 * Created by christian on 6/21/2017.
 */
public class LightRawJsonEncoder {

    private String jsonbOrigin;

    public LightRawJsonEncoder(String jsonbOrigin) {
        this.jsonbOrigin = jsonbOrigin;
    }

    public String encodeContentAsString(String root) {

        Object fromDB = db2map(root != null && root.equals("value"));

        GsonBuilder gsonRaw = EncodeUtilArchie.getGsonBuilderInstance(I_DvTypeAdapter.AdapterType.DBJSON2RAWJSON);
        String raw = null;
        if (fromDB instanceof Map) {
            if (root != null) {
                Object contentMap = ((Map) fromDB).get(root);
                if (contentMap instanceof LinkedTreeMap && ((LinkedTreeMap) contentMap).size() == 0) // empty content
                raw = encodeNullContent();
                else raw = gsonRaw.create().toJson(((Map) fromDB).get(root));
            } else raw = gsonRaw.create().toJson(fromDB);
        }

        return raw;
    }

    private String encodeNullContent() {
        Map<String, Object> nullContentMap = new Hashtable<>();
        nullContentMap.put("content", new ArrayList<>());
        return new GsonBuilder().create().toJson(nullContentMap);
    }

    public JsonElement encodeContentAsJson(String root) {
        GsonBuilder gsonRaw = EncodeUtilArchie.getGsonBuilderInstance(I_DvTypeAdapter.AdapterType.DBJSON2RAWJSON);
        JsonElement jsonElement = gsonRaw.create().toJsonTree(db2map(root != null && root.equals("value")));
        if (root != null) {
            // in order to create the canonical form, build the ELEMENT json (hence the type is passed into the embedded
            // value)
            jsonElement = jsonElement.getAsJsonObject().get(root);
        }

        return jsonElement;
    }

    public String encodeCompositionAsString() {
        // get the composition root key
        String root = new CompositionRoot(jsonbOrigin).toString();
        // convert to raw json
        String converted = encodeContentAsString(root);

        Map<String, Object> compoMap = (Map<String, Object>) db2map(false);

        String compositionName = (String) ((Map) ((List) compoMap.get("/name")).get(0)).get("value");

        return converted.replaceFirst(
                Pattern.quote("{"), new ArchieCompositionProlog(root, compositionName).toString());
    }

    public Map<String, Object> encodeOtherDetailsAsMap() {
        return (Map<String, Object>) db2map(false);
    }

    @SuppressWarnings("unchecked")
    private Object db2map(boolean isValue) {
        boolean isArray = false;

        GsonBuilder gsondb = EncodeUtilArchie.getGsonBuilderInstance();
        if (jsonbOrigin.startsWith("[")) {
            if (isValue) {
                jsonbOrigin = jsonbOrigin.trim().substring(1, jsonbOrigin.length() - 1);
            } else isArray = true;
        }

        Object fromDB = gsondb.create().fromJson(jsonbOrigin, isArray ? ArrayList.class : Map.class);

        if (fromDB instanceof Map && ((Map) fromDB).containsKey("content")) {
            // push contents upward
            Object contents = ((Map) fromDB).get("content");

            if (contents instanceof LinkedTreeMap) {
                for (Object contentItem : ((LinkedTreeMap) contents).entrySet()) {
                    if (contentItem instanceof Map.Entry) {
                        ((Map) fromDB)
                                .put(
                                        ((Map.Entry) contentItem).getKey().toString(),
                                        ((Map.Entry) contentItem).getValue());
                    }
                }
                ((Map) fromDB).remove("content");
            }
        }

        return fromDB;
    }
}
