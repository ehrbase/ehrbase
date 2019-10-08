/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.api.definitions;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
/**
 * Wrapper class for serializable data types in Ethercis. Attaches a format metadata parameter to a specific serialized content.
 *Allowed formats are specified by {@link StructuredStringFormat}
 */
@ApiModel(value = "String", description = "Raw Json or XML")
public class StructuredString {
    @ApiParam(hidden = true)
    private /*final*/ String content;
    @ApiParam(hidden = true)
    private /*final*/ StructuredStringFormat format;

    public StructuredString(String value, StructuredStringFormat format) {
        this.content = value;
        this.format = format;
    }

    public String getValue() {
        return content;
    }

    public void setValue(String value) {
        this.content = value;
    }

    public StructuredStringFormat getFormat() {
        return format;
    }

    public void setFormat(StructuredStringFormat format) {
        this.format = format;
    }
}
