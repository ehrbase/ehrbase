/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
import com.nedap.archie.rm.support.identification.GenericId;
import java.io.IOException;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;

/**
 * GSON adapter for GenericId
 */
public class GenericIdAdapter extends DvTypeAdapter<GenericId> {

    public GenericIdAdapter(AdapterType adapterType) {
        super(adapterType);
    }

    @Override
    public GenericId read(JsonReader arg0) throws IOException {
        return null;
    }

    @Override
    public void write(JsonWriter writer, GenericId genericId) throws IOException {
        if (genericId == null) {
            writer.nullValue();
            return;
        }

        if (adapterType == AdapterType.PG_JSONB) {
            writer.beginObject();
            writer.name("scheme").value(genericId.getScheme());
            writer.name("value").value(genericId.getValue());
            writer.name(CompositionSerializer.TAG_CLASS).value(GenericId.class.getSimpleName());
            writer.endObject();
        }
    }
}
