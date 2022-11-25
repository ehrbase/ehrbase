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
package org.ehrbase.service;

import static org.ehrbase.dao.access.util.FolderUtils.checkSiblingNameConflicts;
import static org.ehrbase.dao.access.util.FolderUtils.doesAnyIdInFolderStructureMatch;
import static org.ehrbase.dao.access.util.FolderUtils.uuidMatchesObjectVersionId;

import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.FolderService;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_EhrAccess;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.jooq.FolderAccess;
import org.ehrbase.dao.access.jooq.FolderHistoryAccess;
import org.ehrbase.dao.access.util.FolderUtils;
import org.ehrbase.response.ehrscape.FolderDto;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.response.ehrscape.StructuredStringFormat;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.serialisation.xmlencoding.CanonicalXML;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FolderServiceImp extends BaseServiceImp implements FolderService {

    private final EhrService ehrService;
    private final TenantService tenantService;

    @Autowired
    FolderServiceImp(
            KnowledgeCacheService knowledgeCacheService,
            DSLContext context,
            ServerConfig serverConfig,
            EhrService ehrService,
            TenantService tenantService) {
        super(knowledgeCacheService, context, serverConfig);
        this.ehrService = ehrService;
        this.tenantService = tenantService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> create(UUID ehrId, Folder objData, UUID systemId, UUID committerId, String description) {
        return internalCreate(
                ehrId, objData, systemId, committerId, description, null, tenantService.getCurrentTenantIdentifier());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> create(UUID ehrId, Folder objData, UUID contribution) {
        return internalCreate(
                ehrId, objData, null, null, null, contribution, tenantService.getCurrentTenantIdentifier());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> create(UUID ehrId, Folder objData) {
        return create(
                ehrId, objData, getSystemUuid(), getCurrentUserId(tenantService.getCurrentTenantIdentifier()), null);
    }

    private Optional<FolderDto> internalCreate(
            UUID ehrId,
            Folder objData,
            UUID systemId,
            UUID committerId,
            String description,
            UUID contribution,
            String tenantIdentifier) {
        /*Note:
        The checks should be performed here, even if parts are checked in some controllers as well, to make sure they are run
        in every necessary case */

        // Check for existence of EHR record
        ehrService.checkEhrExistsAndIsModifiable(ehrId);

        // Check for duplicate directories
        if (ehrService.getDirectoryId(ehrId) != null) {
            throw new StateConflictException(
                    String.format("EHR with id %s already contains a directory.", ehrId.toString()));
        }

        // Check of there are name conflicts on each folder level
        checkSiblingNameConflicts(objData);

        // Save current time which will be used as transaction time
        Timestamp currentTimeStamp = Timestamp.from(Instant.now());

        // Contribution handling - create new one or retrieve existing, if ID is given
        I_ContributionAccess contributionAccess;
        if (contribution == null) {
            contributionAccess = I_ContributionAccess.getInstance(getDataAccess(), ehrId, tenantIdentifier);
        } else {
            contributionAccess = I_ContributionAccess.retrieveInstance(getDataAccess(), contribution);
            // Copy values from contribution to folder's audit
            systemId = contributionAccess.getAuditsSystemId();
            committerId = contributionAccess.getAuditsCommitter();
            description = contributionAccess.getAuditsDescription();
        }

        if (systemId == null || committerId == null) {
            throw new InternalServerException("Error on contribution handling for folder creation.");
        }

        // Get first FolderAccess instance
        I_FolderAccess folderAccess = FolderAccess.buildNewFolderAccessHierarchy(
                getDataAccess(), objData, currentTimeStamp, ehrId, contributionAccess, tenantIdentifier);
        ObjectVersionId folderId;
        if (contribution == null) {
            folderId = new ObjectVersionId(
                    folderAccess
                                    .commit(LocalDateTime.now(), systemId, committerId, description)
                                    .toString() + "::" + getServerConfig().getNodename() + "::1");
        } else {
            folderId = new ObjectVersionId(
                    (folderAccess.commit(LocalDateTime.now(), contribution).toString() + "::"
                            + getServerConfig().getNodename() + "::1"));
        }
        // Save root directory id to ehr entry
        // EHR must exist at this point, so no null check
        // TODO: Refactor to use UID
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);
        ehrAccess.setDirectory(FolderUtils.extractUuidFromObjectVersionId(folderId));
        ehrAccess.update(
                getCurrentUserId(tenantService.getCurrentTenantIdentifier()),
                getSystemUuid(),
                null,
                null,
                ContributionChangeType.MODIFICATION,
                EhrServiceImp.DESCRIPTION);

        return get(folderId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> update(
            UUID ehrId,
            ObjectVersionId targetObjId,
            Folder objData,
            UUID systemId,
            UUID committerId,
            String description) {
        return internalUpdate(
                ehrId,
                targetObjId,
                objData,
                systemId,
                committerId,
                description,
                null,
                tenantService.getCurrentTenantIdentifier());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> update(UUID ehrId, ObjectVersionId targetObjId, Folder objData, UUID contribution) {
        return internalUpdate(
                ehrId,
                targetObjId,
                objData,
                null,
                null,
                null,
                contribution,
                tenantService.getCurrentTenantIdentifier());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> update(UUID ehrId, ObjectVersionId targetObjId, Folder objData) {
        return update(
                ehrId,
                targetObjId,
                objData,
                getSystemUuid(),
                getCurrentUserId(tenantService.getCurrentTenantIdentifier()),
                null);
    }

    private Optional<FolderDto> internalUpdate(
            UUID ehrId,
            ObjectVersionId targetObjId,
            Folder objData,
            UUID systemId,
            UUID committerId,
            String description,
            UUID contribution,
            String tenantIdentifier) {
        var timestamp = LocalDateTime.now();

        /*Note:
        The checks should be performed here, even if parts are checked in some controllers as well, to make sure they are run
        in every necessary case */

        // Check for existence of EHR record and make sure the folder is actually part of the EHR
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        checkFolderWithIdExistsInEhr(ehrId, targetObjId);

        // Check of there are name conflicts on each folder level
        checkSiblingNameConflicts(objData);

        // Get existing root folder
        I_FolderAccess folderAccess = I_FolderAccess.getInstanceForExistingFolder(getDataAccess(), targetObjId);

        // Set update data on root folder
        FolderUtils.updateFolder(objData, folderAccess);

        // Delete sub folders and all their sub folder, as well as their linked entities
        if (contribution == null) {
            folderAccess
                    .getSubfoldersList()
                    .forEach((sf, sa) -> internalDelete(
                            ehrId,
                            new ObjectVersionId(sf.toString()),
                            systemId,
                            committerId,
                            description,
                            null,
                            false));
        } else {
            folderAccess
                    .getSubfoldersList()
                    .forEach((sf, sa) -> internalDelete(
                            ehrId, new ObjectVersionId(sf.toString()), null, null, null, contribution, false));
        }
        // Clear sub folder list
        folderAccess.getSubfoldersList().clear();

        // Create FolderAccess instances for sub folders if there are any
        if (objData.getFolders() != null && !objData.getFolders().isEmpty()) {

            // Create new sub folders list
            objData.getFolders().forEach(childFolder -> folderAccess
                    .getSubfoldersList()
                    .put(
                            UuidGenerator.randomUUID(),
                            FolderAccess.buildNewFolderAccessHierarchy(
                                    getDataAccess(),
                                    childFolder,
                                    Timestamp.from(Instant.now()),
                                    ehrId,
                                    ((FolderAccess) folderAccess).getContributionAccess(),
                                    tenantIdentifier)));
        }

        // Send update to access layer which updates the hierarchy recursive
        boolean success;
        if (contribution == null) {
            success = folderAccess.update(
                    timestamp, systemId, committerId, description, ContributionChangeType.MODIFICATION);
        } else {
            success = folderAccess.update(timestamp, contribution);
        }

        if (success) {
            return createDto(folderAccess, getLastVersionNumber(targetObjId), true);
        } else {
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId, UUID systemId, UUID committerId, String description) {
        internalDelete(ehrId, targetObjId, systemId, committerId, description, null, true);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId, UUID contribution) {
        internalDelete(ehrId, targetObjId, null, null, null, contribution, true);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(UUID ehrId, ObjectVersionId targetObjId) {
        delete(ehrId, targetObjId, getSystemUuid(), getCurrentUserId(tenantService.getCurrentTenantIdentifier()), null);
    }

    private void internalDelete(
            UUID ehrId,
            ObjectVersionId folderId,
            UUID systemId,
            UUID committerId,
            String description,
            UUID contribution,
            boolean withEhrCheck) {
        /*Note:
        The checks should be performed here, even if parts are checked in some controllers as well, to make sure they are run
        in every necessary case */
        if (withEhrCheck) { // provide the option to skip the EHR checks for subfolder deletes while updating

            ehrService.checkEhrExistsAndIsModifiable(ehrId);
            checkFolderWithIdExistsInEhr(ehrId, folderId);
        }

        // first remove the folders reference from EHR if necessary
        if (uuidMatchesObjectVersionId(ehrService.getDirectoryId(ehrId), folderId)) {
            ehrService.removeDirectory(ehrId);
        }

        // delete the folder
        I_FolderAccess folderAccess = I_FolderAccess.getInstanceForExistingFolder(getDataAccess(), folderId);

        var timestamp = LocalDateTime.now();

        int result;
        if (contribution == null) {
            result = folderAccess.delete(timestamp, systemId, committerId, description);
        } else {
            result = folderAccess.delete(timestamp, contribution);
        }

        if (result <= 0) {
            // Not found and bad argument exceptions are handled before thus this case can only occur on unknown errors
            // On the server side
            throw new InternalServerException("Error during deletion of folder " + folderId);
        }
    }

    private void checkFolderWithIdExistsInEhr(UUID ehrId, ObjectVersionId folderId) {
        UUID ehrRootDirectoryId = ehrService.getDirectoryId(ehrId);
        if (ehrRootDirectoryId == null) {
            throw new PreconditionFailedException(String.format(
                    "EHR with id %s does not contain a directory. Maybe it has been deleted?", ehrId.toString()));
        }

        I_FolderAccess folderAccess = I_FolderAccess.getInstanceForExistingFolder(
                getDataAccess(), new ObjectVersionId(ehrRootDirectoryId.toString()));

        if (!doesAnyIdInFolderStructureMatch(folderAccess, folderId)) {
            throw new PreconditionFailedException(
                    String.format("Folder with id %s is not part of EHR with id %s", folderId.getValue(), ehrId));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> get(ObjectVersionId folderId, String path) {

        I_FolderAccess folderAccess;
        Integer version = FolderUtils.extractVersionNumberFromObjectVersionId(folderId);
        if (version == null) {
            // Get the latest object
            folderAccess = I_FolderAccess.getInstanceForExistingFolder(getDataAccess(), folderId);
            version = getLastVersionNumber(folderId);
        } else {
            // Get timestamp for version
            Timestamp versionTimestamp = FolderAccess.getTimestampForVersion(getDataAccess(), folderId, version);
            folderAccess = I_FolderAccess.getInstanceForExistingFolder(getDataAccess(), folderId, versionTimestamp);
        }

        I_FolderAccess withExtractedPath = extractPath(folderAccess, path);

        return createDto(withExtractedPath, version, path == null || path.equals("/"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> getByTimeStamp(ObjectVersionId folderId, Timestamp timestamp, String path) {

        // Get the latest entry for folder
        I_FolderAccess latest = I_FolderAccess.getInstanceForExistingFolder(getDataAccess(), folderId);
        I_FolderAccess withExtractedPath;

        if (latest == null) {
            throw new ObjectNotFoundException(
                    "FOLDER", String.format("Folder with id %s could not be found", folderId.toString()));
        }

        Integer version = FolderUtils.extractVersionNumberFromObjectVersionId(folderId);

        // Check if timestamp is newer or equal than found folder
        if (timestamp.after(latest.getFolderSysTransaction()) || timestamp.equals(latest.getFolderSysTransaction())) {
            if (version == null) {
                version = getLastVersionNumber(folderId);
            }
            withExtractedPath = extractPath(latest, path);
        } else {
            // Get the target timestamp version and data
            version = getVersionNumberForTimestamp(folderId, timestamp);
            I_FolderAccess versionAtTime =
                    FolderHistoryAccess.getInstanceForExistingFolder(getDataAccess(), folderId, timestamp);
            withExtractedPath = extractPath(versionAtTime, path);
        }

        return createDto(withExtractedPath, version, path == null || path.equals("/"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> getLatest(ObjectVersionId folderId, String path) {

        I_FolderAccess folderAccess =
                I_FolderAccess.getInstanceForExistingFolder(getDataAccess(), folderId, Timestamp.from(Instant.now()));
        Integer version = FolderUtils.extractVersionNumberFromObjectVersionId(folderId);

        if (version == null) {
            version = getLastVersionNumber(folderId);
        }

        return createDto(folderAccess, version, path == null || path.equals("/"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructuredString serialize(Folder folder, StructuredStringFormat format) {

        // TODO: Create structure of sub folders recursively including items

        StructuredString folderString;
        switch (format) {
            case XML:
                folderString =
                        new StructuredString(new CanonicalXML().marshal(folder, false), StructuredStringFormat.XML);
                break;
            case JSON:
                folderString = new StructuredString(new CanonicalJson().marshal(folder), StructuredStringFormat.JSON);
                break;
            default:
                throw new UnexpectedSwitchCaseException(
                        "Unsupported target format for serialization of folders: " + format);
        }

        return folderString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getLastVersionNumber(ObjectVersionId folderId) {

        return FolderAccess.getLastVersionNumber(getDataAccess(), folderId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getVersionNumberForTimestamp(ObjectVersionId folderId, Timestamp timestamp) {

        return FolderAccess.getVersionNumberAtTime(getDataAccess(), folderId, timestamp);
    }

    /**
     * Generates a {@link FolderDto} for response if a folder could be found. In other cases it
     * returns an empty {@link Optional}.
     *
     * @param folderAccess - The {@link I_FolderAccess} containing the data
     * @return {@link Optional<FolderDto>}
     */
    private Optional<FolderDto> createDto(I_FolderAccess folderAccess, int version, boolean isRoot) {

        if (folderAccess == null) {
            return Optional.empty();
        }

        Folder folder = createFolderObject(folderAccess);
        // Set the root uid to a valid version_uid
        if (isRoot) {
            folder.setUid(new ObjectVersionId(String.format(
                    "%s::%s::%s",
                    folderAccess.getFolderId().toString(),
                    this.getServerConfig().getNodename(),
                    version)));
        }

        return Optional.of(new FolderDto(folder));
    }

    /**
     * Traverses recursively through the sub folders of a given FolderAccess and Returns a Folder RM
     * Object for internal usage with all sub folders and items which belong to the folder
     * structure.
     *
     * @param folderAccess - Folder dao containing the target folder record
     * @return Folder object
     */
    private Folder createFolderObject(I_FolderAccess folderAccess) {

        Folder result = new Folder(
                new HierObjectId(String.format(
                        "%s::%s",
                        folderAccess.getFolderId().toString(),
                        this.getServerConfig().getNodename())),
                folderAccess.getFolderArchetypeNodeId(),
                new DvText(folderAccess.getFolderName()),
                folderAccess.getFolderDetails(),
                null,
                null,
                null,
                null,
                null,
                folderAccess.getItems(),
                null);

        // Handle sub folder list recursively
        if (!folderAccess.getSubfoldersList().isEmpty()) {

            result.setFolders(folderAccess.getSubfoldersList().values().stream()
                    .map(this::createFolderObject)
                    .collect(Collectors.toList()));
        }

        return result;
    }

    /**
     * If a path was sent by the client the folderAccess retrieved from database will be iterated recursive to find a
     * given sub folder. If the path is empty or contains only one forward slash the root folder will be returned.
     * Trailing slashes at the end of a path will be ignored. If the path cannot be found an ObjectNotFound exception
     * will be thrown which can be handled by the controller layer.
     *
     * @param folderAccess - Retrieved result folder hierarchy from database
     * @param path         - Path to identify desired sub folder
     * @return folderAccess containing the sub folder and its sub tree if path can be found
     */
    private I_FolderAccess extractPath(I_FolderAccess folderAccess, String path) {
        // Handle path if sent by client
        if (path != null && !"/".equals(path)) {
            // Trim starting forward slash
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            folderAccess = FolderUtils.getPath(folderAccess, 0, path.split("/"));
        }

        return folderAccess;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminDeleteFolder(UUID folderId) {
        I_FolderAccess folderAccess = I_FolderAccess.retrieveInstanceForExistingFolder(getDataAccess(), folderId);
        folderAccess.adminDeleteFolder();
    }
}
