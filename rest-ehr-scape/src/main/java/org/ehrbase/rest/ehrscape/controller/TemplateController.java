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
package org.ehrbase.rest.ehrscape.controller;

import static org.ehrbase.rest.ehrscape.controller.BaseController.API_ECIS_CONTEXT_PATH_WITH_VERSION;
import static org.ehrbase.rest.ehrscape.controller.BaseController.TEMPLATE;

import com.nedap.archie.rm.composition.Composition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.rest.ehrscape.responsedata.Action;
import org.ehrbase.rest.ehrscape.responsedata.Meta;
import org.ehrbase.rest.ehrscape.responsedata.RestHref;
import org.ehrbase.rest.ehrscape.responsedata.TemplateResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnMissingBean(name = "primarytemplatecontroller")
@RestController
@RequestMapping(
        path = API_ECIS_CONTEXT_PATH_WITH_VERSION + "/" + TEMPLATE,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "TEMPLATE")
public class TemplateController extends BaseController {

    private final TemplateService templateService;
    private final CompositionService compositionService;

    @Autowired
    public TemplateController(TemplateService templateService, CompositionService compositionService) {
        this.templateService = Objects.requireNonNull(templateService);
        this.compositionService = Objects.requireNonNull(compositionService);
    }

    @GetMapping(path = "/{templateId}/example")
    @Operation(
            summary = "Deprecated since 2.0.0 and marked for removal",
            description =
                    "Replaced by [/rest/openehr/v1/definition/template/adl1.4/{template_id}/example](./index.html?urls.primaryName=1.%20openEHR%20API#/ADL%201.4%20TEMPLATE/getTemplateExample)",
            deprecated = true)
    @Deprecated(since = "2.0.0", forRemoval = true)
    public ResponseEntity<String> getTemplateExample(
            @PathVariable(value = "templateId") String templateId,
            @RequestParam(value = "format", defaultValue = "FLAT") CompositionFormat format) {

        if ((format == CompositionFormat.RAW
                || format == CompositionFormat.EXPANDED
                || format == CompositionFormat.ECISFLAT)) {
            throw new InvalidApiParameterException(String.format("Format %s not supported", format));
        }

        Composition composition = templateService.buildExample(templateId);
        CompositionDto compositionDto = new CompositionDto(composition, templateId, null, null);
        StructuredString serialized = compositionService.serialize(compositionDto, format, true);

        MediaType contentType =
                format == CompositionFormat.XML ? MediaType.APPLICATION_XML : MediaType.APPLICATION_JSON;
        return ResponseEntity.ok()
                .headers(deprecationHeaders("TEMPLATE/getTemplateExample", "ADL 1.4 TEMPLATE/getTemplateExample"))
                .contentType(contentType)
                .body(serialized.getValue());
    }

    @GetMapping(path = "/{templateId}")
    @Operation(
            summary = "Deprecated since 2.0.0 and marked for removal",
            description =
                    "Replaced by [/rest/openehr/v1/definition/template/adl1.4/{template_id}/webtemplate](./index.html?urls.primaryName=1.%20openEHR%20API#/TEMPLATE/getWebTemplate). "
                            + "Note the replacement endpoint provides the Web-Template as it is, without wrapping it in a `webTemplate` property.",
            deprecated = true)
    @Deprecated(since = "2.0.0", forRemoval = true)
    public ResponseEntity<TemplateResponseData> getTemplate(@PathVariable(value = "templateId") String templateId) {
        TemplateResponseData responseData = new TemplateResponseData();
        responseData.setWebTemplate(templateService.findWebTemplate(templateId));
        responseData.setAction(Action.RETRIEVE);
        RestHref url = new RestHref();
        url.setUrl(createLocationUri(TEMPLATE, templateId));
        Meta meta = new Meta();
        meta.setHref(url);
        responseData.setMeta(meta);
        return ResponseEntity.ok()
                .headers(deprecationHeaders("TEMPLATE/getTemplate", "TEMPLATE/getWebTemplate"))
                .body(responseData);
    }
}
