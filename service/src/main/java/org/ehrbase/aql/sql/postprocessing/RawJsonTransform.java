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

import com.google.gson.JsonElement;
import org.ehrbase.aql.sql.QuerySteps;
import org.ehrbase.aql.sql.binding.JsonbBlockDef;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.ehr.encode.rawjson.LightRawJsonEncoder;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by christian on 2/21/2017.
 */
public class RawJsonTransform implements I_RawJsonTransform {

    private final DSLContext context;

    public RawJsonTransform(DSLContext context) {
        this.context = context;
    }

    public static void toRawJson(Result<Record> result, Collection<QuerySteps> querySteps, I_KnowledgeCache knowledgeCache) {

        for (QuerySteps queryStep : querySteps) {
            if (queryStep.jsonColumnsSize() > 0) {
                for (int cursor = 0; cursor < result.size(); cursor++) {
                    Record record = result.get(cursor);
                    List<JsonbBlockDef> deleteList = new ArrayList<>();
                    for (JsonbBlockDef jsonbBlockDef : queryStep.getJsonColumns()) {
                        String jsonbOrigin = (String) record.getValue(jsonbBlockDef.getField());
                        if (jsonbOrigin == null)
                            continue;
                        //apply the transformation
                        try {
                            JsonElement jsonElement = new LightRawJsonEncoder(jsonbOrigin).encodeContentAsJson(jsonbBlockDef.getJsonPathRoot());
                            //debugging
                            if (jsonbOrigin.contains("@class"))
                                System.out.print("Hum...");
                            record.setValue(jsonbBlockDef.getField(), jsonElement);
                        } catch (Exception e) {
                            //assumes this is not a json element
                            record.setValue(jsonbBlockDef.getField(), jsonbOrigin);
                            deleteList.add(jsonbBlockDef);
                        }
                    }
                    for (JsonbBlockDef deleteBlock: deleteList){
                        queryStep.getJsonColumns().remove(deleteBlock);
                    }
                }
            }
        }
    }




    private static int columnIndex(List<Field> fields, String columnName) {
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (field.getName().equals(columnName))
                return i;
        }
        return -1;
    }

    public static Result<Record> deleteNamedColumn(Result<Record> result, String columnName) {

        List<Field> fields = new ArrayList<>();

        fields.addAll(Arrays.asList(result.fields()));
        int ndx = columnIndex(fields, columnName);
        if (ndx >= 0) {
            fields.remove(ndx);
            Field[] arrayField = fields.toArray(new Field[]{});

            return result.into(arrayField);
        } else
            return result;
    }

//    public static Record cloneRecord(Record record, Collection<QuerySteps> querySteps){
//        List<Field> fields = new ArrayList<>();
//        List<String> jsonColumns = new ArrayList<>();
//
//        for (QuerySteps queryStep: querySteps) {
//            for (JsonbBlockDef jsonbBlockDef : queryStep.getJsonColumns()) {
//                jsonColumns.add(jsonbBlockDef.getField().getName());
//            }
//        }
//
//        fields.addAll(Arrays.asList(record.fields()));
//
//
//
//        Field[] arrayField = fields.toArray(new Field[]{});
//
//        Record newRecord = record.into(arrayField);
//        return newRecord;
//    }
}
