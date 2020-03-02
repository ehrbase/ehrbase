/*
 * Modifications copyright (C) 2019 Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Nedap Healthcare
 * This file is part of Project Archie
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
package org.ehrbase.serialisation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nedap.archie.base.OpenEHRBase;
import com.nedap.archie.rm.ehr.EhrStatus;

import java.io.IOException;

/**
 * Class to obtain an ObjectMapper that works with both archie RM and AOM objects, serializing into
 * a JSON with openEHR-spec type names.
 * <p>
 * When a standard is agreed upon in the openEHR-specs, this format will likely change.
 * <p>
 * Created by pieter.bos on 30/06/16.
 */
public class JacksonUtil {

    //threadsafe, can be cached
    private volatile static ObjectMapper objectMapper;

    /**
     * Get an object mapper that works with Archie RM and AOM objects. It will be cached in a static variable for
     * performance reasons
     *
     * @return
     */
    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            configureObjectMapper(objectMapper, true);

        }
        return objectMapper;
    }

    /**
     * Configure an existing object mapper to work with Archie RM and AOM Objects.
     * Indentation is enabled. Feel free to disable again in your own code.
     *
     * @param objectMapper target mapper to configure
     * @param withCustomizers setting to include customizers for primary mapping
     */
    public static void configureObjectMapper(ObjectMapper objectMapper, boolean withCustomizers) {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
        objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        objectMapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //objectMapper.

        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL);

        TypeResolverBuilder typeResolverBuilder = new ArchieTypeResolverBuilder()
                .init(JsonTypeInfo.Id.NAME, new OpenEHRTypeNaming())
                .typeProperty("_type")
                .typeIdVisibility(true)
                .inclusion(JsonTypeInfo.As.PROPERTY);

        //@type is always allowed as an extra property, even if we don't expect it.
        objectMapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
                if (propertyName.equalsIgnoreCase("_type")) {
                    return true;
                }
                return super.handleUnknownProperty(ctxt, p, deserializer, beanOrClass, propertyName);
            }
        });

        objectMapper.setDefaultTyping(typeResolverBuilder);

        // should be included in normal cases, but not in the mapper used by the custom (de)serializers below, or they will loop forever
        if (withCustomizers) {
            // add module to allow custom deserializers
            SimpleModule module = new SimpleModule();
            module.addDeserializer(EhrStatus.class, new EhrStatusDeserializer(EhrStatus.class));
            objectMapper.registerModule(module);
        }
    }

    /**
     * TypeResolverBuilder that outputs type information for all RMObject classes, but not for java classes.
     * Otherwise, you get this for an arrayList: "ARRAY_LIST: []", while you would expect "[]" without type
     */
    static class ArchieTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {
        public ArchieTypeResolverBuilder() {
            super(ObjectMapper.DefaultTyping.NON_FINAL);
        }

        @Override
        public boolean useForType(JavaType t) {
            return (OpenEHRBase.class.isAssignableFrom(t.getRawClass()));
        }
    }

    /**
     * Custom deserializer for {@link EhrStatus} objects. Used to default the is_modifiable and is_queryable attributes
     * to `true`.
     */
    public static class EhrStatusDeserializer extends StdDeserializer<EhrStatus> {

        protected EhrStatusDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public EhrStatus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            final String IS_MODIFIABLE = "is_modifiable";
            final String IS_QUERYABLE = "is_queryable";

            JsonNode node = p.getCodec().readTree(p);

            // check for "is_modifiable", correct to default if necessary
            if (node.has(IS_MODIFIABLE)) {
                if (!node.get(IS_MODIFIABLE).isBoolean()) // is available but invalid value -> default to `true`
                    ((ObjectNode) node).put(IS_MODIFIABLE, true);
            } else  // not available at all -> default to `true`
                ((ObjectNode) node).put(IS_MODIFIABLE, true);

            // check for "is_queryable", correct to default if necessary
            if (node.has(IS_QUERYABLE)) {
                if (!node.get(IS_QUERYABLE).isBoolean()) // is available but invalid value -> default to `true`
                    ((ObjectNode) node).put(IS_QUERYABLE, true);
            } else  // not available at all -> default to `true`
                ((ObjectNode) node).put(IS_QUERYABLE, true);

            // continue with normal deserialization
            ObjectMapper objectMapper = new ObjectMapper();
            configureObjectMapper(objectMapper, false); // to prevent looping through this custom deserializer forever

            return objectMapper.readValue(node.toString(), EhrStatus.class);
        }
    }
}