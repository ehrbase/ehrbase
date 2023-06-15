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
package org.ehrbase.jooq.dbencoding.wrappers.json.writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.generic.PartyIdentified;
import java.io.IOException;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;

/**
 * GSON adapter for DvDateTime
 * Required since JSON does not support natively a DateTime data type
 */
public class PartyIdentifiedAdapter extends DvTypeAdapter<PartyIdentified> {

    private Gson gson;

    public PartyIdentifiedAdapter(AdapterType adapterType) {
        super(adapterType);
        gson = new GsonBuilder()
                .registerTypeAdapter(CodePhrase.class, new CodePhraseAdapter(adapterType))
                .setPrettyPrinting()
                .create();
    }

    public PartyIdentifiedAdapter() {}

    @Override
    public PartyIdentified read(JsonReader arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void write(JsonWriter writer, PartyIdentified partyIdentified) throws IOException {
        if (partyIdentified == null) {
            writer.nullValue();
            return;
        }

        TermMappingAdapter termMappingAdapter = new TermMappingAdapter();

        if (adapterType == AdapterType.PG_JSONB) {
            writer.beginObject();
            writer.name("name").value(partyIdentified.getName());
            writer.name(CompositionSerializer.TAG_CLASS).value(PartyIdentified.class.getSimpleName());
            // TODO: add Identifiers
            writer.endObject();
        } else if (adapterType == AdapterType.RAW_JSON) {
            //
            //            writer.beginObject(); //{
            //            writer.name(I_DvTypeAdapter.TAG_CLASS_RAW_JSON).value(new
            // ObjectSnakeCase(participation).camelToUpperSnake());
            //            writer.name("value").value(participation.getValue());
            //            CodePhrase codePhrase = participation.getDefiningCode();
            //            writer.name("defining_code").value(gson.toJson(codePhrase));
            //            writer.endObject(); //}
        }
    }
}
