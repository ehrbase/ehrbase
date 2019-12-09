package org.ehrbase.rest.openehr.controller;

import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.openehr.response.VersionedObjectResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for /ehr/{ehrId}/versioned_ehr_status resource of openEHR REST API
 */
@Api
@RestController
@RequestMapping(path = "/rest/openehr/v1/ehr/{ehr_id}/versioned_ehr_status", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrVersionedEhrStatusController extends BaseController{

    private final EhrService ehrService;

    @Autowired
    public OpenehrVersionedEhrStatusController(EhrService ehrService) {
        this.ehrService = Objects.requireNonNull(ehrService);
    }

    @GetMapping
    @ApiOperation(value = "Retrieves a VERSIONED_EHR_STATUS associated with an EHR identified by ehr_id.", response = VersionedObjectResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested VERSIONED_EHR_STATUS is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<VersionedObjectResponse<EhrStatus>> retrieveVersionedEhrStatusByEhr(
            @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // check if EHR is valid
        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        // FIXME VERSION_OBJECT_POC: call service method that retrieves meta data about last (and only) entry in ehr_status table, matching ehr id

        VersionedEhrStatus versionedEhrStatus = ehrService.getVersionedEhrStatus(ehrId);

        VersionedObjectResponse<EhrStatus> response = new VersionedObjectResponse<>(versionedEhrStatus);

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

        // create and supplement headers with data depending on which headers are requested
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(contentTypeHeaderInput);

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }
}
