/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.api.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.nedap.archie.rm.RMObject;
import org.ehrbase.serialisation.CanonicalJson;
import org.ehrbase.serialisation.CanonicalXML;

import javax.xml.namespace.QName;
import java.io.IOException;

public class RmObjectJsonSerializer extends JsonSerializer<RMObject> {
    @Override
    public void serialize(RMObject value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (gen instanceof ToXmlGenerator) {
            final QName qName;
            if (gen.getOutputContext().getCurrentName() != null) {
                qName = new QName(null, gen.getOutputContext().getCurrentName());
            } else {
                qName = new QName(null, gen.getOutputContext().getParent().getCurrentName());
            }
            gen.writeRawValue(new CanonicalXML().marshalInline(value, qName));
        } else {
            gen.writeRawValue(new CanonicalJson().marshal(value));
        }
    }
}
