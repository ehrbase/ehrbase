/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.application.config;

import com.nedap.archie.rm.datastructures.DataStructure;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig extends WebMvcConfigurerAdapter {

    // Temporary removal of old EhrScape endpoints from swagger UI
    /*@Bean
    public Docket swaggerSpringMvcPlugin() {
        return new Docket(DocumentationType.SWAGGER_2).groupName("EhrScape API")
                //.host("http://localhost:8080")
                .select()
                .apis(RequestHandlerSelectors.basePackage("org.ehrbase.rest.ehrscape.controller"))
                .paths(PathSelectors.any())
                .build()
                .ignoredParameterTypes(RMObject.class)
                .apiInfo(metaDataEhrScape());
    }

    private ApiInfo metaDataEhrScape() {
        return new ApiInfo(
                "EhrScape openEHR REST API",
                "EhrScape openEHR REST API",
                "1.0",
                "Terms of service",
                new Contact("", "", ""),
                "Apache License Version 2.0",
                "https://www.apache.org/licenses/LICENSE-2.0", Collections.emptyList());
    }*/

    @Bean
    public Docket swaggerSpringMvcPluginOpenEhr() {

        // Add classes from ReferenceModel to be ignored if causing errors
        Class[] classesToIgnore = {EhrStatus.class, Folder.class, PartySelf.class, ItemStructure.class, DvText.class};

        return new Docket(DocumentationType.SWAGGER_2).groupName("openEHR API")
                //.host("http://localhost:8080")
                .select()
                .apis(RequestHandlerSelectors.basePackage("org.ehrbase.rest.openehr.controller"))
                .build()
                .ignoredParameterTypes(classesToIgnore)
                .directModelSubstitute(ResponseEntity.class, java.lang.Void.class)
                .tags(  // Tag name: Short name;
                        // description: From {EHR, Query, Definition} API,
                        // - name of resource;
                        // version of openEHR API this implementation is based on
                        new Tag("EHR", "EHR API - EHR Resource (v1.0)"),
                        new Tag("EHR_STATUS", "EHR API - EHR_STATUS Resource (WIP v1.0.1)"),
                        new Tag("Composition", "EHR API - Composition Resource (v1.0)"),
                        new Tag("Template", "Definitions API - Template Resource (WIP v1.0.1)"),
                        new Tag("Directory", "EHR API - Directory Resource (v1.0)"),
                        new Tag("Query", "Query API - Query Resource (WIP v1.0.1)"),
                        new Tag("Stored Query", "Definitions API - Stored Query Resource (WIP v1.0.1)"),
                        new Tag("Contribution", "EHR API - Contribution Resource (WIP v1.0.1)"))
                .apiInfo(metaDataOpenEhr());
    }

    private ApiInfo metaDataOpenEhr() {
        return new ApiInfo(
                "openEHR REST API",
                "openEHR REST API",
                "1.0",
                "Terms of service",
                new Contact("", "", ""),
                "Apache License Version 2.0",
                "https://www.apache.org/licenses/LICENSE-2.0", Collections.emptyList());
    }
}
