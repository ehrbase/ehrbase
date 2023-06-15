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
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.io.IOException;
import java.util.Optional;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.openehr.sdk.util.ObjectSnakeCase;
import org.ehrbase.openehr.sdk.util.SnakeCase;

/**
 * GSON adapter for DvDateTime
 * Required since JSON does not support natively a DateTime data type
 */
public class DvCodedTextAdapter extends DvTypeAdapter<DvCodedText> {

    private Gson gson;

    public DvCodedTextAdapter(AdapterType adapterType) {
        super(adapterType);
        gson = new GsonBuilder()
                .registerTypeAdapter(CodePhrase.class, new CodePhraseAdapter(adapterType))
                .setPrettyPrinting()
                .create();
    }

    public DvCodedTextAdapter() {}

    @Override
    public DvCodedText read(JsonReader arg0) {
        return null;
    }

    @Override
    public void write(JsonWriter writer, DvCodedText dvalue) throws IOException {
        if (dvalue == null
                || Optional.of(dvalue)
                        .map(DvCodedText::getDefiningCode)
                        .map(CodePhrase::getCodeString)
                        .isEmpty()) {
            writer.nullValue();
            return;
        }

        TermMappingAdapter termMappingAdapter = new TermMappingAdapter();

        if (adapterType == I_DvTypeAdapter.AdapterType.PG_JSONB) {
            writer.beginObject();
            writer.name(VALUE).value(dvalue.getValue());
            writer.name(TAG_CLASS_RAW_JSON).value(new SnakeCase(DvCodedText.class.getSimpleName()).camelToUpperSnake());
            writer.name("definingCode");
            writer.beginObject();
            writer.name("codeString").value(dvalue.getDefiningCode().getCodeString());
            writer.name("terminologyId");
            writer.beginObject();
            writer.name(VALUE).value(dvalue.getDefiningCode().getTerminologyId().getValue());
            writer.name(TAG_CLASS_RAW_JSON)
                    .value(new SnakeCase(TerminologyId.class.getSimpleName()).camelToUpperSnake());
            writer.endObject();
            writer.name(TAG_CLASS_RAW_JSON).value(new SnakeCase(CodePhrase.class.getSimpleName()).camelToUpperSnake());
            writer.endObject();
            termMappingAdapter.write(writer, dvalue.getMappings());
            writer.endObject();
        } else if (adapterType == I_DvTypeAdapter.AdapterType.RAW_JSON) {
            writer.beginObject();
            writer.name(TAG_CLASS_RAW_JSON).value(new ObjectSnakeCase(dvalue).camelToUpperSnake());
            writer.name(VALUE).value(dvalue.getValue());
            CodePhrase codePhrase = dvalue.getDefiningCode();
            writer.name("defining_code").value(gson.toJson(codePhrase));
            writer.name(TAG_CLASS_RAW_JSON).value(new SnakeCase(CodePhrase.class.getSimpleName()).camelToUpperSnake());
            writer.endObject();
        }
    }
}
