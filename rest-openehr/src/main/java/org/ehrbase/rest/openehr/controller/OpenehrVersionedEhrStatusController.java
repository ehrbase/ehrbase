package org.ehrbase.rest.openehr.controller;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.RevisionHistory;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.dto.ContributionDto;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.openehr.response.OriginalVersionResponseData;
import org.ehrbase.rest.openehr.response.RevisionHistoryResponseData;
import org.ehrbase.rest.openehr.response.VersionedObjectResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller for /ehr/{ehrId}/versioned_ehr_status resource of openEHR REST API
 */
@Api
@RestController
@RequestMapping(path = "/rest/openehr/v1/ehr/{ehr_id}/versioned_ehr_status", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrVersionedEhrStatusController extends BaseController{

    private final EhrService ehrService;
    private final ContributionService contributionService;

    @Autowired
    public OpenehrVersionedEhrStatusController(EhrService ehrService, ContributionService contributionService) {
        this.ehrService = Objects.requireNonNull(ehrService);
        this.contributionService = Objects.requireNonNull(contributionService);
    }

    @GetMapping
    @ApiOperation(value = "Retrieves a VERSIONED_EHR_STATUS associated with an EHR identified by ehr_id.", response = VersionedObjectResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested VERSIONED_EHR_STATUS is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<VersionedObjectResponseData<EhrStatus>> retrieveVersionedEhrStatusByEhr(
            @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        VersionedEhrStatus versionedEhrStatus = ehrService.getVersionedEhrStatus(ehrId);

        VersionedObjectResponseData<EhrStatus> response = new VersionedObjectResponseData<>(versionedEhrStatus);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(getMediaType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }

    @GetMapping(path = "/revision_history")
    @ApiOperation(value = "Retrieves a VERSIONED_EHR_STATUS associated with an EHR identified by ehr_id.", response = RevisionHistoryResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested VERSIONED_EHR_STATUS is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<RevisionHistoryResponseData> retrieveVersionedEhrStatusRevisionHistoryByEhr(
            @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        RevisionHistory revisionHistory = ehrService.getRevisionHistoryOfVersionedEhrStatus(ehrId);

        RevisionHistoryResponseData response = new RevisionHistoryResponseData(revisionHistory);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(getMediaType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }

    @GetMapping(path = "/version")
    @ApiOperation(value = "Retrieves the VERSION of an EHR_STATUS associated with the EHR identified by ehr_id. If version_at_time is supplied, retrieves the VERSION extant at specified time, otherwise retrieves the latest VERSION.", response = OriginalVersionResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested VERSION is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByTime(
            @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
            @ApiParam(value = "A timestamp in the ISO8601 format", hidden = true) @RequestParam(value = "version_at_time", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime versionAtTime) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        UUID versionedObjectId = ehrService.getEhrStatusVersionedObjectUidByEhr(ehrId);
        int version;
        if (versionAtTime != null) {
            version = ehrService.getEhrStatusVersionByTimestamp(ehrId, Timestamp.valueOf(versionAtTime));
        } else {
            version = Integer.parseInt(ehrService.getLatestVersionUidOfStatus(ehrId).split("::")[2]);
        }

        Optional<OriginalVersion<EhrStatus>> ehrStatusOriginalVersion = ehrService.getEhrStatusAtVersion(ehrId, versionedObjectId, version);
        UUID contributionId = ehrStatusOriginalVersion
                .map(i -> UUID.fromString(i.getContribution().getId().getValue()))
                .orElseThrow(() -> new InvalidApiParameterException("Couldn't retrieve EhrStatus with given parameters"));

        Optional<ContributionDto> optionalContributionDto = contributionService.getContribution(ehrId, contributionId);
        ContributionDto contributionDto = optionalContributionDto.orElseThrow(() -> new InternalServerException("Couldn't fetch contribution for existing EhrStatus")); // shouldn't happen

        OriginalVersionResponseData<EhrStatus> originalVersionResponseData = new OriginalVersionResponseData<>(ehrStatusOriginalVersion.get(), contributionDto);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(getMediaType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(originalVersionResponseData);
    }

    @GetMapping(path = "/version/{version_uid}")
    @ApiOperation(value = "Retrieves a VERSION identified by version_uid of an EHR_STATUS associated with the EHR identified by ehr_id.", response = OriginalVersionResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested VERSION is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByVersionUid(
            @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
            @ApiParam(value = "User supplied VERSION identifier", required = true) @PathVariable(value = "version_uid") String versionUid) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        // parse given version uid
        UUID versionedObjectId;
        int version;
        try {
            versionedObjectId = UUID.fromString(versionUid.split("::")[0]);
            version = Integer.parseInt(versionUid.split("::")[2]);
        } catch (Exception e) {
            throw new InvalidApiParameterException("VERSION UID parameter has wrong format: " + e.getMessage());
        }

        Optional<OriginalVersion<EhrStatus>> ehrStatusOriginalVersion = ehrService.getEhrStatusAtVersion(ehrId, versionedObjectId, version);
        UUID contributionId = ehrStatusOriginalVersion
                .map(i -> UUID.fromString(i.getContribution().getId().getValue()))
                .orElseThrow(() -> new InvalidApiParameterException("Couldn't retrieve EhrStatus with given parameters"));

        Optional<ContributionDto> optionalContributionDto = contributionService.getContribution(ehrId, contributionId);
        ContributionDto contributionDto = optionalContributionDto.orElseThrow(() -> new InternalServerException("Couldn't fetch contribution for existing EhrStatus")); // shouldn't happen

        OriginalVersionResponseData<EhrStatus> originalVersionResponseData = new OriginalVersionResponseData<>(ehrStatusOriginalVersion.get(), contributionDto);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(getMediaType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(originalVersionResponseData);
    }

    private MediaType getMediaType(@RequestHeader(value = HttpHeaders.ACCEPT, required = false) @ApiParam("Client should specify expected response format") String accept) {
        MediaType contentTypeHeaderInput;  // to prepare header input if this header is needed later
        if (StringUtils.isBlank(accept) || accept.equals("*/*")) {  // "*/*" is standard for "any mime-type"
            // assign default if no header was set
            contentTypeHeaderInput = MediaType.APPLICATION_JSON;
        } else {
            // if header was set process it
            MediaType mediaType = MediaType.parseMediaType(accept);

            if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                contentTypeHeaderInput = MediaType.APPLICATION_JSON;
            } else if (mediaType.isCompatibleWith(MediaType.APPLICATION_XML)) {
                contentTypeHeaderInput = MediaType.APPLICATION_XML;
            } else {
                throw new InvalidApiParameterException("Wrong Content-Type header in request");
            }
        }
        return contentTypeHeaderInput;
    }
}
