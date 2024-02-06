/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.api.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import java.io.IOException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;

public class StructuredStringJSonSerializer extends JsonSerializer<StructuredString> {
    @Override
    public void serialize(StructuredString value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (jgen instanceof ToXmlGenerator) {
            switch (value.getFormat()) {
                case JSON:
                    jgen.writeObject(value.getValue());
                    break;
                case XML:
                    jgen.writeRawValue(value.getValue());
                    break;
                default:
                    throw new UnexpectedSwitchCaseException(value.getFormat());
            }
        } else {
            switch (value.getFormat()) {
                case XML:
                    jgen.writeObject(value.getValue().replace("\"", "\\\""));
                    break;
                case JSON:
                    jgen.writeRawValue(value.getValue());
                    break;
                default:
                    throw new UnexpectedSwitchCaseException(value.getFormat());
            }
        }
    }
}
