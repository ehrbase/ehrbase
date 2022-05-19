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
package org.ehrbase.rest.openehr;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.response.ehrscape.CompositionDto;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.response.openehr.CompositionResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.audit.CompositionAuditInterceptor;
import org.ehrbase.rest.openehr.audit.OpenEhrAuditInterceptor;
import org.ehrbase.rest.openehr.specification.CompositionApiSpecification;
import org.ehrbase.rest.util.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
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
 *
 * @author Stefan Spiska
 * @author Jake Smolka
 * @since 1.0.0
 */
@RestController
@RequestMapping(
        path = "${openehr-api.context-path:/rest/openehr}/v1/ehr",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrCompositionController extends BaseController implements CompositionApiSpecification {

    final CompositionService compositionService;

    @Autowired
    public OpenehrCompositionController(CompositionService compositionService) {
        this.compositionService = Objects.requireNonNull(compositionService);
    }

    @PostMapping(
            value = "/{ehr_id}/composition",
            consumes = {"application/xml", "application/json"})
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PreAuthorize("checkAbacPre(@openehrCompositionController.COMPOSITION, "
            + "@ehrService.getSubjectExtRef(#ehrIdString), #composition, #contentType)")
    @ResponseStatus(value = HttpStatus.CREATED)
    @Override
    public ResponseEntity createComposition(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @RequestHeader(value = CONTENT_TYPE) String contentType,
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestBody String composition,
            HttpServletRequest request) {

        var ehrId = getEhrUuid(ehrIdString);

        var compositionFormat = extractCompositionFormat(contentType);

        var compoObj = compositionService.buildComposition(composition, compositionFormat, null);

        var compositionUuid = compositionService
                .create(ehrId, compoObj)
                .orElseThrow(() -> new InternalServerException("Failed to create composition"));

        var uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId.toString()
                + "/composition/" + compositionUuid.toString()));

        List<String> headerList = Arrays.asList(
                LOCATION,
                ETAG,
                LAST_MODIFIED); // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled
        // separately

        Optional<InternalResponse<CompositionResponseData>>
                respData; // variable to overload with more specific object if requested

        if (Optional.ofNullable(prefer)
                .map(i -> i.equals(RETURN_REPRESENTATION))
                .orElse(false)) { // null safe way to test prefer header
            respData = buildCompositionResponseData(
                    ehrId, compositionUuid, 0, accept, uri, headerList, () -> new CompositionResponseData(null, null));
        } else { // "minimal" is default fallback
            respData = buildCompositionResponseData(ehrId, compositionUuid, 0, accept, uri, headerList, () -> null);
        }

        // Enriches request attributes with current compositionId for later audit processing
        request.setAttribute(OpenEhrAuditInterceptor.EHR_ID_ATTRIBUTE, Collections.singleton(ehrId));
        request.setAttribute(CompositionAuditInterceptor.COMPOSITION_ID_ATTRIBUTE, compositionUuid);

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

    @PutMapping("/{ehr_id}/composition/{versioned_object_uid}")
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PreAuthorize("checkAbacPre(@openehrCompositionController.COMPOSITION, "
            + "@ehrService.getSubjectExtRef(#ehrIdString), #composition, #contentType)")
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
            @RequestBody String composition,
            HttpServletRequest request) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedObjectUid = getCompositionVersionedObjectUidString(versionedObjectUidString);

        CompositionFormat compositionFormat = extractCompositionFormat(contentType);

        // check if composition ID path variable is valid
        compositionService.exists(versionedObjectUid);

        // If the If-Match is not the latest latest existing version, throw error
        if (!((versionedObjectUid + "::" + compositionService.getServerConfig().getNodename() + "::"
                        + compositionService.getLastVersionNumber(
                                extractVersionedObjectUidFromVersionUid(versionedObjectUid.toString())))
                .equals(ifMatch))) {
            throw new PreconditionFailedException("If-Match header does not match latest existing version");
        }

        Composition compoObj = compositionService.buildComposition(composition, compositionFormat, null);
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

        Optional<InternalResponse<CompositionResponseData>> respData =
                Optional.empty(); // variable to overload with more specific object if requested
        try {
            // ifMatch header has to be tested for correctness already above
            var compositionVersionUid = compositionService
                    .update(ehrId, new ObjectVersionId(ifMatch), compoObj)
                    .orElseThrow(() -> new InternalServerException("Failed to create composition"))
                    .toString();

            var uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId.toString()
                    + "/composition/" + compositionVersionUid));

            // whatever is required by REST spec - CONTENT_TYPE only needed for 200, so handled separately
            List<String> headerList = Arrays.asList(LOCATION, ETAG, LAST_MODIFIED);

            UUID compositionId = extractVersionedObjectUidFromVersionUid(compositionVersionUid);

            if (RETURN_REPRESENTATION.equals(prefer)) {
                // both options extract needed info from versionUid
                respData = buildCompositionResponseData(
                        ehrId,
                        compositionId,
                        extractVersionFromVersionUid(compositionVersionUid),
                        accept,
                        uri,
                        headerList,
                        () -> new CompositionResponseData(null, null));
            } else {
                // "minimal" is default fallback
                respData = buildCompositionResponseData(
                        ehrId,
                        compositionId,
                        extractVersionFromVersionUid(compositionVersionUid),
                        accept,
                        uri,
                        headerList,
                        () -> null);
            }

            // Enriches request attributes with current compositionId for later audit processing
            request.setAttribute(OpenEhrAuditInterceptor.EHR_ID_ATTRIBUTE, Collections.singleton(ehrId));
            request.setAttribute(CompositionAuditInterceptor.COMPOSITION_ID_ATTRIBUTE, compositionId);

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
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PreAuthorize("checkAbacPre(@openehrCompositionController.COMPOSITION, "
            + "@ehrService.getSubjectExtRef(#ehrIdString), #precedingVersionUid, null)")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Override
    public ResponseEntity deleteComposition(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "preceding_version_uid") String precedingVersionUid,
            HttpServletRequest request) {
        UUID ehrId = getEhrUuid(ehrIdString);

        HttpHeaders headers = new HttpHeaders();

        // check if this composition in given preceding version is available
        compositionService
                .retrieve(ehrId, extractVersionedObjectUidFromVersionUid(precedingVersionUid), 1)
                .orElseThrow(
                        () -> new ObjectNotFoundException(
                                "composition",
                                "No EHR with the supplied ehr_id or no COMPOSITION with the supplied preceding_version_uid.")); // TODO check for ehr + composition match as well - wow to to that? should be part of deletion, according to openEHR platform spec --> postponed, see EHR-265

        // TODO check if already deleted - how is that saved / retrievable? --> postponed, see EHR-264
        /*if () {
            throw new GeneralRequestProcessingException("The composition with preceding_version_uid is already deleted.");  // exception is wired to 400 BAD_REQUEST
        }*/

        // prepare header data
        String latestVersionId = extractVersionedObjectUidFromVersionUid(precedingVersionUid) + "::"
                + compositionService.getServerConfig().getNodename() + "::"
                + compositionService.getLastVersionNumber(extractVersionedObjectUidFromVersionUid(precedingVersionUid));
        // TODO change to dynamic linking --> postponed, see EHR-230
        URI uri = URI.create(this.encodePath(
                getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId.toString() + "/composition/" + latestVersionId));

        // If precedingVersionUid parameter doesn't match latest version
        if (!compositionService
                .getLastVersionNumber(extractVersionedObjectUidFromVersionUid(precedingVersionUid))
                .equals(extractVersionFromVersionUid(precedingVersionUid))) {
            // 409 is returned when supplied preceding_version_uid doesn’t match the latest version. Returns latest
            // version in the Location and ETag headers.
            headers.setLocation(uri);
            headers.setETag("\"" + latestVersionId + "\"");

            return ResponseEntity.status(HttpStatus.CONFLICT).headers(headers).build();
        }

        try { // the actual deleting
            // precedingVersionUid needs to be checked already
            compositionService.delete(ehrId, new ObjectVersionId(precedingVersionUid));

            headers.setLocation(uri);
            headers.setETag("\"" + latestVersionId + "\"");
            headers.setLastModified(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli());

            // Enriches request attributes with current compositionId for later audit processing
            request.setAttribute(OpenEhrAuditInterceptor.EHR_ID_ATTRIBUTE, Collections.singleton(ehrId));
            request.setAttribute(
                    CompositionAuditInterceptor.COMPOSITION_ID_ATTRIBUTE,
                    extractVersionedObjectUidFromVersionUid(precedingVersionUid));

            return ResponseEntity.noContent().headers(headers).build();
        } catch (ObjectNotFoundException e) {
            // if composition not available at all --> 404
            throw new ObjectNotFoundException(
                    "composition",
                    "No EHR with the supplied ehr_id or no COMPOSITION with the supplied " + "preceding_version_uid.");
        } catch (StateConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException("Deleting of composition failed", e);
        }
    }

    /**
     * Acts as overloaded function and calls the overlapping and more specific method
     * getCompositionByTime. Catches both "/{ehr_id}/composition/{version_uid}" and
     * "/{ehr_id}/composition/{versioned_object_uid}" which works because their processing is the
     * same. "{?version_at_time}" is hidden in swagger-ui, it only is here to be piped through.
     */
    @GetMapping("/{ehr_id}/composition/{version_uid}")
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PostAuthorize("checkAbacPost(@openehrCompositionController.COMPOSITION, "
            + "@ehrService.getSubjectExtRef(#ehrIdString), returnObject, #accept)")
    @Override
    public ResponseEntity<CompositionResponseData> getCompositionByVersionId(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "version_uid") String versionUid,
            @RequestParam(value = "version_at_time", required = false) String versionAtTime,
            HttpServletRequest request) {
        return getCompositionByTime(accept, ehrIdString, versionUid, versionAtTime, request);
    }

    /**
     * This mapping combines both GETs "/{ehr_id}/composition/{version_uid}" (via overlapping path)
     * and "/{ehr_id}/composition/{versioned_object_uid}{?version_at_time}" (here). This is necessary
     * because of the overlapping paths. Both mappings are specified to behave almost the same, so
     * this solution works in this case.
     */
    @GetMapping("/{ehr_id}/composition/{versioned_object_uid}{?version_at_time}")
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PostAuthorize("checkAbacPost(@openehrCompositionController.COMPOSITION, "
            + "@ehrService.getSubjectExtRef(#ehrIdString), returnObject, #accept)")
    @Override
    public ResponseEntity getCompositionByTime(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @RequestParam(value = "version_at_time", required = false) String versionAtTime,
            HttpServletRequest request) {
        UUID ehrId = getEhrUuid(ehrIdString);

        // Note: Since this method can be called by another mapping as "almost overloaded" function some parameters
        // might be semantically named wrong in that case. E.g. versionedObjectUid can contain a versionUid.
        // Note: versionUid should be of format "uuid::domain::version", versionObjectUid of format "uuid"
        UUID compositionUid = extractVersionedObjectUidFromVersionUid(
                versionedObjectUid); // extracts UUID from long or short notation

        if (compositionService.isDeleted(compositionUid)) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        int version = 0; // fallback 0 means latest version
        if (extractVersionFromVersionUid(versionedObjectUid) != 0) {
            // the given ID contains a version, therefore this is case GET {version_uid}
            version = extractVersionFromVersionUid(versionedObjectUid);
        } else {
            // case GET {versioned_object_uid}{?version_at_time}
            Optional<OffsetDateTime> temporal = getVersionAtTimeParam();
            if (versionAtTime != null && temporal.isPresent()) {
                // when optional request parameter was provided, retrieve version according to given time

                Optional<Integer> versionFromTimestamp = Optional.ofNullable(compositionService.getVersionByTimestamp(
                        compositionUid, temporal.get().toLocalDateTime()));
                version = versionFromTimestamp.orElseThrow(() -> new ObjectNotFoundException(
                        "composition", "No composition version matching the timestamp condition"));
            } // else continue with fallback: latest version
        }

        URI uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId.toString()
                + "/composition/" + versionedObjectUid));

        List<String> headerList = Arrays.asList(
                LOCATION,
                ETAG,
                LAST_MODIFIED); // whatever is required by REST spec - CONTENT_TYPE only needed for 200, so handled
        // separately

        Optional<InternalResponse<CompositionResponseData>> respData = buildCompositionResponseData(
                ehrId, compositionUid, version, accept, uri, headerList, () -> new CompositionResponseData(null, null));

        // Enriches request attributes with ehrId, compositionId and version for later audit processing
        request.setAttribute(OpenEhrAuditInterceptor.EHR_ID_ATTRIBUTE, Collections.singleton(ehrId));
        request.setAttribute(CompositionAuditInterceptor.COMPOSITION_ID_ATTRIBUTE, compositionUid);
        request.setAttribute(CompositionAuditInterceptor.VERSION_ATTRIBUTE, version);

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
     * @param <T> Type of the response body
     * @param ehrId
     * @param compositionId ID of the composition
     * @param version 0 if latest, otherwise integer of specific version.
     * @param accept Format the response should be delivered in, as given by request
     * @param uri Location of resource
     * @param headerList List of headers to be set for response
     * @param factory Lambda function to constructor of desired object
     * @return
     */
    private <T extends CompositionResponseData> Optional<InternalResponse<T>> buildCompositionResponseData(
            UUID ehrId,
            UUID compositionId,
            Integer version,
            String accept,
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
                    respHeaders.setETag("\"" + compositionId + "::"
                            + compositionService.getServerConfig().getNodename() + "::"
                            + compositionService.getLastVersionNumber(compositionId) + "\"");
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

        // if response data objects was created as "representation" do all task from wider scope, too
        // if (minimalOrRepresentation.getClass().equals(CompositionResponseData.class)) {     // TODO make
        // Optional.ofNull....
        if (minimalOrRepresentation != null) {
            // when this "if" is true the following casting can be executed and data manipulated by reference (handled
            // by temporary variable)
            CompositionResponseData objByReference = (CompositionResponseData) minimalOrRepresentation;

            CompositionFormat format = extractCompositionFormat(accept);

            // version handling allows to request specific version
            Integer versionNumber = version;
            if (versionNumber == 0) {
                versionNumber = compositionService.getLastVersionNumber(compositionId);
            }

            Optional<CompositionDto> compositionDto = compositionService
                    .retrieve(ehrId, compositionId, versionNumber)
                    .map(c -> CompositionService.from(ehrId, c));
            // TODO how to handle error situation here only with Optional? is there a better way without java 9
            // Optional.ifPresentOrElse()?
            if (compositionDto.isPresent()) {
                StructuredString ss = compositionService.serialize(compositionDto.get(), format);
                objByReference.setValue(ss.getValue());
                objByReference.setFormat(ss.getFormat());
                // objByReference.setComposition(compositionService.serialize(compositionDto.get(), format));
            } else {
                // TODO undo creation of composition, if applicable
                throw new ObjectNotFoundException("composition", "Couldn't retrieve composition");
            }

            // finally set last header
            if (format.equals(CompositionFormat.XML)) {
                respHeaders.setContentType(MediaType.APPLICATION_XML);
            } else if (format.equals(CompositionFormat.FLAT)
                    || format.equals(CompositionFormat.ECISFLAT)
                    || format.equals(CompositionFormat.RAW)) {
                respHeaders.setContentType(MediaType.APPLICATION_JSON);
            }
        } // else continue with returning but without additional data from above, e.g. body

        return Optional.of(new InternalResponse<>(minimalOrRepresentation, respHeaders));
    }

    private Optional<String> getUidFrom(Composition composition) {
        return Optional.ofNullable(composition.getUid()).map(uid -> uid.toString());
    }
}
