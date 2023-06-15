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

public class NameValue {

    private final I_NameValueHandler handler;

    NameValue(JsonWriter writer, String value) {
        this.handler = new DvTextNameValue(writer, value);
    }

    NameValue(JsonWriter writer, LinkedTreeMap value) {
        if (value.containsKey("defining_code")) {
            this.handler = new DvCodedTextNameValue(writer, value);
        } else this.handler = new DvTextNameValue(writer, value);
    }

    /**
     * Encode a name value into the DB json structure
     * <code>
     * "name": {
     * "value":...
     * }
     * </code>
     * @throws IOException
     */
    public void write() throws IOException {
        handler.write();
    }
}
