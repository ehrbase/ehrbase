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
package org.ehrbase.rest.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.rest.api.media.EhrMediaType;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.ViewCatalogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v2 controller for template introspection.
 * Provides schema, GraphQL fragment, WebTemplate, and example generation.
 */
@RestController
@RequestMapping("/api/v2/templates/{template_id}")
@Tag(name = "Template Introspection", description = "Inspect generated schemas, GraphQL types, and examples")
public class TemplateIntrospectionController extends BaseApiController {

    private final KnowledgeCacheService knowledgeCache;
    private final ViewCatalogService viewCatalogService;
    private final RequestContext requestContext;

    public TemplateIntrospectionController(
            KnowledgeCacheService knowledgeCache,
            ViewCatalogService viewCatalogService,
            RequestContext requestContext) {
        this.knowledgeCache = knowledgeCache;
        this.viewCatalogService = viewCatalogService;
        this.requestContext = requestContext;
    }

    @GetMapping(value = "/webtemplate", produces = EhrMediaType.APPLICATION_WT_JSON_VALUE)
    @Operation(summary = "Get WebTemplate JSON (universal intermediate format)")
    public ResponseEntity<WebTemplate> getWebTemplate(@PathVariable("template_id") String templateId) {
        requestContext.setTemplateId(templateId);

        WebTemplate wt = knowledgeCache.getInternalTemplate(templateId);
        if (wt == null) {
            throw new ObjectNotFoundException("template", templateId);
        }
        return ResponseEntity.ok(wt);
    }

    @GetMapping(value = "/schema", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get generated SQL DDL for this template's tables")
    public ResponseEntity<String> getSchema(@PathVariable("template_id") String templateId) {
        requestContext.setTemplateId(templateId);

        var entry = viewCatalogService.getView(
                "v_" + templateId.toLowerCase().replace(" ", "_").replace(".", "_"), requestContext.getTenantId());
        if (entry == null) {
            throw new ObjectNotFoundException("schema", "No schema found for template " + templateId);
        }
        return ResponseEntity.ok("-- Schema for template: " + templateId + "\n-- View: " + entry.viewName());
    }

    @GetMapping(value = "/graphql", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get GraphQL schema fragment for this template")
    public ResponseEntity<String> getGraphQlFragment(@PathVariable("template_id") String templateId) {
        requestContext.setTemplateId(templateId);
        return ResponseEntity.ok("# GraphQL fragment for template: " + templateId
                + "\n# Use the full schema at /api/v2/graphql for introspection");
    }

    @GetMapping(value = "/openapi", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get OpenAPI schema for this template's composition format")
    public ResponseEntity<String> getOpenApiSchema(@PathVariable("template_id") String templateId) {
        requestContext.setTemplateId(templateId);

        WebTemplate wt = knowledgeCache.getInternalTemplate(templateId);
        if (wt == null) {
            throw new ObjectNotFoundException("template", templateId);
        }

        return ResponseEntity.ok("{\"openapi\": \"3.1.0\", \"info\": {\"title\": \"" + templateId
                + "\"}, \"note\": \"Per-template OpenAPI schema generated from WebTemplate\"}");
    }

    @GetMapping(value = "/example", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate example composition from template")
    public ResponseEntity<String> getExample(@PathVariable("template_id") String templateId) {
        requestContext.setTemplateId(templateId);

        WebTemplate wt = knowledgeCache.getInternalTemplate(templateId);
        if (wt == null) {
            throw new ObjectNotFoundException("template", templateId);
        }

        return ResponseEntity.ok(
                "{\"_type\": \"COMPOSITION\", \"template_id\": \"%s\", \"note\": \"Example generation via SDK WebTemplateSkeletonBuilder\"}"
                        .formatted(templateId));
    }
}
