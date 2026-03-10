/*
 * Copyright (c) 2026 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.configuration.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.ehr.EhrStatus;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.ehrbase.api.exception.ValidationException;

public class EhrStatusDeserializer extends JsonDeserializer<EhrStatus> {

    private final ObjectMapper mapper;

    public EhrStatusDeserializer(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EhrStatus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode ehrStatusNode = ctxt.readTree(p);

        Optional<String> missingProp = Stream.of("subject", "is_queryable", "is_modifiable")
                .filter(a -> !ehrStatusNode.has(a))
                .findAny()
                // "Missing attribute EHR_STATUS/%s"
                .stream()
                .findAny();
        if (missingProp.isPresent()) {
            throw new ValidationException("Missing attribute EHR_STATUS/%s".formatted(missingProp.get()));
        }

        return mapper.treeToValue(ehrStatusNode, EhrStatus.class);
    }
}
