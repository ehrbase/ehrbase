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
package org.ehrbase.configuration.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.ehrbase.configuration.config.jackson.JacksonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.UrlHandlerFilter;

class WebConfigurationTest {

    private final WebConfiguration webConfiguration = new WebConfiguration(new CorsProperties());

    private final UrlHandlerFilter trailingSlashFilter = webConfiguration.trailingSlashFilter();

    private HttpServletRequest downstreamRequestOf(String requestUri) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setServletPath(requestUri);
        MockFilterChain chain = new MockFilterChain();
        trailingSlashFilter.doFilter(request, new MockHttpServletResponse(), chain);
        return (HttpServletRequest) chain.getRequest();
    }

    @Test
    void trailingSlashIsTrimmedBeforeDispatch() throws Exception {
        HttpServletRequest downstream = downstreamRequestOf("/rest/openehr/v1/ehr/");
        assertThat(downstream.getRequestURI()).isEqualTo("/rest/openehr/v1/ehr");
        assertThat(downstream.getServletPath()).isEqualTo("/rest/openehr/v1/ehr");
    }

    @Test
    void rootPathIsNotTrimmed() throws Exception {
        assertThat(downstreamRequestOf("/").getRequestURI()).isEqualTo("/");
    }

    @Test
    void jacksonConvertersMoveBehindTheStringConverterWithXmlLast() {
        HttpMessageConverter<?> xml = new MappingJackson2XmlHttpMessageConverter();
        HttpMessageConverter<?> customJson = new MappingJackson2HttpMessageConverter();
        HttpMessageConverter<?> byteArray = new ByteArrayHttpMessageConverter();
        HttpMessageConverter<?> string = new StringHttpMessageConverter();
        List<HttpMessageConverter<?>> converters = new ArrayList<>(List.of(xml, customJson, byteArray, string));

        webConfiguration.extendMessageConverters(converters);

        assertThat(converters).containsExactly(byteArray, string, customJson, xml);
    }

    @Test
    void rmObjectXmlConverterIsNotMoved() {
        HttpMessageConverter<?> rmObjectXml = new JacksonConfiguration.RmObjectXmlHttpMessageConverter();
        HttpMessageConverter<?> xml = new MappingJackson2XmlHttpMessageConverter();
        HttpMessageConverter<?> json = new MappingJackson2HttpMessageConverter();
        List<HttpMessageConverter<?>> converters = new ArrayList<>(List.of(rmObjectXml, xml, json));

        webConfiguration.extendMessageConverters(converters);

        assertThat(converters).containsExactly(rmObjectXml, json, xml);
    }
}
