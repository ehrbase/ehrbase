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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.directory.Folder;
import javax.annotation.Nonnull;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.mapper.StructuredStringJSonSerializer;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.serialisation.mapper.RmObjectJsonDeSerializer;
import org.ehrbase.openehr.sdk.serialisation.mapper.RmObjectJsonSerializer;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.ehrbase.rest.BaseController;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;

@Configuration
public class JacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer addCustomSerialization() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(StructuredString.class, new StructuredStringJSonSerializer())
                // RMObject support
                .serializerByType(RMObject.class, new RmObjectJsonSerializer())
                .deserializerByType(Folder.class, new RmObjectJsonDeSerializer())
                // DTOs with RMObjects support
                .deserializers(new DtoDeSerializer<>(EhrStatusDto.class, RmConstants.EHR_STATUS))
                .modules(new JavaTimeModule());
    }

    @Bean
    @Primary
    public MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter(
            Jackson2ObjectMapperBuilder builder) {

        XmlMapper objectMapper = builder.createXmlMapper(true).build();
        objectMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        return new MappingJackson2XmlHttpMessageConverter(objectMapper) {

            @Override
            protected @Nonnull ObjectWriter customizeWriter(
                    @Nonnull ObjectWriter writer, JavaType javaType, MediaType contentType) {
                return customizeWriterForPrettyPrint(writer);
            }
        };
    }

    @Bean
    @Primary
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(
            Jackson2ObjectMapperBuilder builder) {

        return new MappingJackson2HttpMessageConverter(builder.build()) {

            @Override
            protected @Nonnull ObjectWriter customizeWriter(
                    @Nonnull ObjectWriter writer, JavaType javaType, MediaType contentType) {
                return customizeWriterForPrettyPrint(writer);
            }
        };
    }

    private static ObjectWriter customizeWriterForPrettyPrint(ObjectWriter writer) {
        final Object prettyPrint = RequestContextHolder.getRequestAttributes().getAttribute(BaseController.PRETTY, 0);
        if (Boolean.TRUE.equals(prettyPrint)) {
            return writer.withDefaultPrettyPrinter();
        }
        return writer;
    }
}
