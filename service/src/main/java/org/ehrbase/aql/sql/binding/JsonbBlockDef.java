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
package org.ehrbase.aql.sql.binding;

import org.jooq.Field;

/**
 * Created by christian on 2/17/2017.
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class JsonbBlockDef {
    private String path;
    private Field field;
    private String
            jsonPathRoot; // if set, use this root to extract the actual json struct from the result (f.e. /value in an
    // Element)

    public JsonbBlockDef(String path, Field field, String jsonPathRoot) {
        this.path = path;
        this.field = field;
        this.jsonPathRoot = jsonPathRoot;
    }

    public String getPath() {
        return path;
    }

    public Field getField() {
        return field;
    }

    public String getJsonPathRoot() {
        return jsonPathRoot;
    }
}
