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
package org.ehrbase.rest;

import com.nimbusds.jose.util.Pair;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(basePackages = {"org.ehrbase.rest.admin", "org.ehrbase.rest.openehr", "org.ehrbase.rest.status"})
@EnableAspectJAutoProxy
public class RestModuleConfiguration implements WebMvcConfigurer {
    public static final String NONE = "none";

    @Value("${security.auth-type}")
    private String authType;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (NONE.equalsIgnoreCase(authType)) {
            registry.addInterceptor(new SecurityContextCleanupInterceptor());
        }
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new QueryParameterConverter());
    }

    public static class SecurityContextCleanupInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            SecurityContextHolder.clearContext();
            return true;
        }
    }

    public static class QueryParameterConverter implements Converter<String, Map<String, Object>> {
        private static final String PARAM_ASSIGN = "=";
        private static final String PARAM_DELIM = "&";

        // x=1&ehrId=b907e17a-0dc0-49ef-b126-95b9abb4f906
        @Override
        public Map<String, Object> convert(String source) {

            StringTokenizer tokenizer = new StringTokenizer(source, PARAM_DELIM);
            Spliterator<Object> spliterator = Spliterators.spliterator(tokenizer.asIterator(), 1, Spliterator.ORDERED);

            return StreamSupport.stream(spliterator, false)
                    .map(t -> (String) t)
                    .map(str -> {
                        String[] split = str.split(PARAM_ASSIGN, 2);
                        return switch (split.length) {
                            case 1 -> Pair.of(split[0], "");
                            case 2 -> Pair.of(split[0], split[1]);
                            default -> Pair.of("", "");
                        };
                    })
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        }
    }
}
