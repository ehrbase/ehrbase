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

import org.ehrbase.api.dto.FolderDto;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.FolderService;
import org.ehrbase.rest.openehr.annotation.RequestUrl;
import org.ehrbase.rest.openehr.controller.OperationNotesResourcesReaderOpenehr.ApiNotes;
import org.ehrbase.rest.openehr.response.DirectoryResponseData;
import org.ehrbase.rest.openehr.response.ErrorResponseData;
import com.nedap.archie.rm.directory.Folder;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    @PostMapping(value = "/{ehr_id}/directory", consumes = {"application/xml", "application/json"})
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
    public ResponseEntity createFolder(
            @ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openEhrVersion,
            @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openEhrAuditDetails,
            @ApiParam(value = REQ_CONTENT_TYPE_BODY) @RequestHeader(value = CONTENT_TYPE) String contentType,
            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @ApiParam(value = REQ_PREFER) @RequestHeader(value = PREFER, required = false, defaultValue = RETURN_MINIMAL) String prefer,
            @ApiParam(value = "EHR identifier from resource path after ehr/", required = true) @PathVariable(value = "ehr_id") UUID ehrId,
            @ApiParam(value = "The FOLDER to create.", required = true) @RequestBody Folder folder,
            @RequestUrl String requestUrl
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
        Optional<FolderDto> newFolder = this.folderService.retrieve(folderId, 0);

        if (!newFolder.isPresent()) {
            throw new InternalServerException(
                    "Something went wrong. Folder could be persisted but not fetched again."
            );
        }

        // Create response data
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.setLocation(URI.create(requestUrl +
                                          "/" +
                                          newFolder.get()
                                                   .getUid()
                                                   .toString()));
        resHeaders.setETag("\"" +
                           newFolder.get()
                                    .getUid()
                                    .toString() +
                           "\"");
        // TODO: Set LastModified header

        // Check for desired response representation format from PREFER header
        if (prefer.equals(RETURN_REPRESENTATION)) {

            FolderDto folderDto = newFolder.get();
            // Evaluate target format from accept header
            MediaType resContentType = MediaType.APPLICATION_JSON;

            switch (accept) {
                case MediaType.APPLICATION_JSON_VALUE:
                    break;
                case MediaType.APPLICATION_XML_VALUE:
                    resContentType = MediaType.APPLICATION_XML;
                    break;
                default:
                    throw new NotAcceptableException(
                            "Media type " + accept + " not supported."
                    );
            }

            DirectoryResponseData resBody = buildResponse(folderDto);

            resHeaders.setContentType(resContentType);

            return new ResponseEntity<>(
                    resBody,
                    resHeaders,
                    HttpStatus.CREATED
            );
        }
        // No representation desired
        return new ResponseEntity<>(
                null,
                resHeaders,
                HttpStatus.NO_CONTENT
        );
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

        // Tries to create an UUID from versionUid and throws an IllegalArgumentException for 400 error
        UUID versionUUID = UUID.fromString(versionUid);

        // Get response data format for deserialization; defaults to JSON
        MediaType responseContentType = MediaType.APPLICATION_JSON;

        switch (accept) {
            case MediaType.APPLICATION_XML_VALUE:
                responseContentType = MediaType.APPLICATION_XML;
                break;
            case MediaType.APPLICATION_JSON_VALUE:
                break;
            default:
                throw new NotAcceptableException("Media type " + accept + " not supported.");
        }

        // Check if EHR for the folder exists
        if (!ehrService.doesEhrExist(ehrId)) {
            throw new ObjectNotFoundException(
                    "ehr",
                    "EHR with id " + ehrId + " not found."
            );
        }

        // Get the folder entry from database
        Optional<FolderDto> foundFolder = folderService.retrieve(versionUUID, 1);
        if (!foundFolder.isPresent()) {
            throw new ObjectNotFoundException("folder",
                                              "The FOLDER with id " +
                                              versionUUID.toString() +
                                              " does not exist.");
        }

        FolderDto folderDto = foundFolder.get();

        // Create response data
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(responseContentType);
        headers.setETag("\"" + folderDto.getUid().toString() + "\"");

        DirectoryResponseData resBody = buildResponse(folderDto);

        return new ResponseEntity<>(resBody, headers, HttpStatus.OK);

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
    public ResponseEntity getFolderVersionAtTime(
            @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @ApiParam(value = "EHR identifier from resource path after ehr/", required = true) @PathVariable(value = "ehr_id") UUID ehrId,
            @ApiParam(value = "Timestamp in extended ISO8601 format to identify version of folder.") @RequestParam(value = "version_at_time", required = false) String versionAtTime,
            @ApiParam(value = "Path parameter to specify a subfolder at directory") @RequestParam(value = "path", required = false) String path
                                                ) {
        // UUID ehrId = getEhrUuid(ehrIdString);
        // TODO: Implement get folder by version at time functionality
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                             .build();
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
    public ResponseEntity updateFolder(
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

        UUID folderId = UUID.fromString(ifMatch);

        // Update folder and get new version
        Optional<FolderDto> updatedFolder = this.folderService.update(
                folderId,
                folder,
                ehrId
                                                                     );


        if (!updatedFolder.isPresent()) {
            throw new InternalServerException(
                    "Something went wrong. Folder could be persisted but not fetched again."
            );
        }

        // Create response data
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.setLocation(URI.create(requestUrl +
                                          "/" +
                                          updatedFolder.get()
                                                       .getUid()
                                                       .toString()));
        resHeaders.setETag("\"" +
                           updatedFolder.get()
                                        .getUid()
                                        .toString() +
                           "\"");
        // TODO: Set LastModified header

        // Check for desired response representation format from PREFER header
        if (prefer.equals(RETURN_REPRESENTATION)) {

            FolderDto folderDto = updatedFolder.get();

            // Evaluate target format from accept header
            MediaType resContentType = MediaType.APPLICATION_JSON;

            switch (accept) {
                case MediaType.APPLICATION_JSON_VALUE:
                    break;
                case MediaType.APPLICATION_XML_VALUE:
                    resContentType = MediaType.APPLICATION_XML;
                    break;
                default:
                    throw new NotAcceptableException(
                            "Media type " + accept + " not supported."
                    );
            }

            DirectoryResponseData resBody = buildResponse(folderDto);

            resHeaders.setContentType(resContentType);

            return new ResponseEntity<>(
                    resBody,
                    resHeaders,
                    HttpStatus.OK
            );
        }
        // No representation desired
        return new ResponseEntity<>(
                null,
                resHeaders,
                HttpStatus.NO_CONTENT
        );
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
        // TODO: Implement delete FOLDER fuctionality
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                             .build();
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

}
