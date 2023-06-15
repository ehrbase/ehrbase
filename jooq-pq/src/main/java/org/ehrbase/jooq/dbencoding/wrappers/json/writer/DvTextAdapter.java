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
import com.nedap.archie.rm.datavalues.DvText;
import java.io.IOException;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.openehr.sdk.util.ObjectSnakeCase;
import org.ehrbase.openehr.sdk.util.SnakeCase;

/**
 * GSON adapter for DvDateTime
 * Required since JSON does not support natively a DateTime data type
 */
public class DvTextAdapter extends DvTypeAdapter<DvText> {

    public DvTextAdapter(AdapterType adapterType) {
        super(adapterType);
    }

    public DvTextAdapter() {}

    @Override
    public DvText read(JsonReader arg0) {
        return null;
    }

    @Override
    public void write(JsonWriter writer, DvText dvalue) throws IOException {
        if (dvalue == null) {
            writer.nullValue();
            return;
        }

        if (adapterType == I_DvTypeAdapter.AdapterType.PG_JSONB) {
            TermMappingAdapter termMappingAdapter = new TermMappingAdapter();
            writer.beginObject();
            writer.name("value").value(dvalue.getValue());
            writer.name(I_DvTypeAdapter.TAG_CLASS_RAW_JSON)
                    .value(new SnakeCase(DvText.class.getSimpleName()).camelToUpperSnake());
            termMappingAdapter.write(writer, dvalue.getMappings());
            writer.endObject();
        } else if (adapterType == I_DvTypeAdapter.AdapterType.RAW_JSON) {
            writer.beginObject();
            writer.name(I_DvTypeAdapter.TAG_CLASS_RAW_JSON).value(new ObjectSnakeCase(dvalue).camelToUpperSnake());
            writer.name("value").value(dvalue.getValue());
            writer.endObject();
        }
    }
}
