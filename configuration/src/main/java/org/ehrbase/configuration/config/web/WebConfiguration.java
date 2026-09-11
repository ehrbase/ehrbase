/*
 * Copyright (c) 2024 vitasystems GmbH.
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

import java.util.Comparator;
import java.util.List;
import org.ehrbase.configuration.util.IsoDateTimeConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.AbstractXmlHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link Configuration} from Spring Web MVC.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfiguration implements WebMvcConfigurer {

    private final CorsProperties properties;

    public WebConfiguration(CorsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new IsoDateTimeConverter()); // Converter for version_at_time and other ISO date params
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").combine(properties.toCorsConfiguration());
    }

    /**
     * Keeps {@code /x/} reaching the handler for {@code /x} (Boot 3 behavior). Must stay at default
     * filter order so that Security still evaluates the path as sent.
     */
    @Bean
    public UrlHandlerFilter trailingSlashFilter() {
        return UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build();
    }

    /**
     * Reorders the converters by {@link #converterRank(HttpMessageConverter)}. The sort is stable, so
     * converters of the same rank keep their registration order.
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.sort(Comparator.comparingInt(WebConfiguration::converterRank));
    }

    /**
     * The order to maintain is: all other converters (byte[], String, ...), then Jackson JSON, then XML.
     * Spring Boot registers custom converter beans ahead of the defaults, which would otherwise put a JSON
     * converter in front of the String one. Clients accepting any media type must negotiate JSON before XML.
     *
     * @return the ordering rank of the converter
     */
    private static int converterRank(HttpMessageConverter<?> converter) {
        if (converter instanceof AbstractXmlHttpMessageConverter
                || converter instanceof MappingJackson2XmlHttpMessageConverter
                || converter instanceof JacksonXmlHttpMessageConverter) {
            return 2;
        }

        if (converter instanceof AbstractJackson2HttpMessageConverter
                || converter instanceof AbstractJacksonHttpMessageConverter) {
            return 1;
        }
        return 0;
    }
}
