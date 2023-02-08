/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UID;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.DirectoryService;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.util.FolderUtils;
import org.ehrbase.jooq.pg.tables.records.EhrFolderRecord;
import org.ehrbase.repository.EhrFolderRepository;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Stefan Spiska
 */
@Service
public class DirectoryServiceImp extends BaseServiceImp implements DirectoryService {

    private final ServerConfig serverConfig;
    private final EhrServiceImp ehrServiceImp;

    private final EhrFolderRepository ehrFolderRepository;

    public DirectoryServiceImp(
            KnowledgeCacheService knowledgeCacheService,
            DSLContext context,
            ServerConfig serverConfig,
            EhrServiceImp ehrServiceImp,
            EhrFolderRepository ehrFolderRepository) {
        super(knowledgeCacheService, context, serverConfig);
        this.serverConfig = serverConfig;
        this.ehrServiceImp = ehrServiceImp;
        this.ehrFolderRepository = ehrFolderRepository;
    }

    @Override
    public Optional<Folder> get(UUID ehrId, @Nullable ObjectVersionId folderId, @Nullable String path) {

        List<EhrFolderRecord> ehrFolderRecords;
        if (folderId == null) {
            ehrFolderRecords = ehrFolderRepository.getLatest(ehrId);
        } else {

            ehrFolderRecords = ehrFolderRepository.fromHistory(ehrFolderRepository.getByVersion(
                    ehrId, Integer.parseInt(folderId.getVersionTreeId().getValue())));
        }

        if (!ehrFolderRecords.isEmpty()) {
            return findByPath(ehrFolderRepository.from(ehrFolderRecords), StringUtils.split(path, '/'));
        } else {

            return Optional.empty();
        }
    }

    @Override
    public Optional<Folder> getByTime(UUID ehrId, OffsetDateTime time, @Nullable String path) {

        List<EhrFolderRecord> ehrFolderRecords;

        ehrFolderRecords = ehrFolderRepository.fromHistory(ehrFolderRepository.getByTime(ehrId, time));

        if (!ehrFolderRecords.isEmpty()) {
            return findByPath(ehrFolderRepository.from(ehrFolderRecords), StringUtils.split(path, '/'));
        } else {

            return Optional.empty();
        }
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

        // validation
        ehrServiceImp.checkEhrExistsAndIsModifiable(ehrId);
        if (ehrFolderRepository.hasDirectory(ehrId)) {
            throw new StateConflictException(
                    String.format("EHR with id %s already contains a directory.", ehrId.toString()));
        }

        FolderUtils.checkSiblingNameConflicts(folder);

        updateUuid(
                folder,
                true,
                Optional.ofNullable(folder.getUid())
                        .map(UIDBasedId::getRoot)
                        .map(UID::getValue)
                        .map(UUID::fromString)
                        .orElse(UuidGenerator.randomUUID()),
                1);

        ehrFolderRepository.commit(ehrFolderRepository.toRecord(ehrId, folder), null);

        return folder;
    }

    @Override
    public Folder update(UUID ehrId, Folder folder, ObjectVersionId ifMatches) {
        // validation
        ehrServiceImp.checkEhrExistsAndIsModifiable(ehrId);
        if (!ehrFolderRepository.hasDirectory(ehrId)) {
            throw new StateConflictException(
                    String.format("EHR with id %s dos not contains a directory.", ehrId.toString()));
        }

        FolderUtils.checkSiblingNameConflicts(folder);

        int version = Integer.parseInt(ifMatches.getVersionTreeId().getValue());
        updateUuid(folder, true, UUID.fromString(ifMatches.getObjectId().getValue()), version + 1);
        ehrFolderRepository.update(ehrFolderRepository.toRecord(ehrId, folder));

        return folder;
    }

    @Override
    public void delete(UUID ehrId, ObjectVersionId ifMatches) {

        // validation
        ehrServiceImp.checkEhrExistsAndIsModifiable(ehrId);

        ehrFolderRepository.delete(
                ehrId,
                UUID.fromString(ifMatches.getObjectId().getValue()),
                Integer.valueOf(ifMatches.getVersionTreeId().getValue()));
    }

    private void updateUuid(Folder folder, boolean root, UUID rootUuid, int version) {

        if (folder.getUid() == null || root) {

            if (root) {
                folder.setUid(new ObjectVersionId(rootUuid + "::" + serverConfig.getNodename() + "::" + version));
            } else {
                folder.setUid(new HierObjectId(UuidGenerator.randomUUID().toString()));
            }
        }

        if (folder.getFolders() != null) {

            folder.getFolders().forEach(folder1 -> updateUuid(folder1, false, rootUuid, version));
        }
    }
}
