/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.application.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.ehr.EhrStatus;
import org.ehrbase.api.mapper.StructuredStringJSonSerializer;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.serialisation.mapper.RmObjectJsonDeSerializer;
import org.ehrbase.openehr.sdk.serialisation.mapper.RmObjectJsonSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

@Configuration
public class JacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer addCustomSerialization() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(StructuredString.class, new StructuredStringJSonSerializer())
                .serializerByType(RMObject.class, new RmObjectJsonSerializer())
                .deserializerByType(EhrStatus.class, new RmObjectJsonDeSerializer())
                .deserializerByType(Folder.class, new RmObjectJsonDeSerializer())
                .modules(new JavaTimeModule());
    }

    @Bean
    @Primary
    public MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter(
            Jackson2ObjectMapperBuilder builder) {
        XmlMapper objectMapper = builder.createXmlMapper(true).build();
        objectMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        return new MappingJackson2XmlHttpMessageConverter(objectMapper);
    }
}
