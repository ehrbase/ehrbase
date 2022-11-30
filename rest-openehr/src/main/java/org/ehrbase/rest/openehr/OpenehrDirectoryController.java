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

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.FolderService;
import org.ehrbase.response.ehrscape.FolderDto;
import org.ehrbase.response.openehr.DirectoryResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.DirectoryApiSpecification;
import org.joda.time.DateTime;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for openEHR /directory endpoints
 *
 * @author Jake Smolka
 * @author Luis Marco-Ruiz
 * @author Renaud Subiger
 * @since 1.0
 */
@TenantAware
@RestController
@RequestMapping(path = "${openehr-api.context-path:/rest/openehr}/v1/ehr")
public class OpenehrDirectoryController extends BaseController implements DirectoryApiSpecification {

    private final FolderService folderService;

    private final EhrService ehrService;

    public OpenehrDirectoryController(FolderService folderService, EhrService ehrService) {
        this.folderService = folderService;
        this.ehrService = ehrService;
    }

    /**
     * {@inheritDoc}
     */
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_DIRECTORY_CREATE)
    @Override
    @PostMapping(path = "/{ehr_id}/directory")
    public ResponseEntity<DirectoryResponseData> createDirectory(
            @PathVariable(name = "ehr_id") UUID ehrId,
            @RequestHeader(name = OPENEHR_VERSION, required = false) String openEhrVersion,
            @RequestHeader(name = OPENEHR_AUDIT_DETAILS, required = false) String openEhrAuditDetails,
            @RequestHeader(name = HttpHeaders.CONTENT_TYPE) String contentType,
            @RequestHeader(name = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @RequestHeader(name = PREFER, defaultValue = RETURN_MINIMAL) String prefer,
            @RequestBody Folder folder) {

        var createdFolder = folderService
                .create(ehrId, folder)
                .orElseThrow(() -> new InternalServerException("An error occurred while creating folder"));

        return createDirectoryResponse(HttpMethod.POST, prefer, accept, createdFolder, ehrId);
    }

    /**
     * {@inheritDoc}
     */
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_DIRECTORY_UPDATE)
    @Override
    @PutMapping(path = "/{ehr_id}/directory")
    public ResponseEntity<DirectoryResponseData> updateDirectory(
            @PathVariable(name = "ehr_id") UUID ehrId,
            @RequestHeader(name = HttpHeaders.IF_MATCH) ObjectVersionId folderId,
            @RequestHeader(name = HttpHeaders.CONTENT_TYPE) String contentType,
            @RequestHeader(name = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @RequestHeader(name = PREFER, defaultValue = RETURN_MINIMAL) String prefer,
            @RequestHeader(name = OPENEHR_VERSION, required = false) String openEhrVersion,
            @RequestHeader(name = OPENEHR_AUDIT_DETAILS, required = false) String openEhrAuditDetails,
            @RequestBody Folder folder) {

        // Check version conflicts if EHR and directory exist
        checkDirectoryVersionConflicts(folderId, ehrId);

        // Update folder and get new version
        Optional<FolderDto> updatedFolder = folderService.update(ehrId, folderId, folder);

        if (updatedFolder.isEmpty()) {
            throw new InternalServerException("Something went wrong. Folder could be persisted but not fetched again.");
        }

        return createDirectoryResponse(HttpMethod.PUT, prefer, accept, updatedFolder.get(), ehrId);
    }

    /**
     * {@inheritDoc}
     */
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_DIRECTORY_DELETE)
    @Override
    @DeleteMapping(path = "/{ehr_id}/directory")
    public ResponseEntity<DirectoryResponseData> deleteDirectory(
            @PathVariable(name = "ehr_id") UUID ehrId,
            @RequestHeader(name = OPENEHR_VERSION, required = false) String openEhrVersion,
            @RequestHeader(name = OPENEHR_AUDIT_DETAILS, required = false) String openEhrAuditDetails,
            @RequestHeader(name = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @RequestHeader(name = HttpHeaders.IF_MATCH) ObjectVersionId folderId) {

        // Check version conflicts if EHR and directory exist
        checkDirectoryVersionConflicts(folderId, ehrId);

        // actually delete the EHR root folder
        folderService.delete(ehrId, folderId);

        return createDirectoryResponse(HttpMethod.DELETE, null, accept, null, ehrId);
    }

    /**
     * {@inheritDoc}
     */
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_DIRECTORY_READ)
    @Override
    @GetMapping(path = "/{ehr_id}/directory/{version_uid}")
    public ResponseEntity<DirectoryResponseData> getFolderInDirectory(
            @PathVariable(name = "ehr_id") UUID ehrId,
            @PathVariable(name = "version_uid") ObjectVersionId versionUid,
            @RequestParam(name = "path", required = false) String path,
            @RequestHeader(name = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept) {

        // Check if EHR for the folder exists
        ehrService.checkEhrExists(ehrId);

        assertValidPath(path);

        // Get the folder entry from database
        Optional<FolderDto> foundFolder = folderService.get(versionUid, path);
        if (foundFolder.isEmpty()) {
            throw new ObjectNotFoundException(
                    "DIRECTORY", String.format("Folder with id %s does not exist.", versionUid.toString()));
        }

        return createDirectoryResponse(HttpMethod.GET, RETURN_REPRESENTATION, accept, foundFolder.get(), ehrId);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_DIRECTORY_READ)
    @Override
    @GetMapping(path = "/{ehr_id}/directory")
    public ResponseEntity<DirectoryResponseData> getFolderInDirectoryVersionAtTime(
            @PathVariable(name = "ehr_id") UUID ehrId,
            @RequestParam(name = "version_at_time", required = false) String versionAtTime,
            @RequestParam(name = "path", required = false) String path,
            @RequestHeader(name = ACCEPT, required = false, defaultValue = MediaType.APPLICATION_JSON_VALUE)
                    String accept) {

        // Check ehr exists
        ehrService.checkEhrExists(ehrId);

        assertValidPath(path);

        // Get directory root entry for ehr
        UUID directoryUuid = ehrService.getDirectoryId(ehrId);
        if (directoryUuid == null) {
            throw new ObjectNotFoundException(
                    "DIRECTORY",
                    String.format(
                            "There is no directory stored for EHR with id %s. Maybe it has been deleted?",
                            ehrId.toString()));
        }
        ObjectVersionId directoryId = new ObjectVersionId(directoryUuid.toString());

        final Optional<FolderDto> foundFolder;
        // Get the folder entry from database
        Optional<OffsetDateTime> temporal = getVersionAtTimeParam();
        if (versionAtTime != null && temporal.isPresent()) {
            foundFolder = folderService.getByTimeStamp(
                    directoryId, Timestamp.from(temporal.get().toInstant()), path);
        } else {
            foundFolder = folderService.getLatest(directoryId, path);
        }
        if (foundFolder.isEmpty()) {
            throw new ObjectNotFoundException(
                    "folder", "The FOLDER for ehrId " + ehrId.toString() + " does not exist.");
        }

        return createDirectoryResponse(HttpMethod.GET, RETURN_REPRESENTATION, accept, foundFolder.get(), ehrId);
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

    private ResponseEntity<DirectoryResponseData> createDirectoryResponse(
            HttpMethod method, String prefer, String accept, FolderDto folderDto, UUID ehrId) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus successStatus;
        DirectoryResponseData body;

        if (prefer != null && prefer.equals(RETURN_REPRESENTATION)) {
            headers.setContentType(resolveContentType(accept, MediaType.APPLICATION_XML));
            body = buildResponse(folderDto);
            successStatus = getSuccessStatus(method);
        } else {
            body = null;
            successStatus = HttpStatus.NO_CONTENT;
        }

        if (folderDto != null) {
            String versionUid = folderDto.getUid().toString();

            headers.setETag("\"" + versionUid + "\"");
            headers.setLocation(URI.create(encodePath(
                    getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId.toString() + "/directory/" + versionUid)));
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
     * Assert that the given path is valid.
     *
     * @param path the path to check
     * @throws InvalidApiParameterException if the path is invalid
     */
    private void assertValidPath(String path) {
        if (path == null) {
            return;
        }

        try {
            Paths.get(path);
        } catch (InvalidPathException e) {
            throw new InvalidApiParameterException("The value of path parameter is invalid", e);
        }
    }

    private void checkDirectoryVersionConflicts(ObjectVersionId requestedFolderId, UUID ehrId) {
        UUID directoryUuid;
        if (!ehrService.hasEhr(ehrId) || (directoryUuid = ehrService.getDirectoryId(ehrId)) == null) {
            // Let the service layer handle this, to ensure same behaviour across the application
            return;
        }
        int latestVersion = folderService.getLastVersionNumber(new ObjectVersionId(directoryUuid.toString()));
        // TODO: Change column 'directory' in EHR to String with ObjectVersionId
        String directoryId = String.format(
                "%s::%s::%d", directoryUuid, ehrService.getServerConfig().getNodename(), latestVersion);

        if (requestedFolderId != null && !requestedFolderId.toString().equals(directoryId)) {
            throw new PreconditionFailedException(
                    "If-Match version_uid does not match latest version.",
                    directoryId,
                    encodePath(getBaseEnvLinkURL()
                            + "/rest/openehr/v1/ehr/"
                            + ehrId.toString()
                            + "/directory/" + directoryId));
        }
    }
}
