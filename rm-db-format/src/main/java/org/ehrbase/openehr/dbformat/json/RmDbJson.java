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
package org.ehrbase.openehr.dbformat.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.nedap.archie.base.OpenEHRBase;
import java.io.IOException;
import org.ehrbase.openehr.dbformat.DbToRmFormat;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;

public class RmDbJson {

    private RmDbJson() {}

    public static final ObjectMapper MARSHAL_OM = CanonicalJson.MARSHAL_OM
            .copy()
            .registerModule(rmDbJacksonModule())
            .setDefaultTyping(OpenEHRBaseTypeResolverBuilder.build());

    static final Base64Variant BASE_64_VARIANT = Base64Variants.MIME_NO_LINEFEEDS;

    /**
     * Jackson type resolver builder that add Handling for {@link OpenEHRBase} types based on the <code>_type</code>
     * property.
     */
    private static class OpenEHRBaseTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {

        public OpenEHRBaseTypeResolverBuilder() {
            super(ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS, LaissezFaireSubTypeValidator.instance);
        }

        @Override
        public boolean useForType(JavaType t) {
            return OpenEHRBase.class.isAssignableFrom(t.getRawClass());
        }

        @SuppressWarnings("rawtypes")
        public static TypeResolverBuilder build() {
            return new OpenEHRBaseTypeResolverBuilder()
                    .init(JsonTypeInfo.Id.NAME, new CanonicalJson.CJOpenEHRTypeNaming())
                    .typeProperty(DbToRmFormat.TYPE_ATTRIBUTE)
                    .typeIdVisibility(true)
                    .inclusion(JsonTypeInfo.As.PROPERTY);
        }
    }

    /**
     * Custom serialize that explicitly uses Base64 encoding without a linefeed. This ensures that the DB format is
     * consistently read also in case the Jackson default is changed.
     *
     * Note byte arrays must be encoded before they can be inserted into the DB. That's why we apply a Base64 encoding.
     * Also in case the given json string was already encoded we apply another Base64 while mapping it from the RMObject
     * byte array to be 100% sure we don't insert invalid characters into the DB JSONB field.
     */
    private static class Base64NoLinefeedByteArraySerializer extends StdSerializer<byte[]> {

        Base64NoLinefeedByteArraySerializer() {
            super(byte[].class);
        }

        @Override
        public void serialize(byte[] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeBinary(BASE_64_VARIANT, value, 0, value.length);
        }
    }

    /**
     * Custom deserialize that explicitly uses Base64 decoding without a linefeed. This ensures that the DB format is
     * consistently written also in case the Jackson default is changed.
     *
     * Note byte arrays must be encoded before they can be inserted into the DB. That's why we apply a Base64 encoding.
     * Also in case the given json string was already encoded we apply another Base64 while mapping it from the RMObject
     * byte array to be 100% sure we don't insert invalid characters into the DB JSONB field.
     */
    private static class Base64NoLinefeedByteArrayDeserializer extends StdScalarDeserializer<byte[]> {

        private final StringDeserializer stringDeserializer = new StringDeserializer();

        Base64NoLinefeedByteArrayDeserializer() {
            super(byte[].class);
        }

        @Override
        public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            String encoded = stringDeserializer.deserialize(p, ctxt);
            return BASE_64_VARIANT.decode(encoded);
        }
    }

    private static SimpleModule rmDbJacksonModule() {

        SimpleModule module = new SimpleModule();
        module.addSerializer(new Base64NoLinefeedByteArraySerializer());
        module.addDeserializer(byte[].class, new Base64NoLinefeedByteArrayDeserializer());
        return module;
    }
}
