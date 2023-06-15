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
package org.ehrbase.rest.ehrscape.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.ehrbase.api.mapper.StructuredStringJSonSerializer;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredStringFormat;
import org.ehrbase.rest.ehrscape.responsedata.CompositionResponseData;
import org.junit.Assert;
import org.junit.Test;

public class StructuredStringJSonSerializerTest {

    @Test
    public void serialize() throws JsonProcessingException {
        SimpleModule mapperModule = new SimpleModule();
        mapperModule.addSerializer(StructuredString.class, new StructuredStringJSonSerializer());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(mapperModule);
        // JSON in JSON
        {
            CompositionResponseData responseData = new CompositionResponseData();
            StructuredString structuredString = new StructuredString("{\"test\":false}", StructuredStringFormat.JSON);
            responseData.setComposition(structuredString);

            String actual = objectMapper.writer().writeValueAsString(responseData);
            String expected =
                    "{\"meta\":null,\"action\":null,\"composition\":{\"test\":false},\"format\":null,\"templateId\":null,\"ehrId\":null,\"compositionUid\":null}";
            Assert.assertEquals(expected, actual);
        }

        // XML in JSON
        {
            CompositionResponseData responseData = new CompositionResponseData();
            StructuredString structuredString = new StructuredString("<test>Test<test>", StructuredStringFormat.XML);
            responseData.setComposition(structuredString);

            String actual = objectMapper.writer().writeValueAsString(responseData);
            String expected =
                    "{\"meta\":null,\"action\":null,\"composition\":\"<test>Test<test>\",\"format\":null,\"templateId\":null,\"ehrId\":null,\"compositionUid\":null}";
            Assert.assertEquals(expected, actual);
        }

        // JSON in XML
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(mapperModule);
        {
            CompositionResponseData responseData = new CompositionResponseData();
            StructuredString structuredString = new StructuredString("{\"test\":false}", StructuredStringFormat.JSON);
            responseData.setComposition(structuredString);

            String actual = xmlMapper.writer().writeValueAsString(responseData);
            String expected =
                    "<CompositionResponseData><meta/><action/><composition>{\"test\":false}</composition><format/><templateId/><ehrId/><compositionUid/></CompositionResponseData>";
            Assert.assertEquals(expected, actual);
        }

        // XML in XML
        {
            CompositionResponseData responseData = new CompositionResponseData();
            StructuredString structuredString = new StructuredString("<test>Test<test>", StructuredStringFormat.XML);
            responseData.setComposition(structuredString);

            String actual = xmlMapper.writer().writeValueAsString(responseData);
            String expected =
                    "<CompositionResponseData><meta/><action/><composition><test>Test<test></composition><format/><templateId/><ehrId/><compositionUid/></CompositionResponseData>";
            Assert.assertEquals(expected, actual);
        }
    }
}
