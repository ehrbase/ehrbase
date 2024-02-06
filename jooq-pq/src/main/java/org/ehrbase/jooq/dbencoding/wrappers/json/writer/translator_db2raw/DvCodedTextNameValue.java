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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;

public class DvCodedTextNameValue implements I_NameValueHandler {

    private final JsonWriter writer;
    private final String value;
    private final List mappings;
    private String codeString;
    private String terminologyId;
    private String preferredTerm;

    DvCodedTextNameValue(JsonWriter writer, LinkedTreeMap value) {
        this.writer = writer;
        this.value = value.get("value").toString();
        this.mappings = (List) value.get("mappings");
        if (value.get("defining_code") != null) {
            this.codeString =
                    ((Map) value.get("defining_code")).get("code_string").toString();
            this.terminologyId = ((Map) ((Map) value.get("defining_code")).get("terminology_id"))
                    .get("value")
                    .toString();
            this.preferredTerm = Optional.of(value)
                    .map(v -> v.get("defining_code"))
                    .map(Map.class::cast)
                    .map(m -> m.get("preferred_term"))
                    .map(Object::toString)
                    .orElse(null);
        }
    }

    /**
     * Encode a name value into the DB json structure
     * <code>
     *     "name": {
     *         "value":...
     *     }
     * </code>
     * @throws IOException
     */
    @Override
    public void write() throws IOException {
        if (value == null || value.isEmpty()) return;
        writer.name(I_DvTypeAdapter.NAME);
        writer.beginObject();
        writer.name(I_DvTypeAdapter.VALUE).value(value);
        if (mappings != null) {
            DvTextNameValue.writeTermMappingList(writer, mappings);
        }

        if (codeString != null) {
            writer.name(I_DvTypeAdapter.AT_TYPE).value(RmConstants.DV_CODED_TEXT);
            writer.name("defining_code");

            writer.beginObject();
            writer.name("code_string").value(codeString);

            writer.name("terminology_id")
                    .beginObject()
                    .name("value")
                    .value(terminologyId)
                    .endObject();
            if (preferredTerm != null) {
                writer.name("preferred_term").value(preferredTerm);
            }
            writer.endObject();
        } else writer.name(I_DvTypeAdapter.AT_TYPE).value(RmConstants.DV_TEXT);

        writer.endObject();
    }
}
