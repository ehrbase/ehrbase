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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nedap.archie.rm.generic.Participation;
import com.nedap.archie.rm.generic.PartyIdentified;
import java.io.IOException;

/**
 * GSON adapter for DvDateTime
 * Required since JSON does not support natively a DateTime data type
 */
public class ParticipationAdapter extends DvTypeAdapter<Participation> {

    public ParticipationAdapter(AdapterType adapterType) {
        super(adapterType);
    }

    public ParticipationAdapter() {}

    @Override
    public Participation read(JsonReader arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void write(JsonWriter writer, Participation participation) throws IOException {
        if (participation == null) {
            writer.nullValue();
            return;
        }

        TermMappingAdapter termMappingAdapter = new TermMappingAdapter();

        if (adapterType == AdapterType.PG_JSONB) {
            writer.beginObject();
            writer.name("function");
            new DvTextAdapter(AdapterType.PG_JSONB).write(writer, participation.getFunction());
            writer.name("mode");
            new DvCodedTextAdapter(AdapterType.PG_JSONB).write(writer, participation.getMode());
            writer.name("performer");
            new PartyIdentifiedAdapter(AdapterType.PG_JSONB)
                    .write(writer, (PartyIdentified) participation.getPerformer());
            // TODO: add performer and time
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
