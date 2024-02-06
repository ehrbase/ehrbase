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
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import java.io.IOException;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.datetime.DvDateTimeAttributes;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.openehr.sdk.util.ObjectSnakeCase;

/**
 * GSON adapter for DvDateTime
 * Required since JSON does not support natively a DateTime data type
 */
public class DvDateTimeAdapter extends DvTypeAdapter<DvDateTime> {

    public DvDateTimeAdapter(AdapterType adapterType) {
        super(adapterType);
    }

    public DvDateTimeAdapter() {}

    @Override
    public DvDateTime read(JsonReader arg0) {
        return null;
    }

    @Override
    public void write(JsonWriter writer, DvDateTime dvalue) throws IOException {
        if (dvalue == null || dvalue.getValue() == null) {
            writer.nullValue();
            return;
        }

        // new encoding
        DvDateTimeAttributes dvDateTimeAttributes = DvDateTimeAttributes.instanceFromValue(dvalue);

        if (adapterType == I_DvTypeAdapter.AdapterType.PG_JSONB) {
            writer.beginObject();
            writer.name(VALUE).value(dvDateTimeAttributes.getValueAsProvided().toString());
            writer.name(EPOCH_OFFSET).value(dvDateTimeAttributes.getTimeStamp());
            writer.endObject();
        } else if (adapterType == I_DvTypeAdapter.AdapterType.RAW_JSON) {
            writer.beginObject();
            writer.name(I_DvTypeAdapter.TAG_CLASS_RAW_JSON).value(new ObjectSnakeCase(dvalue).camelToUpperSnake());
            writer.name(VALUE).value(dvalue.getValue().toString());
            writer.endObject();
        }
    }
}
