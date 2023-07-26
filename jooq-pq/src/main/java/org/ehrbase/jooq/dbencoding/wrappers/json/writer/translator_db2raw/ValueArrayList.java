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
package org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw;

import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;

/**
 * deals with values passed as an array. This is a tweak since we use MultiValueMap which is backed by an ArrayList
 * hence the problem to retrieve a value which should have been a string but come out as an array of string of size 1
 */
public class ValueArrayList {
    private final JsonWriter writer;
    private final ArrayList value;
    private final String tag;

    ValueArrayList(JsonWriter writer, Object value, String tag) {
        this.writer = writer;
        if (value instanceof ArrayList) this.value = (ArrayList) value;
        else throw new IllegalStateException("Invalid value passed as argument");
        this.tag = tag;
    }

    public void write() throws IOException {
        if (value.isEmpty()) return;

        switch (tag) {
            case CompositionSerializer.TAG_NAME:
                LinkedTreeMap nameEncoded = (value.get(0) instanceof ArrayList)
                        ? ((LinkedTreeMap) ((ArrayList) value.get(0)).get(0))
                        : ((LinkedTreeMap) (value.get(0)));

                if (nameEncoded.size() == 1) {
                    new DvTextNameValue(writer, nameEncoded).write();
                }
                if (nameEncoded.size() > 1) { // dvCodedText
                    new DvCodedTextNameValue(writer, nameEncoded).write();
                }

                break;
            case CompositionSerializer.TAG_ARCHETYPE_NODE_ID:
                writer.name(I_DvTypeAdapter.ARCHETYPE_NODE_ID)
                        .value(value.get(0).toString());
                break;
            default:
                throw new IllegalStateException("Unknown serialization tag:" + tag);
        }
    }
}
