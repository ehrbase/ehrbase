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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import org.ehrbase.serialisation.CompositionSerializer;

import java.io.IOException;

/**
 * GSON adapter for PartyRef
 */
public class PartyRefAdapter extends DvTypeAdapter<PartyRef> {

    private Gson gson;

    public PartyRefAdapter(AdapterType adapterType) {
        super(adapterType);
        gson = new GsonBuilder()
                .registerTypeAdapter(GenericId.class, new GenericIdAdapter(adapterType))
                .setPrettyPrinting()
                .create();
    }

    public PartyRefAdapter() {
        gson = new GsonBuilder()
                .registerTypeAdapter(GenericId.class, new GenericIdAdapter(adapterType))
                .registerTypeAdapter(HierObjectId.class, new HierObjectIdAdapter(adapterType))
                .create();
    }

    @Override
    public PartyRef read(JsonReader arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void write(JsonWriter writer, PartyRef partyRef) throws IOException {
        if (partyRef == null) {
            writer.nullValue();
            return;
        }

        if (adapterType == AdapterType.PG_JSONB) {
            writer.beginObject();
            writer.name("namespace").value(partyRef.getNamespace());
            writer.name(CompositionSerializer.TAG_CLASS).value(PartyRef.class.getSimpleName());
            if (partyRef.getId() != null){
                writer.name("id").jsonValue(gson.toJson(partyRef.getId()));
            }
            //TODO: add Identifiers
            writer.endObject();
        } else if (adapterType == AdapterType.RAW_JSON) {
//
//            writer.beginObject(); //{
//            writer.name(I_DvTypeAdapter.TAG_CLASS_RAW_JSON).value(new ObjectSnakeCase(participation).camelToUpperSnake());
//            writer.name("value").value(participation.getValue());
//            CodePhrase codePhrase = participation.getDefiningCode();
//            writer.name("defining_code").value(gson.toJson(codePhrase));
//            writer.endObject(); //}
        }

    }

}
