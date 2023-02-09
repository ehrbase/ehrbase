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

import static org.apache.commons.lang3.StringUtils.unwrap;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.service.DirectoryService;
import org.ehrbase.api.service.EhrService;
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
@RequestMapping(path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/ehr")
public class OpenehrDirectoryController extends BaseController implements DirectoryApiSpecification {

    private final DirectoryService folderService;

    private final EhrService ehrService;

    public OpenehrDirectoryController(DirectoryService folderService, EhrService ehrService) {
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

        var createdFolder = folderService.create(ehrId, folder);

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

        folderId.setValue(unwrap(folderId.getValue(), '"'));

        // Update folder and get new version
        Folder updatedFolder = folderService.update(ehrId, folder, folderId);

        return createDirectoryResponse(HttpMethod.PUT, prefer, accept, updatedFolder, ehrId);
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

        folderId.setValue(unwrap(folderId.getValue(), '"'));

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
        Optional<Folder> foundFolder = folderService.get(ehrId, versionUid, path);
        if (foundFolder.isEmpty()) {
            throw new ObjectNotFoundException(
                    "DIRECTORY",
                    String.format(
                            "Folder with id %s and path %s does not exist.",
                            versionUid.toString(), path != null ? path : "/"));
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

        assertValidPath(path);

        final Optional<Folder> foundFolder;
        // Get the folder entry from database
        Optional<OffsetDateTime> temporal = getVersionAtTimeParam();

        if (temporal.isPresent()) {
            foundFolder = folderService.getByTime(ehrId, temporal.get(), path);
        } else {
            foundFolder = folderService.get(ehrId, null, path);
        }

        if (foundFolder.isEmpty()) {
            throw new ObjectNotFoundException(
                    "folder",
                    "The FOLDER for ehrId %s and path %s does not exist."
                            .formatted(ehrId.toString(), path != null ? path : "/"));
        }

        return createDirectoryResponse(HttpMethod.GET, RETURN_REPRESENTATION, accept, foundFolder.get(), ehrId);
    }

    private DirectoryResponseData buildResponse(Folder folderDto) {
        DirectoryResponseData resBody = new DirectoryResponseData();
        resBody.setDetails(folderDto.getDetails());
        resBody.setFolders(folderDto.getFolders());
        resBody.setItems(folderDto.getItems());
        resBody.setName(folderDto.getName());
        resBody.setUid(folderDto.getUid());
        return resBody;
    }

    private ResponseEntity<DirectoryResponseData> createDirectoryResponse(
            HttpMethod method, String prefer, String accept, Folder folderDto, UUID ehrId) {
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
            headers.setLocation(createLocationUri(EHR, ehrId.toString(), DIRECTORY, versionUid));
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
        int latestVersion = 1;
        // folderService.getLastVersionNumber(new ObjectVersionId(directoryUuid.toString()));
        // TODO: Change column 'directory' in EHR to String with ObjectVersionId
        String directoryId = String.format(
                "%s::%s::%d", directoryUuid, ehrService.getServerConfig().getNodename(), latestVersion);

        if (requestedFolderId != null && !requestedFolderId.toString().equals(directoryId)) {
            throw new PreconditionFailedException(
                    "If-Match version_uid does not match latest version.",
                    directoryId,
                    createLocationUri(EHR, ehrId.toString(), DIRECTORY, directoryId)
                            .toString());
        }
    }
}
