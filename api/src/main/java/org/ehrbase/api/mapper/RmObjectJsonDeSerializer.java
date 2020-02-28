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

package org.ehrbase.api.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.nedap.archie.rm.RMObject;
import org.ehrbase.serialisation.CanonicalJson;
import org.ehrbase.serialisation.CanonicalXML;

import java.io.IOException;

public class RmObjectJsonDeSerializer extends StdDeserializer {

    public RmObjectJsonDeSerializer() {
        this(null);
    }

    public RmObjectJsonDeSerializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String value = p.readValueAsTree().toString();
        if (p instanceof FromXmlParser) {
            return new CanonicalXML().unmarshal(value, RMObject.class);
        } else {
            return new CanonicalJson().unmarshal(value, RMObject.class);
        }
    }
}
