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
package org.ehrbase.rest.openehr;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.audit.msg.AuditMsgBuilder;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.openehr.sdk.response.dto.ContributionResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.ContributionApiSpecification;
import org.ehrbase.rest.util.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@TenantAware
@RestController
@RequestMapping(
        path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/ehr",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrContributionController extends BaseController implements ContributionApiSpecification {

    private final ContributionService contributionService;

    @Autowired
    public OpenehrContributionController(ContributionService contributionService) {
        this.contributionService = Objects.requireNonNull(contributionService);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_CONTRIBUTION_CREATE)
    @PostMapping(
            value = "/{ehr_id}/contribution",
            consumes = {"application/xml", "application/json"})
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PreAuthorize("checkAbacPre(@openehrContributionController.CONTRIBUTION, "
            + "@ehrService.getSubjectExtRef(#ehrIdString), #contribution, #contentType)")
    @ResponseStatus(
            value = HttpStatus.CREATED) // overwrites default 200, fixes the wrong listing of 200 in swagger-ui (EHR-56)
    @Override
    public ResponseEntity createContribution(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @RequestHeader(value = CONTENT_TYPE) String contentType,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestBody String contribution) {
        UUID ehrId = getEhrUuid(ehrIdString);

        UUID contributionId =
                contributionService.commitContribution(ehrId, contribution, extractCompositionFormat(contentType));

        URI uri = createLocationUri(EHR, ehrId.toString(), CONTRIBUTION, contributionId.toString());

        List<String> headerList = Arrays.asList(
                LOCATION,
                ETAG); // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled separately

        Optional<InternalResponse<ContributionResponseData>>
                respData; // variable to overload with more specific object if requested

        if (Optional.ofNullable(prefer)
                .map(i -> i.equals(RETURN_REPRESENTATION))
                .orElse(false)) { // null safe way to test prefer header
            respData = buildContributionResponseData(
                    contributionId,
                    ehrId,
                    accept,
                    uri,
                    headerList,
                    () -> new ContributionResponseData(null, null, null));
        } else { // "minimal" is default fallback
            respData = buildContributionResponseData(contributionId, ehrId, accept, uri, headerList, () -> null);
        }

        createAuditLogsMsgBuilder(ehrId, contributionId);

        // returns 201 with body + headers, 204 only with headers or 500 error depending on what processing above yields
        return respData.map(i -> Optional.ofNullable(i.getResponseData())
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

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_CONTRIBUTION_READ)
    @GetMapping(value = "/{ehr_id}/contribution/{contribution_uid}")
    @Override
    public ResponseEntity getContribution(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "contribution_uid") String contributionUidString) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID contributionUid = getContributionVersionedObjectUidString(contributionUidString);

        URI uri = createLocationUri(EHR, ehrId.toString(), CONTRIBUTION, contributionUid.toString());

        List<String> headerList = Arrays.asList(
                LOCATION, ETAG, LAST_MODIFIED); // whatever is required by REST spec - CONTENT_TYPE handled separately

        Optional<InternalResponse<ContributionResponseData>>
                respData; // variable to overload with more specific object if requested

        // building full / representation response
        respData = buildContributionResponseData(
                contributionUid, ehrId, accept, uri, headerList, () -> new ContributionResponseData(null, null, null));

        createAuditLogsMsgBuilder(ehrId, contributionUid);

        // returns 200 with body + headers or 500 in case of unexpected error
        return respData.map(i -> Optional.ofNullable(i.getResponseData())
                        .map(j -> ResponseEntity.ok().headers(i.getHeaders()).body(j))
                        // when response is empty, throw error
                        .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .build()))
                // when no response could be created at all, throw error, too
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    private <T extends ContributionResponseData> Optional<InternalResponse<T>> buildContributionResponseData(
            UUID contributionId, UUID ehrId, String accept, URI uri, List<String> headerList, Supplier<T> factory) {
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
                    respHeaders.setETag("\"" + contributionId + "\"");
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
        if (minimalOrRepresentation != null) {
            // when this "if" is true the following casting can be executed and data manipulated by reference (handled
            // by temporary variable)
            ContributionResponseData objByReference = minimalOrRepresentation;

            // retrieve contribution
            Optional<ContributionDto> contribution = contributionService.getContribution(ehrId, contributionId);

            // set all response field according to retrieved contribution
            objByReference.setUid(new HierObjectId(contributionId.toString()));
            List<ObjectRef<ObjectVersionId>> refs = new LinkedList<>();
            contribution
                    .get()
                    .getObjectReferences()
                    .forEach((id, type) -> refs.add(new ObjectRef<>(new ObjectVersionId(id), "local", type)));
            objByReference.setVersions(refs);
            objByReference.setAudit(contribution.get().getAuditDetails());

            CompositionFormat format = extractCompositionFormat(accept);

            // finally set last header
            if (format.equals(CompositionFormat.XML)) {
                respHeaders.setContentType(MediaType.APPLICATION_XML);
            } else if (format.equals(CompositionFormat.JSON)
                    || format.equals(CompositionFormat.FLAT)
                    || format.equals(CompositionFormat.ECISFLAT)
                    || format.equals(CompositionFormat.RAW)) {
                respHeaders.setContentType(MediaType.APPLICATION_JSON);
            } else {
                throw new NotAcceptableException("Wrong Accept header in request");
            }
        } // else continue with returning but without additional data from above, e.g. body

        return Optional.of(new InternalResponse<>(minimalOrRepresentation, respHeaders));
    }

    private void createAuditLogsMsgBuilder(UUID ehrId, UUID contributionId) {
        AuditMsgBuilder.getInstance()
                .setEhrIds(ehrId)
                .setContributionId(contributionId.toString())
                .setLocation(fromPath(EMPTY)
                        .pathSegment(EHR, ehrId.toString(), CONTRIBUTION, contributionId.toString())
                        .build()
                        .toString());
    }
}
