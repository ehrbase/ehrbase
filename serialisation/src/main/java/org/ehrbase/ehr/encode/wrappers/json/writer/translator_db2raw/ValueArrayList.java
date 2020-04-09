/*
 * Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley Hannover Medical School.
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
package org.ehrbase.ehr.encode.wrappers.json.writer.translator_db2raw;

import com.google.gson.stream.JsonWriter;
import org.ehrbase.ehr.encode.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.serialisation.CompositionSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

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
        if (value instanceof ArrayList)
            this.value = (ArrayList)value;
        else
            throw new IllegalStateException("Invalid value passed as argument");
        this.tag = tag;
    }

    public void write() throws IOException {
        if (value.isEmpty())
            return;

        switch (tag){
            case CompositionSerializer.TAG_NAME:
                Object nameDefinition = ((Map) (value.get(0))).get("value");
                if (nameDefinition != null) {
                    new NameValue(writer, nameDefinition.toString()).write();
                }
                break;
            case CompositionSerializer.TAG_ARCHETYPE_NODE_ID:
                writer.name(I_DvTypeAdapter.ARCHETYPE_NODE_ID).value(value.get(0).toString());
                break;
            default:
                throw new IllegalStateException("Unknown serialization tag:"+tag);
        }
    }
}
