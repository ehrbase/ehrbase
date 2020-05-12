/*
 * Copyright (c) 2020 Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
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
package org.ehrbase.ehr.encode.wrappers.json.writer;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nedap.archie.rm.support.identification.HierObjectId;
import org.ehrbase.serialisation.CompositionSerializer;

import java.io.IOException;

/**
 * GSON adapter for HierObjectId
 */
public class HierObjectIdAdapter extends DvTypeAdapter<HierObjectId> {


    public HierObjectIdAdapter(AdapterType adapterType) {
        super(adapterType);
    }

    public HierObjectIdAdapter() {
    }

    @Override
    public HierObjectId read(JsonReader arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void write(JsonWriter writer, HierObjectId hierObjectId) throws IOException {
        if (hierObjectId == null) {
            writer.nullValue();
            return;
        }

        if (adapterType == AdapterType.PG_JSONB) {
            writer.beginObject();
            writer.name("value").value(hierObjectId.getValue());
            writer.name(CompositionSerializer.TAG_CLASS).value(HierObjectId.class.getSimpleName());
            writer.endObject();
        } else if (adapterType == AdapterType.RAW_JSON) {
//            writer.beginObject(); //{
//            writer.name(I_DvTypeAdapter.TAG_CLASS_RAW_JSON).value(new ObjectSnakeCase(genericId).camelToUpperSnake());
//            writer.name("code_string").value(genericId.getCodeString());
//            writer.name("terminology_id").value(gson.toJson(genericId.getTerminologyId()));
//            writer.endObject(); //}
        }

    }

}
