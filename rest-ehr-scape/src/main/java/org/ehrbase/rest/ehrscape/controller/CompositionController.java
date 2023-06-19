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

import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.rest.ehrscape.responsedata.Action;
import org.ehrbase.rest.ehrscape.responsedata.ActionRestResponseData;
import org.ehrbase.rest.ehrscape.responsedata.CompositionResponseData;
import org.ehrbase.rest.ehrscape.responsedata.CompositionWriteRestResponseData;
import org.ehrbase.rest.ehrscape.responsedata.Meta;
import org.ehrbase.rest.ehrscape.responsedata.RestHref;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@TenantAware
@RestController
@RequestMapping(
        path = API_ECIS_CONTEXT_PATH_WITH_VERSION + "/" + BaseController.COMPOSITION,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class CompositionController extends BaseController {

    private final CompositionService compositionService;

    public CompositionController(CompositionService compositionService) {
        this.compositionService = Objects.requireNonNull(compositionService);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_COMPOSITION_CREATE)
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
        var compositionUuid = compositionService
                .create(ehrId, composition)
                .orElseThrow(() -> new InternalServerException("Failed to create composition"));

        var responseData = new CompositionWriteRestResponseData();
        responseData.setAction(Action.CREATE);
        responseData.setCompositionUid(
                compositionUuid + "::" + compositionService.getServerConfig().getNodename() + "::" + 1);
        responseData.setMeta(buildMeta(responseData.getCompositionUid()));

        return ResponseEntity.created(
                        URI.create(responseData.getMeta().getHref().getUrl()))
                .body(responseData);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_COMPOSITION_READ)
    @GetMapping(path = "/{uid}")
    public ResponseEntity<CompositionResponseData> getComposition(
            @PathVariable("uid") String compositionUid,
            @RequestParam(value = "format", defaultValue = "XML") CompositionFormat format) {
        UUID identifier = getCompositionIdentifier(compositionUid);
        Integer version = null;

        if (isFullCompositionUid(compositionUid)) {
            version = getCompositionVersion(compositionUid); // version number is inorder: 1, 2, 3 etc.
        }

        UUID ehrId = compositionService.getEhrId(identifier);
        Optional<CompositionDto> compositionDto =
                compositionService.retrieve(ehrId, identifier, version).map(c -> CompositionService.from(ehrId, c));
        if (compositionDto.isPresent()) {

            // Serialize onto target format
            StructuredString serialize = compositionService.serialize(compositionDto.get(), format);

            CompositionResponseData responseDto = new CompositionResponseData();
            responseDto.setComposition(serialize);
            responseDto.setAction(Action.RETRIEVE);
            responseDto.setFormat(format);
            responseDto.setTemplateId(compositionDto.get().getTemplateId());
            String fullUid = compositionDto.get().getUuid() + "::"
                    + compositionService.getServerConfig().getNodename() + "::"
                    + compositionService.getLastVersionNumber(
                            compositionDto.get().getUuid());
            responseDto.setCompositionUid(fullUid);
            responseDto.setEhrId(compositionDto.get().getEhrId());
            Meta meta = buildMeta(responseDto.getCompositionUid());
            responseDto.setMeta(meta);
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_COMPOSITION_UPDATE)
    @PutMapping(path = "/{uid}")
    public ResponseEntity<ActionRestResponseData> update(
            @PathVariable("uid") String compositionUid,
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
        UUID ehrId = compositionService.getEhrId(compositionIdentifier);

        var compoObj = compositionService.buildComposition(content, format, templateId);

        // Actual update
        Optional<UUID> dtoOptional = compositionService.update(ehrId, objectVersionId, compoObj);

        var compositionVersionUid = dtoOptional
                .orElseThrow(() -> new InternalServerException("Failed to create composition"))
                .toString();
        ActionRestResponseData responseData = new ActionRestResponseData();
        responseData.setAction(Action.UPDATE);
        responseData.setMeta(buildMeta(compositionVersionUid));
        return ResponseEntity.ok(responseData);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_COMPOSITION_DELETE)
    @DeleteMapping(path = "/{uid}")
    public ResponseEntity<ActionRestResponseData> delete(@PathVariable("uid") String compositionUid) {

        ObjectVersionId objectVersionId = getObjectVersionId(compositionUid);
        UUID compositionIdentifier = getCompositionIdentifier(compositionUid);
        UUID ehrId = compositionService.getEhrId(compositionIdentifier);

        compositionService.delete(ehrId, objectVersionId);
        ActionRestResponseData responseData = new ActionRestResponseData();
        responseData.setAction(Action.DELETE);
        responseData.setMeta(buildMeta(""));
        return ResponseEntity.ok(responseData);
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
        url.setUrl(createLocationUri(COMPOSITION, compositionUid));
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
            throw new IllegalArgumentException("UID of the composition does not contain domain and version parts");
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
