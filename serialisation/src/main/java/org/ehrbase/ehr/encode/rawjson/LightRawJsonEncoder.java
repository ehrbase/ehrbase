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

package org.ehrbase.ehr.encode.rawjson;

import com.google.gson.GsonBuilder;
import org.ehrbase.ehr.encode.EncodeUtilArchie;
import org.ehrbase.ehr.encode.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.ehr.encode.wrappers.json.writer.translator_db2raw.ArchieCompositionProlog;
import org.ehrbase.ehr.encode.wrappers.json.writer.translator_db2raw.CompositionRoot;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by christian on 6/21/2017.
 */
public class LightRawJsonEncoder {

    String jsonbOrigin;

    public LightRawJsonEncoder(String jsonbOrigin) {
        this.jsonbOrigin = jsonbOrigin;
    }

    public String encodeContentAsString(String root) {
//        Type listType = new TypeToken<ArrayList<ArrayList<Object>>>(){}.getType();
        GsonBuilder gsondb = EncodeUtilArchie.getGsonBuilderInstance();
        if (jsonbOrigin.startsWith("[")) { //strip the expression as an array
            jsonbOrigin = jsonbOrigin.trim().substring(1, jsonbOrigin.length() - 1);
        }
        Map<String, Object> fromDB = gsondb.create().fromJson(jsonbOrigin, Map.class);

        GsonBuilder gsonRaw = EncodeUtilArchie.getGsonBuilderInstance(I_DvTypeAdapter.AdapterType.DBJSON2RAWJSON);
        String raw;
        if (root != null)
            raw = gsonRaw.create().toJson(fromDB.get(root));
        else
            raw = gsonRaw.create().toJson(fromDB);

        return raw;
    }

    public String encodeCompositionAsString() {
        //get the composition root key
        String root = new CompositionRoot(jsonbOrigin).toString();
        //convert to raw json
        String converted = encodeContentAsString(root);

        return converted.replaceFirst(Pattern.quote("{"), new ArchieCompositionProlog(root).toString());
    }


}
