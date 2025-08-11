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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.ehrbase.api.rest.HttpRestContext.EHR_ID;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.openehr.sdk.response.dto.ContributionResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.ContributionApiSpecification;
import org.ehrbase.rest.util.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnMissingBean(name = "primaryopenehrcontributioncontroller")
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

    @PostMapping(
            value = "/{ehr_id}/contribution",
            consumes = {"application/xml", "application/json"})
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
            @RequestParam(value = PRETTY, required = false) String pretty,
            @RequestBody String contribution) {

        if (!resolveContentType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)) {
            throw new NotAcceptableException(
                    "Invalid content type, only %s is supported".formatted(MediaType.APPLICATION_JSON));
        }

        UUID ehrId = getEhrUuid(ehrIdString);

        UUID contributionId = contributionService.commitContribution(ehrId, contribution);

        URI uri = createLocationUri(EHR, ehrId.toString(), CONTRIBUTION, contributionId.toString());

        List<String> headerList = Arrays.asList(
                LOCATION,
                ETAG); // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled separately

        boolean doReturnRepresentation = RETURN_REPRESENTATION.equals(prefer);

        InternalResponse<ContributionResponseData> respData =
                buildContributionResponseData(contributionId, ehrId, accept, uri, headerList, doReturnRepresentation);

        createRestContext(ehrId, contributionId);

        if (doReturnRepresentation) {
            // 201 with body + headers
            setPrettyPrintResponse(pretty);
            return ResponseEntity.created(uri).headers(respData.getHeaders()).body(respData.getResponseData());
        } else {
            // 204 only with headers
            return ResponseEntity.noContent().headers(respData.getHeaders()).build();
        }
    }

    @GetMapping(value = "/{ehr_id}/contribution/{contribution_uid}")
    @Override
    public ResponseEntity getContribution(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion,
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "contribution_uid") String contributionUidString,
            @RequestParam(value = PRETTY, required = false) String pretty) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID contributionUid = getContributionVersionedObjectUidString(contributionUidString);

        URI uri = createLocationUri(EHR, ehrId.toString(), CONTRIBUTION, contributionUid.toString());

        List<String> headerList = Arrays.asList(
                LOCATION, ETAG, LAST_MODIFIED); // whatever is required by REST spec - CONTENT_TYPE handled separately

        // building full / representation response
        InternalResponse<ContributionResponseData> respData =
                buildContributionResponseData(contributionUid, ehrId, accept, uri, headerList, true);

        createRestContext(ehrId, contributionUid);

        setPrettyPrintResponse(pretty);

        // returns 200 with body
        return ResponseEntity.ok().headers(respData.getHeaders()).body(respData.getResponseData());
    }

    private InternalResponse<ContributionResponseData> buildContributionResponseData(
            UUID contributionId,
            UUID ehrId,
            String accept,
            URI uri,
            List<String> headerList,
            boolean includeResponseData) {

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

        // create either CompositionResponseData or null (means no body, only headers incl. link to resource), via
        // lambda request
        // if response data objects was created as "representation" do all task from wider scope, too

        final ContributionResponseData responseData;
        if (includeResponseData) {
            final MediaType mediaType = resolveContentType(accept);
            respHeaders.setContentType(mediaType);

            // when this "if" is true the following casting can be executed and data manipulated by reference (handled
            // by temporary variable)

            // retrieve contribution
            ContributionDto contribution = contributionService.getContribution(ehrId, contributionId);

            // set all response field according to retrieved contribution
            responseData = new ContributionResponseData(
                    new HierObjectId(contributionId.toString()),
                    contribution.getObjectReferences().entrySet().stream()
                            .map(e -> new ObjectRef<>(new ObjectVersionId(e.getKey()), "local", e.getValue()))
                            .collect(Collectors.toList()),
                    contribution.getAuditDetails());

        } else {
            // else continue with returning but without additional data from above, e.g. body
            responseData = null;
        }

        return new InternalResponse<>(responseData, respHeaders);
    }

    private void createRestContext(UUID ehrId, UUID contributionId) {
        HttpRestContext.register(
                EHR_ID,
                ehrId,
                HttpRestContext.LOCATION,
                fromPath(EMPTY)
                        .pathSegment(EHR, ehrId.toString(), CONTRIBUTION, contributionId.toString())
                        .build()
                        .toString());
    }
}
