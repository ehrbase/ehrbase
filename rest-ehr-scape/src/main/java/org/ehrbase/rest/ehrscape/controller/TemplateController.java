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

package org.ehrbase.rest.ehrscape.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.Objects;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.rest.ehrscape.responsedata.Action;
import org.ehrbase.rest.ehrscape.responsedata.Meta;
import org.ehrbase.rest.ehrscape.responsedata.RestHref;
import org.ehrbase.rest.ehrscape.responsedata.TemplateExampleResponseData;
import org.ehrbase.rest.ehrscape.responsedata.TemplateResponseData;
import org.ehrbase.rest.ehrscape.responsedata.TemplatesResponseData;
import org.ehrbase.webtemplate.filter.Filter;
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

@RestController
@RequestMapping(
    path = "/rest/ecis/v1/template",
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Api(value = "Template Endpoint")
public class TemplateController extends BaseController {

  private final TemplateService templateService;

  @Autowired
  public TemplateController(TemplateService templateService) {
    this.templateService = Objects.requireNonNull(templateService);
  }

  @GetMapping()
  @ApiOperation(value = "Get a List of  templates")
  public ResponseEntity<TemplatesResponseData> getTemplate() {
    TemplatesResponseData responseData = new TemplatesResponseData();
    responseData.setAction(Action.LIST);
    responseData.setTemplates(templateService.getAllTemplates());
    return ResponseEntity.ok(responseData);
  }

  @PostMapping()
  @ApiOperation(value = "Create a template")
  public ResponseEntity<TemplatesResponseData> createTemplate(@RequestBody() String content) {
    templateService.create(content);
    TemplatesResponseData responseData = new TemplatesResponseData();
    responseData.setAction(Action.LIST);
    responseData.setTemplates(templateService.getAllTemplates());
    return ResponseEntity.ok(responseData);
  }

  @GetMapping(path = "/{templateId}/example")
  @ApiOperation(value = "Get an example for a template")
  public ResponseEntity<TemplateExampleResponseData> getTemplateExample(
      @PathVariable(value = "templateId") String templateId,
      @ApiParam(value = "Format of the example", allowableValues = "XML, ECISFLAT, FLAT")
          @RequestParam(value = "format", defaultValue = "XML")
          CompositionFormat format) {

    TemplateExampleResponseData responseData = new TemplateExampleResponseData();
    responseData.setComposition(templateService.buildExample(templateId, format));
    responseData.setAction(Action.RETRIEVE);
    RestHref url = new RestHref();
    url.setUrl(getBaseEnvLinkURL() + "/rest/ecis/v1/template" + templateId + "/example");
    Meta meta = new Meta();
    meta.setHref(url);
    responseData.setMeta(meta);
    return ResponseEntity.ok(responseData);
  }

  @GetMapping(path = "/{templateId}")
  @ApiOperation(value = "Get a template by templateId")
  public ResponseEntity<TemplateResponseData> getTemplate(
      @PathVariable(value = "templateId") String templateId) {
    TemplateResponseData responseData = new TemplateResponseData();
    responseData.setWebTemplate(new Filter().filter(templateService.findTemplate(templateId)));
    responseData.setAction(Action.RETRIEVE);
    RestHref url = new RestHref();
    url.setUrl(getBaseEnvLinkURL() + "/rest/ecis/v1/template" + templateId + "/");
    Meta meta = new Meta();
    meta.setHref(url);
    responseData.setMeta(meta);
    return ResponseEntity.ok(responseData);
  }
}
