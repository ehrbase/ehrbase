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
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.DirectoryService;
import org.ehrbase.dao.access.util.FolderUtils;
import org.ehrbase.jooq.pg.tables.records.EhrFolderRecord;
import org.ehrbase.repository.EhrFolderRepository;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.Result;
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
    public Optional<Folder> get(UUID ehrId, ObjectVersionId folderId, String path) {
        Result<EhrFolderRecord> ehrFolderRecords = ehrFolderRepository.getLatest(ehrId);

        if (ehrFolderRecords.isNotEmpty()) {
            return Optional.of(ehrFolderRepository.from(ehrFolderRecords));
        } else {

            return Optional.empty();
        }
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

        updateUuid(folder, true, 1);

        ehrFolderRepository.commit(ehrFolderRepository.to(ehrId, folder));

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
        updateUuid(folder, true, version + 1);
        ehrFolderRepository.update(ehrFolderRepository.to(ehrId, folder));

        return folder;
    }

    private void updateUuid(Folder folder, boolean root, int version) {

        if (folder.getUid() == null) {

            if (root) {
                folder.setUid(new ObjectVersionId(
                        UuidGenerator.randomUUID() + "::" + serverConfig.getNodename() + "::" + version));
            } else {
                folder.setUid(HierObjectId.createRandomUUID());
            }
        }

        if (folder.getFolders() != null) {

            folder.getFolders().forEach(folder1 -> updateUuid(folder1, false, version));
        }
    }
}
