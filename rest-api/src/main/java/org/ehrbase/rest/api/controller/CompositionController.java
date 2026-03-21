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

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.rest.api.format.FormatNegotiator;
import org.ehrbase.rest.api.media.EhrMediaType;
import org.ehrbase.service.RequestContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v1 controller for composition CRUD.
 * Supports all 4 openEHR formats: Canonical JSON, Canonical XML, Flat (simSDT), Structured (structSDT).
 * Content negotiation via Content-Type, Accept headers, and {@code ?format=} query parameter.
 */
@RestController
@RequestMapping("/api/v1/ehrs/{ehr_id}/compositions")
@Tag(name = "Composition", description = "Composition CRUD with multi-format support and versioning")
public class CompositionController extends BaseApiController {

    private final CompositionService compositionService;
    private final EhrService ehrService;
    private final SystemService systemService;
    private final RequestContext requestContext;

    public CompositionController(
            CompositionService compositionService,
            EhrService ehrService,
            SystemService systemService,
            RequestContext requestContext) {
        this.compositionService = compositionService;
        this.ehrService = ehrService;
        this.systemService = systemService;
        this.requestContext = requestContext;
    }

    @PostMapping(
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                EhrMediaType.APPLICATION_WT_FLAT_VALUE,
                EhrMediaType.APPLICATION_WT_STRUCTURED_VALUE
            },
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(
            summary = "Create composition",
            description = "Accepts Canonical JSON, Canonical XML, Flat, or Structured format")
    public ResponseEntity<Object> createComposition(
            @PathVariable("ehr_id") String ehrIdStr,
            @RequestBody String body,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
            @RequestParam(value = "format", required = false) String formatParam,
            @RequestParam(value = "templateId", required = false)
                    @Parameter(description = "Required for Flat and Structured formats")
                    String templateId,
            @RequestHeader(value = "Prefer", required = false) String prefer) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        FormatNegotiator.Format inputFormat = FormatNegotiator.resolveInput(contentType, formatParam);
        CompositionFormat sdkFormat = toSdkFormat(inputFormat);

        Composition composition = compositionService.buildComposition(body, sdkFormat, templateId);
        UUID compositionId = compositionService.create(ehrId, composition).orElseThrow();

        requestContext.setCompositionId(compositionId);
        String versionUid = compositionId + "::" + systemService.getSystemId() + "::1";
        URI location = locationUri("api", "v1", "ehrs", ehrId.toString(), "compositions", versionUid);

        if (preferRepresentation(prefer)) {
            StructuredString serialized = compositionService.serialize(composition, sdkFormat);
            return created(location, versionUid)
                    .contentType(inputFormat.mediaType())
                    .body(serialized.getValue());
        }
        return created(location, versionUid).build();
    }

    @GetMapping(
            value = "/{versioned_object_uid}",
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                EhrMediaType.APPLICATION_WT_FLAT_VALUE,
                EhrMediaType.APPLICATION_WT_STRUCTURED_VALUE
            })
    @Operation(summary = "Get composition", description = "Returns composition in requested format")
    public ResponseEntity<Object> getComposition(
            @PathVariable("ehr_id") String ehrIdStr,
            @PathVariable("versioned_object_uid") String versionedObjectUid,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader,
            @RequestParam(value = "format", required = false) String formatParam,
            @RequestParam(value = "version_at_time", required = false) String versionAtTime) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        UUID compositionId = extractVersionedObjectUid(versionedObjectUid);
        requestContext.setCompositionId(compositionId);

        Integer version = null;
        if (versionedObjectUid.contains("::")) {
            version = extractVersion(versionedObjectUid);
        }

        if (versionAtTime != null) {
            OffsetDateTime timestamp = parseVersionAtTime(versionAtTime);
            version = compositionService.getVersionByTimestamp(compositionId, timestamp);
        }

        Optional<Composition> composition = compositionService.retrieve(ehrId, compositionId, version);
        if (composition.isEmpty()) {
            if (compositionService.isDeleted(ehrId, compositionId, version)) {
                throw new org.ehrbase.api.exception.GeneralRequestProcessingException("Composition has been deleted");
            }
            throw new ObjectNotFoundException("composition", compositionId.toString());
        }

        FormatNegotiator.Format outputFormat = FormatNegotiator.resolveOutput(acceptHeader, formatParam);
        CompositionFormat sdkFormat = toSdkFormat(outputFormat);
        StructuredString serialized = compositionService.serialize(composition.get(), sdkFormat);

        int currentVersion = version != null ? version : compositionService.getLastVersionNumber(ehrId, compositionId);
        String etag = compositionId + "::" + systemService.getSystemId() + "::" + currentVersion;

        return ok(etag).contentType(outputFormat.mediaType()).body(serialized.getValue());
    }

    @PutMapping(
            value = "/{versioned_object_uid}",
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                EhrMediaType.APPLICATION_WT_FLAT_VALUE,
                EhrMediaType.APPLICATION_WT_STRUCTURED_VALUE
            },
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Update composition", description = "Requires If-Match header for optimistic locking")
    public ResponseEntity<Object> updateComposition(
            @PathVariable("ehr_id") String ehrIdStr,
            @PathVariable("versioned_object_uid") String versionedObjectUid,
            @RequestBody String body,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
            @RequestHeader("If-Match") String ifMatch,
            @RequestParam(value = "format", required = false) String formatParam,
            @RequestParam(value = "templateId", required = false) String templateId,
            @RequestHeader(value = "Prefer", required = false) String prefer) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        String cleanIfMatch = ifMatch.replace("\"", "");
        ObjectVersionId targetObjId = new ObjectVersionId(cleanIfMatch);
        UUID compositionId = extractVersionedObjectUid(cleanIfMatch);
        requestContext.setCompositionId(compositionId);

        FormatNegotiator.Format inputFormat = FormatNegotiator.resolveInput(contentType, formatParam);
        CompositionFormat sdkFormat = toSdkFormat(inputFormat);

        Composition composition = compositionService.buildComposition(body, sdkFormat, templateId);
        UUID resultId =
                compositionService.update(ehrId, targetObjId, composition).orElseThrow();

        int newVersion = extractVersion(cleanIfMatch) + 1;
        String etag = resultId + "::" + systemService.getSystemId() + "::" + newVersion;
        URI location = locationUri("api", "v1", "ehrs", ehrId.toString(), "compositions", etag);

        if (preferRepresentation(prefer)) {
            StructuredString serialized = compositionService.serialize(composition, sdkFormat);
            return ok(etag).location(location)
                    .contentType(inputFormat.mediaType())
                    .body(serialized.getValue());
        }
        return ResponseEntity.noContent()
                .eTag("\"" + etag + "\"")
                .location(location)
                .build();
    }

    @DeleteMapping("/{preceding_version_uid}")
    @Operation(summary = "Delete composition (soft delete)")
    public ResponseEntity<Void> deleteComposition(
            @PathVariable("ehr_id") String ehrIdStr,
            @PathVariable("preceding_version_uid") String precedingVersionUid) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        String clean = precedingVersionUid.replace("\"", "");
        ObjectVersionId targetObjId = new ObjectVersionId(clean);
        UUID compositionId = extractVersionedObjectUid(clean);
        requestContext.setCompositionId(compositionId);

        compositionService.delete(ehrId, targetObjId);

        int deletedVersion = extractVersion(clean) + 1;
        String etag = compositionId + "::" + systemService.getSystemId() + "::" + deletedVersion;
        URI location = locationUri("api", "v1", "ehrs", ehrId.toString(), "compositions", etag);

        return ResponseEntity.noContent()
                .eTag("\"" + etag + "\"")
                .location(location)
                .build();
    }

    private static CompositionFormat toSdkFormat(FormatNegotiator.Format format) {
        return switch (format) {
            case CANONICAL_JSON -> CompositionFormat.JSON;
            case CANONICAL_XML -> CompositionFormat.XML;
            case FLAT -> CompositionFormat.FLAT;
            case STRUCTURED -> CompositionFormat.STRUCTURED;
        };
    }
}
