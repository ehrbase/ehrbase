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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.ehr.EhrStatus;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.ehrbase.api.mapper.StructuredStringJSonSerializer;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.serialisation.mapper.RmObjectJsonDeSerializer;
import org.ehrbase.openehr.sdk.serialisation.mapper.RmObjectJsonSerializer;
import org.ehrbase.openehr.sdk.serialisation.xmlencoding.CanonicalXML;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

@Configuration
public class JacksonConfiguration {

    @Bean
    public com.fasterxml.jackson.databind.Module ehrbaseJacksonModule() {
        var module = new com.fasterxml.jackson.databind.module.SimpleModule("ehrbase-rm");
        module.addSerializer(StructuredString.class, new StructuredStringJSonSerializer());
        module.addSerializer(RMObject.class, new RmObjectJsonSerializer());
        module.addDeserializer(Folder.class, new RmObjectJsonDeSerializer());
        module.addDeserializer(EhrStatus.class, new EhrStatusDeserializer(CanonicalJson.MARSHAL_OM));
        module.addDeserializer(ContributionCreateDto.class, new ContributionCreateDtoDeserializer());
        return module;
    }

    @Bean
    @Primary
    public MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter(
            Jackson2ObjectMapperBuilder builder) {
        XmlMapper objectMapper = builder.createXmlMapper(true).build();
        objectMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        return new MappingJackson2XmlHttpMessageConverter(objectMapper);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RmObjectXmlHttpMessageConverter rmObjectXmlHttpMessageConverter() {
        return new RmObjectXmlHttpMessageConverter();
    }

    public static class RmObjectXmlHttpMessageConverter extends AbstractHttpMessageConverter<RMObject> {

        public RmObjectXmlHttpMessageConverter() {
            super(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
        }

        @Override
        protected boolean supports(final Class<?> clazz) {
            return RMObject.class.isAssignableFrom(clazz);
        }

        @Override
        protected RMObject readInternal(final Class<? extends RMObject> clazz, final HttpInputMessage inputMessage)
                throws IOException, HttpMessageNotReadableException {
            Charset charset = Optional.of(inputMessage)
                    .map(HttpInputMessage::getHeaders)
                    .map(HttpHeaders::getContentType)
                    .map(MediaType::getCharset)
                    .orElse(StandardCharsets.UTF_8);
            return CanonicalXML.DEFAULT_INSTANCE.unmarshal(IOUtils.toString(inputMessage.getBody(), charset), clazz);
        }

        @Override
        protected void writeInternal(final RMObject rmObject, final HttpOutputMessage outputMessage)
                throws IOException, HttpMessageNotWritableException {
            String marshal = CanonicalXML.DEFAULT_INSTANCE.marshal(rmObject, true);
            IOUtils.write(marshal, outputMessage.getBody(), StandardCharsets.UTF_8);
        }
    }
}
