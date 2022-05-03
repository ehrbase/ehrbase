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

import com.nedap.archie.rm.support.identification.ObjectVersionId;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.response.ehrscape.CompositionDto;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.rest.ehrscape.responsedata.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(
        path = "/rest/ecis/v1/composition",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
)
public class CompositionController extends BaseController {

    private final CompositionService compositionService;

    public CompositionController(CompositionService compositionService) {
        this.compositionService = Objects.requireNonNull(compositionService);
    }

    @PostMapping
    public ResponseEntity<CompositionWriteRestResponseData> createComposition(
            @RequestParam(value = "format", defaultValue = "XML") CompositionFormat format,
            @RequestParam(value = "templateId", required = false) String templateId,
            @RequestParam(value = "ehrId") UUID ehrId,
            @RequestBody String content) {

        if ((format == CompositionFormat.FLAT
                || format == CompositionFormat.STRUCTURED
                || format == CompositionFormat.ECISFLAT)
                && StringUtils.isEmpty(templateId)) {
            throw new InvalidApiParameterException(
                    String.format("Template Id needs to specified for format %s", format));
        }

        var composition = compositionService.buildComposition(content, format, templateId);
        var compositionUuid = compositionService.create(ehrId, composition)
                .orElseThrow(() -> new InternalServerException("Failed to create composition"));

        var responseData = new CompositionWriteRestResponseData();
        responseData.setAction(Action.CREATE);
        responseData.setCompositionUid(
                compositionUuid + "::" + compositionService.getServerConfig().getNodename() + "::" + 1);
        responseData.setMeta(buildMeta(responseData.getCompositionUid()));

        return ResponseEntity.created(URI.create(responseData.getMeta().getHref().getUrl()))
                .body(responseData);
    }

    @GetMapping(path = "/{uid}")
    public ResponseEntity<CompositionResponseData> getComposition(
            @PathVariable("uid") String compositionUid,
            @RequestParam(value = "format", defaultValue = "XML") CompositionFormat format) {
        UUID identifier = getCompositionIdentifier(compositionUid);
        Integer version = null;

        if (isFullCompositionUid(compositionUid)) {
            version = getCompositionVersion(compositionUid); // version number is inorder: 1, 2, 3 etc.
        }

        Optional<CompositionDto> compositionDto = compositionService.retrieve(identifier, version);
        if (compositionDto.isPresent()) {

            // Serialize onto target format
            StructuredString serialize = compositionService.serialize(compositionDto.get(), format);

            CompositionResponseData responseDto = new CompositionResponseData();
            responseDto.setComposition(serialize);
            responseDto.setAction(Action.RETRIEVE);
            responseDto.setFormat(format);
            responseDto.setTemplateId(compositionDto.get().getTemplateId());
            String fullUid = compositionDto.get().getUuid() + "::" + compositionService.getServerConfig().getNodename() + "::"
                    + compositionService.getLastVersionNumber(compositionDto.get().getUuid());
            responseDto.setCompositionUid(fullUid);
            responseDto.setEhrId(compositionDto.get().getEhrId());
            Meta meta = buildMeta(responseDto.getCompositionUid());
            responseDto.setMeta(meta);
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(path = "/{uid}")
    public ResponseEntity<ActionRestResponseData> update(@PathVariable("uid") String compositionUid,
                                                         @RequestParam(value = "format", defaultValue = "XML") CompositionFormat format,
                                                         @RequestParam(value = "templateId", required = false) String templateId,
                                                         @RequestBody String content) {

        if ((format == CompositionFormat.FLAT
                || format == CompositionFormat.STRUCTURED
                || format == CompositionFormat.ECISFLAT)
                && StringUtils.isEmpty(templateId)) {
            throw new InvalidApiParameterException(
                    String.format("Template Id needs to specified for format %s", format));
        }

        ObjectVersionId objectVersionId = getObjectVersionId(compositionUid);
        UUID compositionIdentifier = getCompositionIdentifier(compositionUid);
        UUID ehrId = getEhrId(compositionIdentifier);

        var compoObj = compositionService.buildComposition(content, format, templateId);

        // Actual update
        Optional<UUID> dtoOptional = compositionService.update(ehrId, objectVersionId, compoObj);

        var compositionVersionUid =
                dtoOptional
                        .orElseThrow(() -> new InternalServerException("Failed to create composition"))
                        .toString();
        ActionRestResponseData responseData = new ActionRestResponseData();
        responseData.setAction(Action.UPDATE);
        responseData.setMeta(buildMeta(compositionVersionUid));
        return ResponseEntity.ok(responseData);
    }

    @DeleteMapping(path = "/{uid}")
    public ResponseEntity<ActionRestResponseData> delete(@PathVariable("uid") String compositionUid) {

        ObjectVersionId objectVersionId = getObjectVersionId(compositionUid);
        UUID compositionIdentifier = getCompositionIdentifier(compositionUid);
        UUID ehrId = getEhrId(compositionIdentifier);

        compositionService.delete(ehrId, objectVersionId);
        ActionRestResponseData responseData = new ActionRestResponseData();
        responseData.setAction(Action.DELETE);
        responseData.setMeta(buildMeta(""));
        return ResponseEntity.ok(responseData);
    }

    private UUID getEhrId(UUID compositionId) {
        // EhrScape API doesn't have access to the EHR ID here, so it needs to be retrieved.
        // Version 1 is enough because EHR never changes & it is always available.
        Optional<CompositionDto> dtoOptionalForEhr = compositionService.retrieve(compositionId, 1);
        return dtoOptionalForEhr
                .orElseThrow(() -> new InvalidApiParameterException("Invalid composition ID."))
                .getEhrId();
    }

    private ObjectVersionId getLatestVersionId(UUID compositionId) {
        // EhrScape API doesn't have access to the "If-Match" header or previous version, so it needs to
        // be retrieved.
        return new ObjectVersionId(
                compositionId.toString(),
                compositionService.getServerConfig().getNodename(),
                compositionService.getLastVersionNumber(compositionId).toString());
    }

    private Meta buildMeta(String compositionUid) {
        RestHref url = new RestHref();
        url.setUrl(getBaseEnvLinkURL() + "/rest/ecis/v1/composition/" + compositionUid);
        Meta meta = new Meta();
        meta.setHref(url);
        return meta;
    }

    private boolean isFullCompositionUid(String compositionUid) {
        return StringUtils.contains(compositionUid, "::");
    }

    private UUID getCompositionIdentifier(String compositionUid) {
        if (isFullCompositionUid(compositionUid)) {
            return UUID.fromString(compositionUid.substring(0, compositionUid.indexOf("::")));
        } else {
            return UUID.fromString(compositionUid);
        }
    }

    private int getCompositionVersion(String compositionUid) {
        if (!compositionUid.contains("::")) {
            throw new IllegalArgumentException(
                    "UID of the composition does not contain domain and version parts");
        }
        return Integer.parseInt(compositionUid.substring(compositionUid.lastIndexOf("::") + 2));
    }

    private ObjectVersionId getObjectVersionId(String compositionUid) {
        if (isFullCompositionUid(compositionUid)) {
            return new ObjectVersionId(compositionUid);
        } else {
            return getLatestVersionId(UUID.fromString(compositionUid));
        }
    }
}
