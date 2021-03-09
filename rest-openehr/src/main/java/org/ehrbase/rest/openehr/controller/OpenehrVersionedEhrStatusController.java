package org.ehrbase.rest.openehr.controller;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.RevisionHistory;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.ehrscape.ContributionDto;
import org.ehrbase.response.openehr.OriginalVersionResponseData;
import org.ehrbase.response.openehr.RevisionHistoryResponseData;
import org.ehrbase.response.openehr.VersionedObjectResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for /ehr/{ehrId}/versioned_ehr_status resource of openEHR REST API
 */
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
    public ResponseEntity<VersionedObjectResponseData<EhrStatus>> retrieveVersionedEhrStatusByEhr(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString) {

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
    public ResponseEntity<RevisionHistoryResponseData> retrieveVersionedEhrStatusRevisionHistoryByEhr(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString) {

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
    public ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByTime(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestParam(value = "version_at_time", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime versionAtTime) {

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
    public ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByVersionUid(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "version_uid") String versionUid) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found.");
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

        if (version < 1)
            throw new InvalidApiParameterException("Version can't be negative.");

        if(!ehrService.hasStatus(versionedObjectId)) {
            throw new ObjectNotFoundException("ehr_status", "No EHR_STATUS with given ID can be found.");
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

    private MediaType getMediaType(@RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {
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
