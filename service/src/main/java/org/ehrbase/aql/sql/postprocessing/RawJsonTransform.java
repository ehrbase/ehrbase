/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

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

package org.ehrbase.aql.sql.postprocessing;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.ehrbase.aql.sql.QuerySteps;
import org.ehrbase.aql.sql.binding.JsonbBlockDef;
import org.ehrbase.ehr.util.LocatableHelper;
import org.ehrbase.serialisation.dbencoding.EncodeUtilArchie;
import org.ehrbase.serialisation.dbencoding.rawjson.LightRawJsonEncoder;
import org.jooq.Record;
import org.jooq.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by christian on 2/21/2017.
 */
@SuppressWarnings({"unchecked","java:S3776"})
public class RawJsonTransform implements IRawJsonTransform {

    public static final String TEMPLATE_ID = "_TEMPLATE_ID";
    public static final String ARRAY_MARKER = "$array$";

    private RawJsonTransform(){}

    public static void toRawJson(Result<Record> result, Collection<QuerySteps> querySteps) {


        for (QuerySteps queryStep : querySteps) {

            if (queryStep.jsonColumnsSize() > 0) {
                result.forEach(record -> {
                    List<JsonbBlockDef> deleteList = new ArrayList<>();
                    for (JsonbBlockDef jsonbBlockDef : queryStep.getJsonColumns()) {

                        if (record.getValue(jsonbBlockDef.getField()) == null)
                            continue;

                        String jsonbOrigin = record.getValue(jsonbBlockDef.getField()).toString();

                        //apply the transformation
                        try {
                            JsonElement jsonElement;
                            if (new ResultBlock(jsonbBlockDef).isCanonical()) {
                                GsonBuilder gsonRaw = EncodeUtilArchie.getGsonBuilderInstance();
                                JsonElement item = gsonRaw.create().toJsonTree(gsonRaw.create().fromJson(jsonbOrigin, List.class));
                                if (item instanceof JsonArray)
                                    jsonElement = item.getAsJsonArray();
                                else
                                    jsonElement = item.getAsJsonObject();
                            }
                            else {
                                //this allows to deal with json array without impacting the structure of json transform
                                //as it deals with complex arrays such as items[...], content[...] etc.
                                //hence, we pass the array as a a simple key-value and then retrieve the value part
                                //referenced by "$array$"
                                if (jsonbOrigin.startsWith("[") && jsonbOrigin.endsWith("]"))
                                    jsonbOrigin = "{\"$array$\":"+jsonbOrigin+"}";
                                jsonElement = new LightRawJsonEncoder(jsonbOrigin).encodeContentAsJson(jsonbBlockDef.getJsonPathRoot());
                                if (jsonElement.getAsJsonObject().has(ARRAY_MARKER)) {
                                    if (hasPredicate(jsonbBlockDef.getPath())) //f.e. events[at0002]
                                        jsonElement = jsonElement.getAsJsonObject().getAsJsonArray(ARRAY_MARKER).get(0);
                                    else //f.e. ehr/contributions -> an attribute that is an array
                                        jsonElement = jsonElement.getAsJsonObject().getAsJsonArray(ARRAY_MARKER);

                                }
                            }

                            record.setValue(jsonbBlockDef.getField(), jsonElement);

                        } catch (Exception e) {
                            //assumes this is not a json element
                            record.setValue(jsonbBlockDef.getField(), jsonbOrigin);
                            deleteList.add(jsonbBlockDef);
                        }

                    }
                    for (JsonbBlockDef deleteBlock : deleteList) {
                        queryStep.getJsonColumns().remove(deleteBlock);
                    }
                });
            }
        }
    }


    private static boolean hasPredicate(String path){

        try {
            List<String> segments = LocatableHelper.dividePathIntoSegments(path);

            return (segments.get(segments.size() - 1).contains("["));
        }
        catch (NoSuchElementException e) { //not an AQL path (f.e. function based)
            return false;
        }
    }
}
