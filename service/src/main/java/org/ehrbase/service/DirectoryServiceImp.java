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
package org.ehrbase.service;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.VersionTreeId;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.api.util.LocatableUtils;
import org.ehrbase.repository.EhrFolderRepository;
import org.ehrbase.util.UuidGenerator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DirectoryServiceImp implements InternalDirectoryService {

    private final SystemService systemService;
    private final EhrService ehrService;
    private final EhrFolderRepository ehrFolderRepository;
    private final ValidationService validationService;

    public DirectoryServiceImp(
            SystemService systemService,
            EhrService ehrService,
            EhrFolderRepository ehrFolderRepository,
            final ValidationService validationService) {

        this.systemService = systemService;
        this.ehrService = ehrService;
        this.ehrFolderRepository = ehrFolderRepository;
        this.validationService = validationService;
    }

    @Override
    public Optional<Folder> get(UUID ehrId, ObjectVersionId folderId, String path) {

        final Optional<Folder> root;
        if (folderId == null) {
            // directory
            root = ehrFolderRepository.findHead(ehrId, 1);

        } else {
            // check creating system matches
            if (!folderId.getCreatingSystemId().getValue().equals(systemService.getSystemId())) {
                return Optional.empty();
            }
            int version;
            try {
                version = LocatableUtils.getUidVersion(folderId);
            } catch (NumberFormatException e) {
                VersionTreeId versionTreeId = folderId.getVersionTreeId();
                if (versionTreeId.isBranch()) {
                    throw new UnsupportedOperationException(
                            "Version branching is not supported: %s".formatted(versionTreeId.getValue()));
                } else {
                    throw e;
                }
            }

            // directory
            root = ehrFolderRepository.findByVersion(ehrId, 1, version);
        }

        if (root.isEmpty()) {
            // Check if EHR for the folder exists
            ehrService.checkEhrExists(ehrId);
            return Optional.empty();
        }
        return root
                // check if id matches
                .filter(f ->
                        folderId == null || folderId.getRoot().equals(f.getUid().getRoot()))
                .flatMap(f -> findByPath(f, StringUtils.split(path, '/')));
    }

    @Override
    public Optional<Folder> getByTime(UUID ehrId, OffsetDateTime time, String path) {

        Optional<ObjectVersionId> versionByTime = ehrFolderRepository.findVersionByTime(ehrId, 1, time);

        return versionByTime
                .flatMap(v -> ehrFolderRepository.findByVersion(ehrId, 1, LocatableUtils.getUidVersion(v)))
                .flatMap(f -> findByPath(f, StringUtils.split(path, '/')));
    }

    private Optional<Folder> findByPath(Folder root, String[] path) {

        if (ArrayUtils.isEmpty(path)) {
            return Optional.of(root);
        }
        if (root.getFolders() == null) {
            return Optional.empty();
        }

        return root.getFolders().stream()
                .filter(sf -> sf.getNameAsString().equals(path[0]))
                .findAny()
                .flatMap(sf -> findByPath(sf, ArrayUtils.subarray(path, 1, path.length)));
    }

    @Override
    public Folder create(UUID ehrId, Folder folder) {

        return create(ehrId, folder, null, null);
    }

    @Override
    public Folder create(UUID ehrId, Folder folder, UUID contributionId, UUID auditId) {

        // validation
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        if (ehrFolderRepository.hasFolderAtIndex(ehrId, EHR_DIRECTORY_FOLDER_IDX)) {
            throw new StateConflictException("EHR with id %s already contains a directory.".formatted(ehrId));
        }

        UUID folderUid = Optional.of(folder).map(LocatableUtils::getUuid).orElse(UuidGenerator.randomUUID());
        updateUuid(folder, true, folderUid, 1);
        validationService.check(folder);

        try {
            ehrFolderRepository.commit(ehrId, folder, contributionId, auditId, EHR_DIRECTORY_FOLDER_IDX);
        } catch (DuplicateKeyException _) {
            throw new StateConflictException("FOLDER with uid %s already exist.".formatted(folderUid));
        }

        return get(ehrId, null, null).orElseThrow();
    }

    @Override
    public Folder update(UUID ehrId, Folder folder, ObjectVersionId ifMatches) {

        return update(ehrId, folder, ifMatches, null, null);
    }

    @Override
    public Folder update(UUID ehrId, Folder folder, ObjectVersionId ifMatches, UUID contributionId, UUID auditId) {

        UUID uuid = UUID.fromString(ifMatches.getObjectId().getValue());

        // Ensure UIDs matching
        Optional.of(folder)
                .map(LocatableUtils::getUuid)
                .filter(u -> !u.equals(uuid))
                .ifPresent(u -> {
                    throw new PreconditionFailedException(String.format("FOLDER uid %s does not match %s", u, uuid));
                });

        // validation
        ehrService.checkEhrExistsAndIsModifiable(ehrId);

        if (!ehrFolderRepository.hasFolderInEhrForVoId(ehrId, uuid, EHR_DIRECTORY_FOLDER_IDX)) {
            // perform a second check to provide a more detailed error message
            if (!ehrFolderRepository.hasFolderAtIndex(ehrId, EHR_DIRECTORY_FOLDER_IDX)) {
                throw new PreconditionFailedException(
                        String.format("EHR with id %s does not contain a directory.", ehrId.toString()));
            }
            throw new PreconditionFailedException(
                    String.format("EHR with id %s does not contain a directory with id %s", ehrId, uuid));
        }

        int version = LocatableUtils.getUidVersion(ifMatches);

        updateUuid(folder, true, uuid, version + 1);
        validationService.check(folder);
        ehrFolderRepository.update(ehrId, folder, contributionId, auditId, EHR_DIRECTORY_FOLDER_IDX);

        return get(ehrId, null, null).orElseThrow();
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId ifMatches) {

        delete(ehrId, ifMatches, null, null);
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId ifMatches, UUID contributionId, UUID auditId) {

        // validation
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        if (!ehrFolderRepository.hasFolderAtIndex(ehrId, EHR_DIRECTORY_FOLDER_IDX)) {
            throw new PreconditionFailedException("EHR with id %s does not contain a directory.".formatted(ehrId));
        }

        ehrFolderRepository.delete(
                ehrId,
                UUID.fromString(ifMatches.getObjectId().getValue()),
                LocatableUtils.getUidVersion(ifMatches),
                1,
                contributionId,
                auditId);
    }

    private void updateUuid(Folder folder, boolean root, UUID rootUuid, int version) {

        if (folder.getUid() == null || root) {

            if (root) {
                folder.setUid(new ObjectVersionId(rootUuid + "::" + systemService.getSystemId() + "::" + version));
            } else {
                folder.setUid(new HierObjectId(UuidGenerator.randomUUID().toString()));
            }
        }

        if (folder.getFolders() != null) {

            folder.getFolders().forEach(folder1 -> updateUuid(folder1, false, rootUuid, version));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminDeleteFolder(UUID ehrId, UUID folderId) {

        // Check if EHR exists
        if (!this.ehrService.hasEhr(ehrId)) {
            throw new ObjectNotFoundException("Admin Directory", "EHR with id %s does not exist".formatted(ehrId));
        }

        // For now only EHR.directory is supported
        Optional<Folder> latest = ehrFolderRepository.findHead(ehrId, 1);

        if (latest.isPresent()) {
            Folder from = latest.get();

            if (!LocatableUtils.getUuid(from).equals(folderId)) {
                throw new IllegalArgumentException("FolderIds do not match");
            }

            ehrFolderRepository.adminDelete(ehrId, 1);
        }
    }
}
