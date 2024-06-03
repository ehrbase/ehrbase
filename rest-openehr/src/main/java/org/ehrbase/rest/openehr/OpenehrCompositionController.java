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
package org.ehrbase.rest.openehr;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.ehrbase.api.rest.HttpRestContext.EHR_ID;
import static org.ehrbase.api.rest.HttpRestContext.TEMPLATE_ID;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.rest.EHRbaseHeader;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.sdk.response.dto.CompositionResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.format.CompositionRepresentation;
import org.ehrbase.rest.openehr.format.OpenEHRMediaType;
import org.ehrbase.rest.openehr.specification.CompositionApiSpecification;
import org.ehrbase.rest.util.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for /composition resource as part of the EHR sub-API of the openEHR REST API
 */
@ConditionalOnMissingBean(name = "primaryopenehrcompositioncontroller")
@RestController
@RequestMapping(
        path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/ehr",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            OpenEHRMediaType.APPLICATION_WT_FLAT_SCHEMA_JSON_VALUE,
            OpenEHRMediaType.APPLICATION_WT_STRUCTURED_SCHEMA_JSON_VALUE
        })
public class OpenehrCompositionController extends BaseController implements CompositionApiSpecification {

    private final CompositionService compositionService;

    private final SystemService systemService;

    @Autowired
    public OpenehrCompositionController(CompositionService compositionService, SystemService systemService) {
        this.compositionService = Objects.requireNonNull(compositionService);
        this.systemService = systemService;
    }

    @PostMapping(
            value = "/{ehr_id}/composition",
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                OpenEHRMediaType.APPLICATION_WT_FLAT_SCHEMA_JSON_VALUE,
                OpenEHRMediaType.APPLICATION_WT_STRUCTURED_SCHEMA_JSON_VALUE
            })
    @ResponseStatus(value = HttpStatus.CREATED)
    @Override
    public ResponseEntity createComposition(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @RequestHeader(value = CONTENT_TYPE) String contentType,
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestParam(value = "templateId", required = false) String templateId,
            @RequestParam(value = "format", required = false) String format,
            @RequestBody String composition) {

        var ehrId = getEhrUuid(ehrIdString);

        var requestRepresentation = extractCompositionRepresentation(contentType, format);
        var responseRepresentation = extractCompositionRepresentation(accept, format);

        var compoObj = compositionService.buildComposition(composition, requestRepresentation.format, templateId);
        var compositionUuid = compositionService
                .create(ehrId, compoObj)
                .orElseThrow(() -> new InternalServerException("Failed to create composition"));
        URI uri = createLocationUri(EHR, ehrId.toString(), COMPOSITION, compositionUuid.toString());

        // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled separately
        List<String> headerList = Arrays.asList(LOCATION, ETAG, LAST_MODIFIED);

        Optional<InternalResponse<CompositionResponseData>> respData = buildCompositionResponseData(
                ehrId,
                compositionUuid,
                1,
                responseRepresentation,
                uri,
                headerList,
                RETURN_REPRESENTATION.equals(prefer) ? () -> new CompositionResponseData(null, null) : () -> null);

        // returns 201 with body + headers, 204 only with headers or 500 error depending on what processing above yields
        return respData.map(i -> Optional.ofNullable(i.getResponseData())
                        .map(StructuredString::getValue)
                        .map(j -> ResponseEntity.created(uri)
                                .headers(i.getHeaders())
                                .body(j))
                        // when the body is empty
                        .orElse(ResponseEntity.noContent()
                                .headers(i.getHeaders())
                                .build()))
                // when no response could be created at all
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PutMapping(
            value = "/{ehr_id}/composition/{versioned_object_uid}",
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                OpenEHRMediaType.APPLICATION_WT_FLAT_SCHEMA_JSON_VALUE,
                OpenEHRMediaType.APPLICATION_WT_STRUCTURED_SCHEMA_JSON_VALUE
            })
    @Override
    public ResponseEntity updateComposition(
            String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @RequestHeader(value = CONTENT_TYPE, required = false) String contentType,
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @RequestHeader(value = IF_MATCH) String ifMatch,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUidString,
            @RequestParam(value = "templateId", required = false) String templateId,
            @RequestParam(value = "format", required = false) String format,
            @RequestBody String composition) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedObjectUid = getCompositionVersionedObjectUidString(versionedObjectUidString);

        CompositionRepresentation requestRepresentation = extractCompositionRepresentation(contentType, format);
        CompositionRepresentation responseRepresentation = extractCompositionRepresentation(accept, format);

        Composition compoObj =
                compositionService.buildComposition(composition, requestRepresentation.format, templateId);

        // If body already contains a composition uid it must match the {versioned_object_uid} in request url
        Optional<String> inputUuid = getUidFrom(compoObj);
        inputUuid.ifPresent(id -> {
            // TODO currently the this part of the spec is implemented as "the request body's composition version_uid
            // must be compatible to the given versioned_object_uid"
            // TODO it is further unclear what exactly the REST spec's "match" means, see:
            // https://github.com/openEHR/specifications-ITS-REST/issues/83
            if (!versionedObjectUid.equals(extractVersionedObjectUidFromVersionUid(id)))
                throw new PreconditionFailedException(
                        "UUID from input must match given versioned_object_uid in request URL");
        });

        final Optional<InternalResponse<CompositionResponseData>> respData;
        try {
            ObjectVersionId ifMatchId = new ObjectVersionId(ifMatch);
            // ifMatch header has to be tested for correctness already above
            String compositionVersionUid = compositionService
                    .update(ehrId, ifMatchId, compoObj)
                    .orElseThrow(() -> new InternalServerException("Failed to create composition"))
                    .toString();

            URI uri = createLocationUri(EHR, ehrId.toString(), COMPOSITION, compositionVersionUid);

            // whatever is required by REST spec - CONTENT_TYPE only needed for 200, so handled separately
            List<String> headerList = Arrays.asList(LOCATION, ETAG, LAST_MODIFIED);

            UUID compositionId = extractVersionedObjectUidFromVersionUid(compositionVersionUid);
            int nextVersion = Integer.parseInt(ifMatchId.getVersionTreeId().getValue()) + 1;
            respData = buildCompositionResponseData(
                    ehrId,
                    compositionId,
                    nextVersion,
                    responseRepresentation,
                    uri,
                    headerList,
                    RETURN_REPRESENTATION.equals(prefer) ? () -> new CompositionResponseData(null, null) : () -> null);

        } catch (ObjectNotFoundException e) { // composition not found
            return ResponseEntity.notFound().build();
        } // composition input not parsable / buildable -> bad request handled by BaseController class

        // returns 200 with body + headers, 204 only with headers or 500 error depending on what processing above yields
        return respData.map(i -> Optional.ofNullable(i.getResponseData())
                        .map(StructuredString::getValue)
                        .map(j -> ResponseEntity.ok().headers(i.getHeaders()).body(j))
                        .orElse(ResponseEntity.noContent()
                                .headers(i.getHeaders())
                                .build()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @DeleteMapping("/{ehr_id}/composition/{preceding_version_uid}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Override
    public ResponseEntity deleteComposition(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "preceding_version_uid") String precedingVersionUid) {
        UUID ehrId = getEhrUuid(ehrIdString);

        HttpHeaders headers = new HttpHeaders();

        // prepare header data

        try { // the actual deleting
            // precedingVersionUid needs to be checked already
            ObjectVersionId targetObjId = new ObjectVersionId(precedingVersionUid);
            compositionService.delete(ehrId, targetObjId);

            // set next deleted version
            int nextVersion = Integer.parseInt(targetObjId.getVersionTreeId().getValue()) + 1;
            targetObjId.getVersionTreeId().setValue(String.valueOf(nextVersion));
            URI uri = createLocationUri(EHR, ehrId.toString(), COMPOSITION, targetObjId.getValue());

            headers.setLocation(uri);
            headers.setETag("\"" + targetObjId.getValue() + "\"");
            headers.setLastModified(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli());

            UUID compositionUid = UUID.fromString(targetObjId.getObjectId().getValue());
            Integer version = Integer.parseInt(targetObjId.getVersionTreeId().getValue());

            HttpRestContext.register(
                    EHR_ID,
                    ehrId,
                    HttpRestContext.LOCATION,
                    getLocationUrl(compositionUid, ehrId, version),
                    TEMPLATE_ID,
                    compositionService.retrieveTemplateId(compositionUid));

            return ResponseEntity.noContent().headers(headers).build();
        } catch (ObjectNotFoundException e) {
            // if composition not available at all --> 404
            throw new ObjectNotFoundException(
                    COMPOSITION,
                    "No EHR with the supplied ehr_id or no COMPOSITION with the supplied " + "preceding_version_uid.");
        }
    }

    private String getLocationUrl(UUID versionedObjectUid, UUID ehrId, int version) {
        if (version == 0) version = compositionService.getLastVersionNumber(versionedObjectUid);

        return fromPath("/{ehrSegment}/{ehrId}/{compositionSegment}/{compositionId}::{nodeName}::{version}")
                .build(EHR, ehrId.toString(), COMPOSITION, versionedObjectUid, systemService.getSystemId(), version)
                .toString();
    }

    /**
     * This mapping combines both GETs "/{ehr_id}/composition/{version_uid}" (via overlapping path)
     * and "/{ehr_id}/composition/{versioned_object_uid}{?version_at_time}" (here). This is necessary
     * because of the overlapping paths. Both mappings are specified to behave almost the same, so
     * this solution works in this case.
     */
    @GetMapping("/{ehr_id}/composition/{versioned_object_uid}")
    @Override
    public ResponseEntity getComposition(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "version_at_time", required = false) String versionAtTime) {

        UUID ehrId = getEhrUuid(ehrIdString);

        CompositionRepresentation responseRepresentation = extractCompositionRepresentation(accept, format);

        // Note: Since this method can be called by another mapping as "almost overloaded" function some parameters
        // might be semantically named wrong in that case. E.g. versionedObjectUid can contain a versionUid.
        // Note: versionUid should be of format "uuid::domain::version", versionObjectUid of format "uuid"
        UUID compositionUid = extractVersionedObjectUidFromVersionUid(
                versionedObjectUid); // extracts UUID from long or short notation

        int version = extractVersionFromVersionUid(versionedObjectUid)
                .or(() -> decodeVersionAtTime(versionAtTime)
                        .map(t -> Optional.ofNullable(compositionService.getVersionByTimestamp(compositionUid, t))
                                .orElseThrow(() -> new ObjectNotFoundException(
                                        COMPOSITION, "No composition version matching the timestamp condition"))))
                .orElseGet(() -> compositionService.getLastVersionNumber(compositionUid));

        if (compositionService.isDeleted(ehrId, compositionUid, version)) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        URI uri = createLocationUri(EHR, ehrId.toString(), COMPOSITION, versionedObjectUid);

        List<String> headerList = Arrays.asList(
                LOCATION,
                ETAG,
                LAST_MODIFIED); // whatever is required by REST spec - CONTENT_TYPE only needed for 200, so handled
        // separately

        Optional<InternalResponse<CompositionResponseData>> respData = buildCompositionResponseData(
                ehrId,
                compositionUid,
                version,
                responseRepresentation,
                uri,
                headerList,
                () -> new CompositionResponseData(null, null));

        // returns 200 with body + headers, 204 only with headers or 500 error depending on what processing above yields
        return respData.map(i -> Optional.ofNullable(i.getResponseData().getValue())
                        .map(j -> ResponseEntity.ok().headers(i.getHeaders()).body(j))
                        // when the body is empty
                        .orElse(ResponseEntity.noContent()
                                .headers(i.getHeaders())
                                .build()))
                // when no response could be created at all
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Builder method to prepare appropriate HTTP response. Flexible to either allow minimal or full
     * representation of resource.
     *
     * @param <T>                    Type of the response body
     * @param ehrId                  ID of the affected EHR
     * @param compositionId          ID of the composition
     * @param version                0 if latest, otherwise integer of specific version.
     * @param responseRepresentation Response Content-Type and {@link org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat}
     *                               the response should be delivered in, as given by request
     * @param uri                    Location of resource
     * @param headerList             List of headers to be set for response
     * @param factory                Lambda function to constructor of desired object
     * @return
     */
    private <T extends CompositionResponseData> Optional<InternalResponse<T>> buildCompositionResponseData(
            UUID ehrId,
            UUID compositionId,
            int version,
            CompositionRepresentation responseRepresentation,
            URI uri,
            List<String> headerList,
            Supplier<T> factory) {
        // create either CompositionResponseData or null (means no body, only headers incl. link to resource), via
        // lambda request
        T minimalOrRepresentation = factory.get();

        // do minimal scope steps
        // create and supplement headers with data depending on which headers are requested
        HttpHeaders respHeaders = new HttpHeaders();
        for (String header : headerList) {
            switch (header) {
                case LOCATION:
                    respHeaders.setLocation(uri);
                    break;
                case ETAG:
                    respHeaders.setETag(
                            "\"" + compositionId + "::" + systemService.getSystemId() + "::" + version + "\"");
                    break;
                case LAST_MODIFIED:
                    // TODO should be VERSION.commit_audit.time_committed.value which is not implemented yet - mock for
                    // now
                    respHeaders.setLastModified(123124442);
                    break;
                default:
                    // Ignore header
            }
        }

        String templateId = null;

        // if response data objects was created as "representation" do all task from wider scope, too
        // if (minimalOrRepresentation.getClass().equals(CompositionResponseData.class)) {     // TODO make
        // Optional.ofNull....
        if (minimalOrRepresentation != null) {
            // when this "if" is true the following casting can be executed and data manipulated by reference (handled
            // by temporary variable)
            CompositionResponseData objByReference = minimalOrRepresentation;

            CompositionDto compositionDto = compositionService
                    .retrieve(ehrId, compositionId, version)
                    .map(c -> CompositionService.from(ehrId, c))
                    .orElseThrow(() -> new ObjectNotFoundException(COMPOSITION, "Couldn't retrieve composition"));

            templateId = compositionDto.getTemplateId();

            StructuredString ss = compositionService.serialize(compositionDto, responseRepresentation.format);
            objByReference.setValue(ss.getValue());
            objByReference.setFormat(ss.getFormat());

            // finally set last header
            respHeaders.setContentType(responseRepresentation.mediaType);
        } // else continue with returning but without additional data from above, e.g. body

        if (isBlank(templateId)) {
            templateId = compositionService.retrieveTemplateId(compositionId);
        }

        respHeaders.addIfAbsent(EHRbaseHeader.TEMPLATE_ID, templateId);

        HttpRestContext.register(
                EHR_ID,
                ehrId,
                HttpRestContext.LOCATION,
                getLocationUrl(compositionId, ehrId, version),
                TEMPLATE_ID,
                templateId);

        return Optional.of(new InternalResponse<>(minimalOrRepresentation, respHeaders));
    }

    private Optional<String> getUidFrom(Composition composition) {
        return Optional.ofNullable(composition.getUid()).map(ObjectId::toString);
    }
}
