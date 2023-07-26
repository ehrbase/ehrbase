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
package org.ehrbase.rest.ehrscape.controller;

import static org.ehrbase.rest.ehrscape.controller.BaseController.API_ECIS_CONTEXT_PATH_WITH_VERSION;
import static org.ehrbase.rest.ehrscape.controller.BaseController.TEMPLATE;

import com.nedap.archie.rm.composition.Composition;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.webtemplate.filter.Filter;
import org.ehrbase.rest.ehrscape.responsedata.Action;
import org.ehrbase.rest.ehrscape.responsedata.Meta;
import org.ehrbase.rest.ehrscape.responsedata.RestHref;
import org.ehrbase.rest.ehrscape.responsedata.TemplateResponseData;
import org.ehrbase.rest.ehrscape.responsedata.TemplatesResponseData;
import org.openehr.schemas.v1.TemplateDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@TenantAware
@RestController
@RequestMapping(
        path = API_ECIS_CONTEXT_PATH_WITH_VERSION + "/" + TEMPLATE,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class TemplateController extends BaseController {

    private final TemplateService templateService;
    private final CompositionService compositionService;

    @Autowired
    public TemplateController(TemplateService templateService, CompositionService compositionService) {
        this.templateService = Objects.requireNonNull(templateService);
        this.compositionService = Objects.requireNonNull(compositionService);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_READ)
    @GetMapping()
    public ResponseEntity<TemplatesResponseData> getTemplate() {
        TemplatesResponseData responseData = new TemplatesResponseData();
        responseData.setAction(Action.LIST);
        responseData.setTemplates(templateService.getAllTemplates());
        return ResponseEntity.ok(responseData);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_CREATE)
    @PostMapping()
    public ResponseEntity<TemplatesResponseData> createTemplate(@RequestBody() String content) {

        TemplateDocument document;
        try {
            document =
                    TemplateDocument.Factory.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (XmlException | IOException e) {
            throw new InvalidApiParameterException(e.getMessage());
        }

        templateService.create(document.getTemplate());
        TemplatesResponseData responseData = new TemplatesResponseData();
        responseData.setAction(Action.LIST);
        responseData.setTemplates(templateService.getAllTemplates());
        return ResponseEntity.ok(responseData);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_EXAMPLE)
    @GetMapping(path = "/{templateId}/example")
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
        StructuredString serialized = compositionService.serialize(compositionDto, format);

        MediaType contentType =
                format == CompositionFormat.XML ? MediaType.APPLICATION_XML : MediaType.APPLICATION_JSON;
        return ResponseEntity.ok().contentType(contentType).body(serialized.getValue());
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_READ)
    @GetMapping(path = "/{templateId}")
    public ResponseEntity<TemplateResponseData> getTemplate(@PathVariable(value = "templateId") String templateId) {
        TemplateResponseData responseData = new TemplateResponseData();
        responseData.setWebTemplate(new Filter().filter(templateService.findTemplate(templateId)));
        responseData.setAction(Action.RETRIEVE);
        RestHref url = new RestHref();
        url.setUrl(createLocationUri(TEMPLATE, templateId));
        Meta meta = new Meta();
        meta.setHref(url);
        responseData.setMeta(meta);
        return ResponseEntity.ok(responseData);
    }
}
