/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
 *
 * This file is part of project EHRbase
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

package org.ehrbase.rest.ehrscape.controller;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;

/**
 * Introducing a new annotation to swagger-ui to allow expression of markdown code examples.
 * Workaround for non-functional @Example annotation. See: https://github.com/springfox/springfox/issues/2822
 * // TODO check licence of blog post source
 * Source: https://blog.codecentric.de/2017/09/springfox-swagger-inkludieren-markdown/
 */
@Component
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER)
public class OperationNotesResourcesReader implements OperationBuilderPlugin {
    private final DescriptionResolver descriptions;

    final static Logger logger = LoggerFactory.getLogger(OperationNotesResourcesReader.class);

    @Autowired
    public OperationNotesResourcesReader(DescriptionResolver descriptions) {
        this.descriptions = descriptions;
    }

    /**
     * Custom @ApiNotes annotation to serve as annotation for markdown input.
     */
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ApiNotes {
        String value() default "";
    }

    @Override
    public void apply(OperationContext context) {
        Optional<ApiNotes> methodAnnotation = context.findAnnotation(ApiNotes.class);
        if (methodAnnotation.isPresent() && StringUtils.hasText(methodAnnotation.get().value())) {

            final String mdFile = methodAnnotation.get().value();
            URL url = Resources.getResource(mdFile);
            String text;
            try {
                text = Resources.toString(url, Charsets.UTF_8);
            } catch (IOException e) {
                logger.error("Error while reading markdown description file {}", mdFile, e);
                text = "Markdown file " + mdFile + " not loaded";
            }

            context.operationBuilder().notes(descriptions.resolve(text));
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }
}
