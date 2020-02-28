/*
 * Copyright (c) 2019 Vitasystems GmbH,  Jake Smolka (Hannover Medical School), and Luis Marco-Ruiz(Hannover Medical School).
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

import com.nedap.archie.rm.directory.Folder;
import io.swagger.annotations.*;
import org.ehrbase.api.dto.FolderDto;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.FolderService;
import org.ehrbase.rest.openehr.annotation.RequestUrl;
import org.ehrbase.rest.openehr.controller.OperationNotesResourcesReaderOpenehr.ApiNotes;
import org.ehrbase.rest.openehr.response.DirectoryResponseData;
import org.ehrbase.rest.openehr.response.ErrorResponseData;
import org.ehrbase.rest.openehr.util.VersionUidHelper;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Controller for openEHR /directory endpoints
 */
@Api(tags = "Directory")
@RestController
@RequestMapping(path = "/rest/openehr/v1/ehr")
public class OpenehrDirectoryController extends BaseController {

    private final FolderService folderService;
    private final EhrService ehrService;

    @Autowired
    public OpenehrDirectoryController(FolderService folderService, EhrService ehrService) {
        this.folderService = Objects.requireNonNull(folderService);
        this.ehrService = Objects.requireNonNull(ehrService);
    }

    @PostMapping(value = "/{ehr_id}/directory")
    @ApiOperation("Create a new directory folder associated with the EHR identified by ehr_id.")
    @ApiNotes("directoryPost.md")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 201,
                    response = DirectoryResponseData.class,
                    message = "Created successfully - new FOLDER created was created. Content body is only returned when Prefer header has return=representation, otherwise only headers are returned.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }
            ),
            @ApiResponse(
                    code = 204,
                    message = "No Content - New FOLDER was created but not full representation requested. Details in response headers.",
                    responseHeaders = {
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }
            ),
            @ApiResponse(
                    code = 400,
                    message = "Bad Request - New FOLDER could not be created due to a malformed request data. Request must be modified to match the expected formats.",
                    response = ErrorResponseData.class
            ),
            @ApiResponse(
                    code = 404,
                    message = "Not Found - New FOLDER could not be created due to the EHR identified by the ehr_id could not be found.",
                    response = ErrorResponseData.class
            )
    })
    @ResponseStatus(value = HttpStatus.CREATED)
    public ResponseEntity<DirectoryResponseData> createFolder(
            @ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openEhrVersion,
            @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openEhrAuditDetails,
            @ApiParam(value = REQ_CONTENT_TYPE_BODY) @RequestHeader(value = CONTENT_TYPE) String contentType,
            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @ApiParam(value = REQ_PREFER) @RequestHeader(value = PREFER, required = false, defaultValue = RETURN_MINIMAL) String prefer,
            @ApiParam(value = "EHR identifier from resource path after ehr/", required = true) @PathVariable(value = "ehr_id") UUID ehrId,
            @ApiParam(value = "The FOLDER to create.", required = true) @RequestBody Folder folder
    ) {

        // Check for existence of EHR record
        if (!this.ehrService.doesEhrExist(ehrId)) {
            throw new ObjectNotFoundException(
                    "ehr",
                    "EHR with id " + ehrId + " not found."
            );
        }
        // Insert New folder
        UUID folderId = this.folderService.create(
                ehrId,
                folder
        );

        // Fetch inserted folder for response data
        Optional<FolderDto> newFolder = this.folderService.retrieve(folderId, 1, null);

        if (newFolder.isEmpty()) {
            throw new InternalServerException(
                    "Something went wrong. Folder could be persisted but not fetched again."
            );
        }

        // No representation desired
        return createDirectoryResponse(HttpMethod.POST, prefer, accept, newFolder.get(), ehrId);
    }

    @GetMapping(path = "/{ehr_id}/directory/{version_uid}{?path}")
    @ApiOperation("Get an existing FOLDER in EHR identified by id ")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    response = DirectoryResponseData.class,
                    message = "Success - FOLDER found and will be returned inside response body.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }
            ),
            @ApiResponse(
                    code = 204,
                    message = "No Content - No FOLDER found at specified path."
            ),
            @ApiResponse(
                    code = 404,
                    message = ("Not Found - Either specified EHR with ehr_id or directory with the directory_id do not exist.")
            )
    })
    public ResponseEntity<DirectoryResponseData> getFolder(
            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @ApiParam(value = "EHR identifier from resource path after ehr/", required = true) @PathVariable(value = "ehr_id") UUID ehrId,
            @ApiParam(value = "DIRECTORY identifier from resource path after directory/", required = true) @PathVariable(value = "version_uid") String versionUid,
            @ApiParam(value = "Path parameter to specify a subfolder at directory") @RequestParam(value = "path", required = false) String path
    ) {

        // Path value
        if (path != null && !isValidPath(path)) {
            throw new IllegalArgumentException("Value for path is malformed. Expecting a unix like notation, e.g. '/episodes/a/b/c'");
        }
        // Tries to create an UUID from versionUid and throws an IllegalArgumentException for 400 error
        UUID versionUUID = extractVersionedObjectUidFromVersionUid(versionUid);

        // Check if EHR for the folder exists
        if (!ehrService.doesEhrExist(ehrId)) {
            throw new ObjectNotFoundException(
                    "ehr",
                    "EHR with id " + ehrId + " not found."
            );
        }

        // Get the folder entry from database
        Optional<FolderDto> foundFolder = folderService.retrieve(versionUUID, 1, path);
        if (foundFolder.isEmpty()) {
            throw new ObjectNotFoundException("folder",
                    "The FOLDER with id " +
                            versionUUID.toString() +
                            " does not exist.");
        }

        return createDirectoryResponse(HttpMethod.GET, RETURN_REPRESENTATION, accept, foundFolder.get(), ehrId);
    }

    @GetMapping(path = "/{ehr_id}/directory{?version_at_time,path}")
    @ApiOperation("Get an existing FOLDER in EHR which was actual at given version at time.")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    response = DirectoryResponseData.class,
                    message = "Success - FOLDER found and will be returned inside response body.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }
            ),
            @ApiResponse(
                    code = 204,
                    message = "No Content - EHR has no version at time or no folder at path"
            ),
            @ApiResponse(
                    code = 404,
                    message = ("Not Found - Specified EHR with ehr_id does not exist.")
            )
    })
    public ResponseEntity<DirectoryResponseData> getFolderVersionAtTime(
            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @ApiParam(value = "EHR identifier from resource path after ehr/", required = true) @PathVariable(value = "ehr_id") UUID ehrId,
            @ApiParam(value = "Timestamp in extended ISO8601 format to identify version of folder.") @RequestParam(value = "version_at_time", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime versionAtTime,
            @ApiParam(value = "Path parameter to specify a subfolder at directory") @RequestParam(value = "path", required = false) String path
    ) {
        // Check path string if they are valid
        if (path != null && !isValidPath(path)) {
            throw new IllegalArgumentException("Value for path is malformed. Expecting a unix like notation, e.g. '/episodes/a/b/c'");
        }

        // Get directory root entry for ehr
        UUID rootDirectoryId = ehrService.getDirectoryId(ehrId);
        final Optional<FolderDto> foundFolder;
        // Get the folder entry from database
        if (versionAtTime != null) {
            foundFolder = folderService.retrieveByTimestamp(rootDirectoryId, Timestamp.from(versionAtTime.toInstant()), path);
        } else {
            foundFolder = folderService.retrieveLatest(ehrId, path);
        }
        if (foundFolder.isEmpty()) {
            throw new ObjectNotFoundException("folder",
                    "The FOLDER for ehrId " +
                            ehrId.toString() +
                            " does not exist.");
        }

        return createDirectoryResponse(HttpMethod.GET, RETURN_REPRESENTATION, accept, foundFolder.get(), ehrId);
    }

    @PutMapping(path = "/{ehr_id}/directory")
    @ApiOperation("Update an existing folder in directory. The folder will be identified by the latest version_uid specified in the If-Match header")
    @ApiNotes("directoryPut.md")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    response = DirectoryResponseData.class,
                    message = "Success - FOLDER has been updated successfully.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }
            ),
            @ApiResponse(
                    code = 204,
                    message = "Success - FOLDER has been updated successfully but no representation has been requested. Details at response headers.",
                    responseHeaders = {
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }
            ),
            @ApiResponse(
                    code = 400,
                    response = ErrorResponseData.class,
                    message = "Bad Request - FOLDER could not be updated due to one or more malformed request parameters or data."
            ),
            @ApiResponse(
                    code = 404,
                    response = ErrorResponseData.class,
                    message = "Not Found - EHR with given id from path could not be found."
            ),
            @ApiResponse(
                    code = 412,
                    response = ErrorResponseData.class,
                    message = "Precondition failed - The version specified in the If-Match header does not match the latest version of the FOLDER. Returns the latest version_uid in Location and ETag header.",
                    responseHeaders = {
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = URI.class)
                    }
            )
    })
    public ResponseEntity<DirectoryResponseData> updateFolder(
            @ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openEhrVersion,
            @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openEhrAuditDetails,
            @ApiParam(value = REQ_CONTENT_TYPE_BODY) @RequestHeader(value = CONTENT_TYPE) String contentType,
            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @ApiParam(value = REQ_PREFER) @RequestHeader(value = PREFER, required = false, defaultValue = RETURN_MINIMAL) String prefer,
            @ApiParam(value = "{preceding_version_uid}", required = true) @RequestHeader(value = IF_MATCH) String ifMatch,
            @ApiParam(value = "EHR identifier from resource path after ehr/", required = true) @PathVariable(value = "ehr_id") UUID ehrId,
            @ApiParam(value = "Update data for the target FOLDER") @RequestBody Folder folder,
            @RequestUrl String requestUrl
    ) {

        // Check for existence of EHR record
        if (!this.ehrService.doesEhrExist(ehrId)) {
            throw new ObjectNotFoundException(
                    "ehr",
                    "EHR with id " + ehrId + " not found"
            );
        }

        UUID folderId = extractVersionedObjectUidFromVersionUid(ifMatch);

        // Update folder and get new version
        Optional<FolderDto> updatedFolder = this.folderService.update(
                folderId,
                folder,
                ehrId
        );


        if (updatedFolder.isEmpty()) {
            throw new InternalServerException(
                    "Something went wrong. Folder could be persisted but not fetched again."
            );
        }

        return createDirectoryResponse(HttpMethod.PUT, prefer, accept, updatedFolder.get(), ehrId);
    }

    @DeleteMapping(path = "/{ehr_id}/directory")
    @ApiOperation("Delete an existing folder in directory. The folder will be identified by the latest version_uid specified in the If-Match header")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 204,
                    message = "Success - DIRECTORY has been deleted successfully.",
                    responseHeaders = {
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }
            ),
            @ApiResponse(
                    code = 400,
                    response = ErrorResponseData.class,
                    message = "Bad Request - FOLDER could not be deleted due to one or more malformed request parameters or data."
            ),
            @ApiResponse(
                    code = 404,
                    response = ErrorResponseData.class,
                    message = "Not Found - EHR with given id from path could not be found."
            ),
            @ApiResponse(
                    code = 412,
                    response = ErrorResponseData.class,
                    message = "Precondition failed - The version specified in the If-Match header does not match the latest version of the FOLDER. Returns the latest version_uid in Location and ETag header.",
                    responseHeaders = {
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = URI.class)
                    }
            )
    })
    public ResponseEntity deleteFolder(
            @ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openEhrVersion,
            @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openEhrAuditDetails,
            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @ApiParam(value = "{preceding_version_uid}", required = true) @RequestHeader(value = IF_MATCH) String ifMatch,
            @ApiParam(value = "EHR identifier from resource path after ehr/", required = true) @PathVariable(value = "ehr_id") String ehrIdString
    ) {
        UUID ehrId = getEhrUuid(ehrIdString);

        if (!this.ehrService.doesEhrExist(ehrId)) {
            throw new ObjectNotFoundException("EHR with id " + ehrIdString + " not found",
                    "FOLDER");
        }

        this.folderService.delete(VersionUidHelper.extractUUID(ifMatch));
        return createDirectoryResponse(HttpMethod.DELETE, null, accept, null, ehrId);
    }

    private DirectoryResponseData buildResponse(FolderDto folderDto) {

        DirectoryResponseData resBody = new DirectoryResponseData();
        resBody.setDetails(folderDto.getDetails());
        resBody.setFolders(folderDto.getFolders());
        resBody.setItems(folderDto.getItems());
        resBody.setName(folderDto.getName());
        resBody.setUid(folderDto.getUid());
        return resBody;
    }

    private ResponseEntity<DirectoryResponseData> createDirectoryResponse(HttpMethod method, String prefer, String accept, FolderDto folderDto, UUID ehrId) {

        HttpHeaders headers = new HttpHeaders();
        HttpStatus successStatus;
        DirectoryResponseData body;

        if (prefer != null && prefer.equals(RETURN_REPRESENTATION)) {
            headers.setContentType(extractMediaType(accept));
            body = buildResponse(folderDto);
            successStatus = getSuccessStatus(method);
        } else {
            body = null;
            successStatus = HttpStatus.NO_CONTENT;
        }

        if (folderDto != null) {
            String versionUid = folderDto.getUid().toString() +
                    "::" + this.folderService.getServerConfig().getNodename() +
                    "::" + folderService.getLastVersionNumber(UUID.fromString(folderDto.getUid().toString())
            );

            headers.setETag("\"" + versionUid + "\"");
            headers.setLocation(
                    URI.create(encodePath(getBaseEnvLinkURL() +
                            "/rest/openehr/v1/ehr/" + ehrId.toString() +
                            "/directory/" + versionUid))
            );
            // TODO: Extract last modified from SysPeriod timestamp of fetched folder record
            headers.setLastModified(DateTime.now().getMillis());
        }

        return new ResponseEntity<>(body, headers, successStatus);

    }

    private HttpStatus getSuccessStatus(HttpMethod method) {

        switch (method) {
            case POST: {
                return HttpStatus.CREATED;
            }
            case DELETE: {
                return HttpStatus.NO_CONTENT;
            }
            default: {
                return HttpStatus.OK;
            }
        }
    }

    /**
     * Checks if a given path is a valid path value, i.e. a unix like notation of a path which allows trailing forward
     * slashes as well as only one slash which is equivalent to the root folder and would not have any effect on the
     * result.
     *
     * @param path - String to check
     * @return String is a valid path value or not
     */
    private boolean isValidPath(String path) {
        Pattern pathPattern = Pattern.compile("^(?:/?(?:\\w+|\\s)*/?)+$");
        return pathPattern.matcher(path).matches();
    }
}

