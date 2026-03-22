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
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.api.knowledge.TemplateMetaData;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.rest.api.dto.TemplateResponseDto;
import org.ehrbase.rest.api.media.EhrMediaType;
import org.ehrbase.service.RequestContext;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v2 controller for template management.
 * Supports ADL 1.4 OPT XML upload/retrieval and ADL 2.4 stubs (501).
 */
@RestController
@RequestMapping("/api/v2/templates")
@Tag(name = "Template", description = "Template upload, listing, retrieval, and deletion")
public class TemplateController extends BaseApiController {

    private final KnowledgeCacheService knowledgeCache;
    private final TemplateService templateService;
    private final org.ehrbase.service.SchemaExecutorService schemaExecutor;
    private final RequestContext requestContext;

    public TemplateController(
            KnowledgeCacheService knowledgeCache,
            TemplateService templateService,
            org.ehrbase.service.SchemaExecutorService schemaExecutor,
            RequestContext requestContext) {
        this.knowledgeCache = knowledgeCache;
        this.templateService = templateService;
        this.schemaExecutor = schemaExecutor;
        this.requestContext = requestContext;
    }

    // ==================== ADL 1.4 ====================

    @PostMapping(
            value = "/adl1.4",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Upload ADL 1.4 OPT XML template",
            description = "Stores template, generates normalized tables in ehr_data, creates views in ehr_views, "
                    + "registers in schema_registry and view_catalog, refreshes GraphQL schema")
    public ResponseEntity<java.util.Map<String, Object>> uploadAdl14(InputStream body) {

        // 1. Parse OPT XML using XmlBeans (not Jackson)
        OPERATIONALTEMPLATE template;
        try {
            TemplateDocument document = TemplateDocument.Factory.parse(body);
            template = document.getTemplate();
        } catch (Exception e) {
            throw new InvalidApiParameterException("Invalid OPT XML: " + e.getMessage());
        }

        // 2. Store template in knowledge cache
        String templateId = knowledgeCache.addOperationalTemplate(template);
        requestContext.setTemplateId(templateId);

        // 2. Resolve template UUID
        java.util.UUID templateUuid = knowledgeCache
                .findUuidByTemplateId(templateId)
                .orElseThrow(() -> new ObjectNotFoundException("template", templateId));

        // 3. Run full schema generation pipeline
        String tableName =
                schemaExecutor.executeSchemaGeneration(templateId, templateUuid, requestContext.getTenantId());

        URI location = locationUri("api", "v2", "templates", "adl1.4", templateId);

        OPERATIONALTEMPLATE stored = knowledgeCache
                .retrieveOperationalTemplate(templateId)
                .orElseThrow(() -> new ObjectNotFoundException("template", templateId));

        return ResponseEntity.created(location)
                .body(java.util.Map.of(
                        "templateId",
                        templateId,
                        "concept",
                        stored.getConcept() != null ? stored.getConcept() : "",
                        "tables",
                        java.util.List.of("ehr_data." + tableName),
                        "status",
                        "active"));
    }

    @GetMapping(value = "/adl1.4", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all ADL 1.4 templates")
    public ResponseEntity<List<TemplateResponseDto>> listAdl14Templates() {
        List<TemplateMetaData> templates = knowledgeCache.listAllOperationalTemplates();

        List<TemplateResponseDto> dtos = templates.stream()
                .map(t -> {
                    OPERATIONALTEMPLATE opt = t.getOperationaltemplate();
                    return new TemplateResponseDto(
                            opt.getTemplateId().getValue(),
                            opt.getConcept(),
                            opt.getDefinition() != null
                                    ? opt.getDefinition().getArchetypeId().getValue()
                                    : null,
                            null);
                })
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/adl1.4/{template_id}", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Get ADL 1.4 template as OPT XML")
    public ResponseEntity<String> getAdl14AsXml(@PathVariable("template_id") String templateId) {
        requestContext.setTemplateId(templateId);

        String xml = templateService.findOperationalTemplate(templateId, OperationalTemplateFormat.XML);
        return ResponseEntity.ok(xml);
    }

    @GetMapping(value = "/adl1.4/{template_id}", produces = EhrMediaType.APPLICATION_WT_JSON_VALUE)
    @Operation(summary = "Get ADL 1.4 template as WebTemplate JSON")
    public ResponseEntity<WebTemplate> getAdl14AsWebTemplate(@PathVariable("template_id") String templateId) {
        requestContext.setTemplateId(templateId);

        WebTemplate wt = knowledgeCache.getInternalTemplate(templateId);
        if (wt == null) {
            throw new ObjectNotFoundException("template", templateId);
        }
        return ResponseEntity.ok(wt);
    }

    // ==================== ADL 2.4 (Stubs — Phase 11) ====================

    @PostMapping(value = "/adl2")
    @Operation(summary = "Upload ADL 2.4 template (not yet implemented)")
    public ResponseEntity<Void> uploadAdl2() {
        throw new UnsupportedOperationException("ADL 2.4 template upload deferred to Phase 11");
    }

    @GetMapping(value = "/adl2")
    @Operation(summary = "List ADL 2.4 templates (not yet implemented)")
    public ResponseEntity<Void> listAdl2() {
        throw new UnsupportedOperationException("ADL 2.4 template listing deferred to Phase 11");
    }

    @GetMapping(value = "/adl2/{template_id}/{version}")
    @Operation(summary = "Get ADL 2.4 template (not yet implemented)")
    public ResponseEntity<Void> getAdl2(
            @PathVariable("template_id") String templateId, @PathVariable("version") String version) {
        throw new UnsupportedOperationException("ADL 2.4 template retrieval deferred to Phase 11");
    }

    // ==================== Common ====================

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all templates (all ADL versions)")
    public ResponseEntity<List<TemplateResponseDto>> listAllTemplates() {
        return listAdl14Templates();
    }

    @DeleteMapping("/{template_id}")
    @Operation(summary = "Delete template (only if no compositions reference it)")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("template_id") String templateId) {
        requestContext.setTemplateId(templateId);

        OPERATIONALTEMPLATE opt = knowledgeCache
                .retrieveOperationalTemplate(templateId)
                .orElseThrow(() -> new ObjectNotFoundException("template", templateId));

        knowledgeCache.deleteOperationalTemplate(opt);
        return ResponseEntity.noContent().build();
    }
}
