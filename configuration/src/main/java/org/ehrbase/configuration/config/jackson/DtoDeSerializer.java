/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Objects;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;

/**
 * Deserializer that allows to use arbitrary object that contain <code>RmObjects</code> as parameters. Can be used
 * for custom DTOs.
 * @param <T> Of the DTO to deserialize.
 */
class DtoDeSerializer<T> extends StdDeserializer<T> {

    private final String typeNode;

    public DtoDeSerializer(Class<T> vc, String type) {
        super(vc);
        this.typeNode = "\"%s\"".formatted(type);
    }

    @SuppressWarnings("unchecked")
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        // rad as nodes
        TreeNode root = parser.readValueAsTree();
        validateType(parser, root);
        // return interpretation as handled type
        return (T) CanonicalJson.MARSHAL_OM.convertValue(root, handledType());
    }

    private void validateType(JsonParser p, TreeNode root) throws JsonParseException {
        TreeNode type = root.get("_type");
        if (type == null) {
            throw new JsonParseException(p, "Missing [_type] value");
        } else if (!Objects.equals(type.toString(), typeNode)) {
            throw new JsonParseException(
                    p, "Unexpected [_type] value [%s] not matching [%s]".formatted(type, typeNode));
        }
    }
}
