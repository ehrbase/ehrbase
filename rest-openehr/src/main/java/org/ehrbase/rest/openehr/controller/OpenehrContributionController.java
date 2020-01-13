/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.rest.openehr.controller;

import org.ehrbase.api.definitions.CompositionFormat;
import org.ehrbase.api.dto.ContributionDto;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.rest.openehr.response.CompositionResponseData;
import org.ehrbase.rest.openehr.response.ContributionResponseData;
import org.ehrbase.rest.openehr.response.ErrorResponseData;
import org.ehrbase.rest.openehr.response.InternalResponse;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

@Api(tags = "Contribution")
@RestController
@RequestMapping(path = "/rest/openehr/v1/ehr", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrContributionController extends BaseController {

    private final ContributionService contributionService;

    @Autowired
    public OpenehrContributionController(ContributionService contributionService) {
        this.contributionService = Objects.requireNonNull(contributionService);
    }

    @PostMapping(value = "/{ehr_id}/contribution", consumes = {"application/xml", "application/json"})
    @OperationNotesResourcesReaderOpenehr.ApiNotes("contributionPost.md")     // this utilizes a workaround, see source class for info
    @ApiOperation(value = "Create a new composition.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, response = CompositionResponseData.class, message = "New Contribution was created. Content body is only returned when Prefer header has return=representation, otherwise only headers are returned.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 204, message = "No Content - New Contribution was created but not full representation requested. Details in response headers.",
                    responseHeaders = {
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 400, response = ErrorResponseData.class, message = "Bad request: validation errors in one of the attached locatables, modification type doesn’t match the operation - i.e. first version of a composition with MODIFICATION."),
            @ApiResponse(code = 404, response = ErrorResponseData.class, message = "Not Found - The EHR with the supplied ehr_id did not exist.")})
    @ResponseStatus(value = HttpStatus.CREATED)    // overwrites default 200, fixes the wrong listing of 200 in swagger-ui (EHR-56)
    public ResponseEntity createContribution(@ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
                                            @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
                                            @ApiParam(value = REQ_CONTENT_TYPE_BODY, required = true) @RequestHeader(value = CONTENT_TYPE) String contentType,
                                            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                            @ApiParam(value = REQ_PREFER) @RequestHeader(value = PREFER, required = false) String prefer,
                                            @ApiParam(value = "EHR identifier taken from EHR.ehr_id.value", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
                                            @ApiParam(value = "The contribution to create", required = true) @RequestBody String contribution) {
        UUID ehrId = getEhrUuid(ehrIdString);

        UUID contributionId = contributionService.commitContribution(ehrId, contribution, extractCompositionFormat(contentType));

        URI uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId.toString() + "/contribution/" + contributionId.toString()));

        List<String> headerList = Arrays.asList(LOCATION, ETAG);   // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled separately

        Optional<InternalResponse<ContributionResponseData>> respData;   // variable to overload with more specific object if requested

        if (Optional.ofNullable(prefer).map(i -> i.equals(RETURN_REPRESENTATION)).orElse(false)) {      // null safe way to test prefer header
            respData = buildContributionResponseData(contributionId, ehrId, accept, uri, headerList, () -> new ContributionResponseData(null, null, null));
        } else {    // "minimal" is default fallback
            respData = buildContributionResponseData(contributionId, ehrId, accept, uri, headerList, () -> null);
        }

        // returns 201 with body + headers, 204 only with headers or 500 error depending on what processing above yields
        return respData.map(i -> Optional.ofNullable(i.getResponseData()).map(j -> ResponseEntity.created(uri).headers(i.getHeaders()).body(j))
                // when the body is empty
                .orElse(ResponseEntity.noContent().headers(i.getHeaders()).build()))
                // when no response could be created at all
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping(value = "/{ehr_id}/contribution/{contribution_uid}")
    @ApiOperation(value = "Get contribution by id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, response = CompositionResponseData.class, message = "Contribution found and returned.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 404, response = ErrorResponseData.class, message = "Not Found - No EHR with the supplied ehr_id or no Contribution with the supplied contribution_uid was found.")})
    public ResponseEntity getContribution(@ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
                                             @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
                                             @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                             @ApiParam(value = "EHR identifier taken from EHR.ehr_id.value", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
                                             @ApiParam(value = "", required = true) @PathVariable(value = "contribution_uid") String contributionUidString) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID contributionUid = getContributionVersionedObjectUidString(contributionUidString);

        URI uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId.toString() + "/contribution/" + contributionUid.toString()));

        List<String> headerList = Arrays.asList(LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec - CONTENT_TYPE handled separately

        Optional<InternalResponse<ContributionResponseData>> respData;   // variable to overload with more specific object if requested

        // building full / representation response
        respData = buildContributionResponseData(contributionUid, ehrId, accept, uri, headerList, () -> new ContributionResponseData(null, null, null));

        // returns 200 with body + headers or 500 in case of unexpected error
        return respData.map(i -> Optional.ofNullable(i.getResponseData()).map(j -> ResponseEntity.ok().headers(i.getHeaders()).body(j))
                // when response is empty, throw error
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()))
                // when no response could be created at all, throw error, too
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    private <T extends ContributionResponseData> Optional<InternalResponse<T>> buildContributionResponseData(UUID contributionId, UUID ehrId, String accept, URI uri, List<String> headerList, Supplier<T> factory) {
        // create either CompositionResponseData or null (means no body, only headers incl. link to resource), via lambda request
        T minimalOrRepresentation = factory.get();

        // do minimal scope steps
        // create and supplement headers with data depending on which headers are requested
        HttpHeaders respHeaders = new HttpHeaders();
        for (String header : headerList) {
            switch (header) {   // no default because everything else can be ignored
                case LOCATION:
                    respHeaders.setLocation(uri);
                    break;
                case ETAG:
                    respHeaders.setETag("\"" + contributionId + "\"");
                    break;
                case LAST_MODIFIED:
                    // TODO should be VERSION.commit_audit.time_committed.value which is not implemented yet - mock for now
                    respHeaders.setLastModified(123124442);
                    break;
            }
        }

        // if response data objects was created as "representation" do all task from wider scope, too
        if (minimalOrRepresentation != null) {
            // when this "if" is true the following casting can be executed and data manipulated by reference (handled by temporary variable)
            ContributionResponseData objByReference = (ContributionResponseData) minimalOrRepresentation;

            // retrieve contribution
            Optional<ContributionDto> contribution = contributionService.getContribution(ehrId, contributionId);

            // set all response field according to retrieved contribution
            objByReference.setUid(new HierObjectId(contributionId.toString()));
            List<ObjectRef<HierObjectId>> refs = new LinkedList<>();
            contribution.get().getObjectReferences().forEach((id, type) ->
                    refs.add(
                            new ObjectRef<HierObjectId>(new HierObjectId(id), "local", type)
                    ));
            objByReference.setVersions(refs);
            objByReference.setAudit(contribution.get().getAuditDetails());

            // if accept is empty fall back to XML
            if (accept.equals("*/*") || accept.isEmpty())
                accept = MediaType.APPLICATION_XML.toString();

            CompositionFormat format = extractCompositionFormat(accept);

            // finally set last header
            if (format.equals(CompositionFormat.XML)) {
                respHeaders.setContentType(MediaType.APPLICATION_XML);
            } else if (format.equals(CompositionFormat.JSON) || format.equals(CompositionFormat.FLAT) || format.equals(CompositionFormat.ECISFLAT) || format.equals(CompositionFormat.RAW)) {
                respHeaders.setContentType(MediaType.APPLICATION_JSON);
            } else {
                throw new NotAcceptableException("Wrong Accept header in request");
            }
        } // else continue with returning but without additional data from above, e.g. body

        return Optional.of(new InternalResponse<>(minimalOrRepresentation, respHeaders));
    }

}
