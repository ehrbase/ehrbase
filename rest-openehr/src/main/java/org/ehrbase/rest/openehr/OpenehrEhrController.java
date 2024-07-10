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

import static org.ehrbase.api.rest.HttpRestContext.EHR_ID;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.support.identification.HierObjectId;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.sdk.response.dto.EhrResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.EhrApiSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
 * Controller for /ehr resource of openEHR REST API
 */
@ConditionalOnMissingBean(name = "primaryopenehrehrcontroller")
@RestController
@RequestMapping(
        path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/" + BaseController.EHR,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrEhrController extends BaseController implements EhrApiSpecification {

    private final EhrService ehrService;
    private final SystemService systemService;

    @Autowired
    public OpenehrEhrController(EhrService ehrService, SystemService systemService) {
        this.ehrService = Objects.requireNonNull(ehrService);
        this.systemService = systemService;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseStatus(value = HttpStatus.CREATED)
    public ResponseEntity<EhrResponseData> createEhr(
            @RequestHeader(value = BaseController.OPENEHR_VERSION, required = false) String openehrVersion,
            @RequestHeader(value = BaseController.OPENEHR_AUDIT_DETAILS, required = false) String openehrAuditDetails,
            @RequestHeader(value = PREFER, required = false, defaultValue = RETURN_MINIMAL) String prefer,
            @RequestBody(required = false) EhrStatus ehrStatus) {

        UUID ehrId = ehrService.create(null, ehrStatus);

        HttpRestContext.register(EHR_ID, ehrId);

        // initialize HTTP 201 Created body builder
        ResponseEntity.BodyBuilder bodyBuilder = ehrResponseBuilder(HttpStatus.CREATED, ehrId);

        // return either representation body or only the created response
        if (RETURN_REPRESENTATION.equals(prefer)) {
            EhrResponseData ehrResponseData = ehrResponseData(ehrId);
            return bodyBuilder.body(ehrResponseData);
        } else {
            return bodyBuilder.build();
        }
    }

    @PutMapping(path = "/{ehr_id}")
    @ResponseStatus(value = HttpStatus.CREATED)
    public ResponseEntity<EhrResponseData> createEhrWithId(
            @RequestHeader(value = BaseController.OPENEHR_VERSION, required = false) String openehrVersion,
            @RequestHeader(value = BaseController.OPENEHR_AUDIT_DETAILS, required = false) String openehrAuditDetails,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @RequestBody(required = false) EhrStatus ehrStatus) {

        // can't use getEhrUuid(..) because here another exception needs to be thrown (-> 400, not 404 in response)
        UUID newEhrId = parseUUID(ehrIdString, "EHR ID format not a UUID");
        UUID ehrId = ehrService.create(newEhrId, ehrStatus);

        createRestContext(ehrId);

        // initialize HTTP 201 Created body builder
        ResponseEntity.BodyBuilder bodyBuilder = ehrResponseBuilder(HttpStatus.CREATED, ehrId);

        // return either representation body or only the created response
        if (RETURN_REPRESENTATION.equals(prefer)) {
            EhrResponseData ehrResponseData = ehrResponseData(ehrId);
            return bodyBuilder.body(ehrResponseData);
        } else {
            return bodyBuilder.build();
        }
    }

    @GetMapping(path = "/{ehr_id}")
    public ResponseEntity<EhrResponseData> getEhrById(@PathVariable(value = "ehr_id") String ehrIdString) {

        UUID ehrId = getEhrUuid(ehrIdString);

        // load the EHR response
        EhrResponseData ehrResponseData = ehrResponseData(ehrId);

        // Return HTTP 200 OK body builder
        return ehrResponseBuilder(HttpStatus.OK, ehrId).body(ehrResponseData);
    }

    /**
     * Returns EHR by subject (id and namespace)
     */
    @GetMapping(params = {"subject_id", "subject_namespace"})
    public ResponseEntity<EhrResponseData> getEhrBySubject(
            @RequestParam(value = "subject_id") String subjectId,
            @RequestParam(value = "subject_namespace") String subjectNamespace) {

        UUID ehrId = ehrService
                .findBySubject(subjectId, subjectNamespace)
                .orElseThrow(() -> new ObjectNotFoundException("ehr", "No EHR with supplied subject parameters found"));

        // load the EHR response
        EhrResponseData ehrResponseData = ehrResponseData(ehrId);

        // Return HTTP 200 OK body builder
        return ehrResponseBuilder(HttpStatus.OK, ehrId).body(ehrResponseData);
    }

    private EhrResponseData ehrResponseData(UUID ehrId) {

        // populate maximum response data
        EhrResponseData responseData = new EhrResponseData();
        responseData.setEhrId(new HierObjectId(ehrId.toString()));
        responseData.setSystemId(new HierObjectId(systemService.getSystemId()));
        responseData.setEhrStatus(ehrService.getEhrStatus(ehrId));
        responseData.setTimeCreated(ehrService.getCreationTime(ehrId));
        return responseData;
    }

    private ResponseEntity.BodyBuilder ehrResponseBuilder(HttpStatus status, UUID ehrId) {

        URI uri = createLocationUri(EHR, ehrId.toString());

        // initialize HTTP 201 Created body builder
        return ResponseEntity.status(status)
                .location(uri)
                .eTag("\"%s\"".formatted(ehrId.toString()))
                // TODO should be VERSION.commit_audit.time_committed.value which is not implemented yet - mock for now
                .lastModified(123124442);
    }

    private void createRestContext(UUID resultEhrId) {
        HttpRestContext.register(
                EHR_ID,
                resultEhrId,
                HttpRestContext.LOCATION,
                fromPath("").pathSegment(EHR, resultEhrId.toString()).build().toString());
    }
}
