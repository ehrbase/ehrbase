/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.GeneralRequestProcessingException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.EhrStatusDto;
import org.ehrbase.rest.ehrscape.responsedata.Action;
import org.ehrbase.rest.ehrscape.responsedata.EhrResponseData;
import org.ehrbase.rest.ehrscape.responsedata.Meta;
import org.ehrbase.rest.ehrscape.responsedata.RestHref;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for /ehr resource of EhrScape REST API
 *
 * @author Stefan Spiska
 * @author Jake Smolka
 */
@TenantAware
@RestController
@RequestMapping(path = API_ECIS_CONTEXT_PATH_WITH_VERSION + "/ehr")
public class EhrController extends BaseController {

    private static final String MODIFIABLE = "modifiable";

    private static final String QUERYABLE = "queryable";

    private final EhrService ehrService;

    public EhrController(EhrService ehrService) {
        this.ehrService = Objects.requireNonNull(ehrService);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_CREATE)
    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    // overwrites default 200, fixes the wrong listing of 200 in swagger-ui (EHR-56)
    public ResponseEntity<EhrResponseData> createEhr(
            @RequestParam(value = "subjectId", required = false) String subjectId,
            @RequestParam(value = "subjectNamespace", required = false) String subjectNamespace,
            @RequestParam(value = "committerId", required = false) String committerId,
            @RequestParam(value = "committerName", required = false) String committerName,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {

        // subjectId and subjectNamespace are not required by EhrScape spec but without those parameters a 400 error
        // shall be
        // returned
        if ((subjectId == null) || (subjectNamespace == null)) {
            throw new InvalidApiParameterException("subjectId or subjectNamespace missing");
        } else if ((subjectId.isEmpty()) || (subjectNamespace.isEmpty())) {
            throw new InvalidApiParameterException("subjectId or subjectNamespace emtpy");
        }

        var subject = new PartySelf(new PartyRef(new HierObjectId(subjectId), subjectNamespace, "PERSON"));
        var status = new EhrStatus();
        status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        status.setName(new DvText("EHR Status"));
        status.setSubject(subject);

        UUID ehrId = ehrService.create(null, status);

        // TODO: use config file or alike to set the basic api path
        URI url = URI.create(createLocationUri(EHR, ehrId.toString()));
        return Optional.ofNullable(ehrId)
                .flatMap(i -> buildEhrResponseData(i, Action.CREATE, contentType))
                .map(ResponseEntity.created(url)::body)
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_READ)
    @GetMapping
    public ResponseEntity<EhrResponseData> getEhr(
            @RequestParam(value = "subjectId") String subjectId,
            @RequestParam(value = "subjectNamespace") String subjectNamespace,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {

        Optional<UUID> ehrId = ehrService.findBySubject(subjectId, subjectNamespace);
        return ehrId.flatMap(i -> buildEhrResponseData(i, Action.RETRIEVE, contentType))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_READ)
    @GetMapping(path = "/{uuid}")
    public ResponseEntity<EhrResponseData> getEhr(
            @PathVariable("uuid") UUID ehrId,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {

        return Optional.ofNullable(ehrId)
                .flatMap(i -> buildEhrResponseData(i, Action.RETRIEVE, contentType))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_EHR_UPDATE_STATUS)
    @PutMapping(path = "/{uuid}/status")
    public ResponseEntity<EhrResponseData> updateStatus(
            @PathVariable("uuid") UUID ehrId,
            @RequestBody String ehrStatus,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {

        ehrService.updateStatus(ehrId, extractEhrStatus(ehrStatus), null, null);
        return Optional.ofNullable(ehrId)
                .flatMap(i -> buildEhrResponseData(i, Action.UPDATE, contentType))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private EhrStatus extractEhrStatus(@RequestBody String content) {
        EhrStatus ehrStatus = new EhrStatus();
        ehrStatus.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        ehrStatus.setName(new DvText("EHR Status"));

        if (StringUtils.isBlank(content)) {
            return ehrStatus;
        }

        Map<String, Object> attributes;
        try {
            attributes = new ObjectMapper().readerForMapOf(Object.class).readValue(content);
        } catch (JsonProcessingException e) {
            throw new GeneralRequestProcessingException("Invalid content format", e);
        }

        String subjectId = getAttribute(attributes, "subjectId", String.class)
                .orElseThrow(() -> new InvalidApiParameterException("subjectId missing"));

        String subjectNamespace = getAttribute(attributes, "subjectNamespace", String.class)
                .orElseThrow(() -> new InvalidApiParameterException("subjectNamespace missing"));

        PartySelf subject = new PartySelf(new PartyRef(new HierObjectId(subjectId), subjectNamespace, "PERSON"));
        ehrStatus.setSubject(subject);

        getAttribute(attributes, MODIFIABLE, Boolean.class).ifPresent(ehrStatus::setModifiable);
        getAttribute(attributes, QUERYABLE, Boolean.class).ifPresent(ehrStatus::setQueryable);

        return ehrStatus;
    }

    private static <T> Optional<T> getAttribute(Map<String, Object> attributes, String key, Class<T> valueType) {
        return Optional.of(key)
                .map(attributes::get)
                .map(valueType::cast)
                .filter(v -> valueType != String.class || StringUtils.isNotBlank((String) v));
    }

    private Optional<EhrResponseData> buildEhrResponseData(UUID ehrId, Action create, String contentType) {
        // check for valid format header to produce content accordingly
        CompositionFormat format;
        if (contentType == null) {
            // assign default if no header was set
            format = CompositionFormat.RAW;
        } else {
            // if header was set process it
            MediaType mediaType = MediaType.parseMediaType(contentType);

            if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                format = CompositionFormat.RAW;
            } else if (mediaType.isCompatibleWith(MediaType.APPLICATION_XML)) {
                format = CompositionFormat.XML;
            } else {
                throw new InvalidApiParameterException("Wrong Content-Type header in request");
            }
        }

        // Optional<EhrStatusDto> ehrStatus = ehrService.getEhrStatus(ehrId, CompositionFormat.FLAT);
        // // older, keep until rework of formatting
        Optional<EhrStatusDto> ehrStatus = ehrService.getEhrStatusEhrScape(ehrId, format);
        if (!ehrStatus.isPresent()) {
            return Optional.empty();
        }
        EhrResponseData responseData = new EhrResponseData();
        responseData.setAction(create);
        responseData.setEhrId(ehrId);
        responseData.setEhrStatus(ehrStatus.get());
        RestHref url = new RestHref();
        // TODO: use config file or alike to set the basic api path
        url.setUrl(createLocationUri(EHR, responseData.getEhrId().toString()));
        Meta meta = new Meta();
        meta.setHref(url);
        responseData.setMeta(meta);
        return Optional.of(responseData);
    }
}
