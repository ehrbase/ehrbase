/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.service;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.definitions.StructuredString;
import org.ehrbase.api.definitions.StructuredStringFormat;
import org.ehrbase.api.dto.FolderDto;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.service.FolderService;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.jooq.FolderAccess;
import org.ehrbase.dao.access.util.FolderUtils;
import org.ehrbase.serialisation.CanonicalJson;
import org.ehrbase.serialisation.CanonicalXML;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

@Service
@Transactional
public class FolderServiceImp extends BaseService implements FolderService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Formatter formatter = new Formatter();

    @Autowired
    FolderServiceImp(
            KnowledgeCacheService knowledgeCacheService,
            DSLContext context,
            ServerConfig serverConfig) {
        super(knowledgeCacheService, context, serverConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID create(UUID ehrId, Folder content) {

        // Save current time which will be used as transaction time
        DateTime currentTimeStamp = DateTime.now();

        // Create Contribution Access
        I_ContributionAccess contributionAccess = I_ContributionAccess.getInstance(
                getDataAccess(),
                ehrId);

        // Get first FolderAccess instance
        I_FolderAccess folderAccess = FolderAccess.buildNewFolderAccessHierarchy(getDataAccess(),
                                                                                 content,
                                                                                 currentTimeStamp,
                                                                                 ehrId,
                                                                                 contributionAccess);
        return folderAccess.commit(new Timestamp(currentTimeStamp.getMillis()));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> retrieve(UUID folderId, Integer version) {

        I_FolderAccess folderAccess;

        folderAccess = I_FolderAccess.retrieveInstanceForExistingFolder(getDataAccess(), folderId);

        return createDto(folderAccess);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> retrieveByTimestamp(
            UUID folderId, LocalDateTime timestamp) {

        try {
            // Get version active at the timestamp
            // TODO: Fetch entry by FolderAccess.retrieveByTimestamp
            return Optional.empty();
        } catch (ObjectNotFoundException e) {
            logger.debug(formatter.format(
                    "Folder entry not found for timestamp: %s",
                    timestamp.format(ISO_DATE_TIME))
                                  .toString());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FolderDto> update(
            UUID folderId, Folder update, UUID ehrId) {

        DateTime timestamp = DateTime.now();

        // Get existing root folder
        I_FolderAccess
                folderAccess
                = FolderAccess.retrieveInstanceForExistingFolder(getDataAccess(), folderId);

        // Set update data on root folder
        FolderUtils.updateFolder(update, folderAccess);

        // Clear sub folder list
        folderAccess.getSubfoldersList()
                    .clear();

        // Create FolderAccess instances for sub folders if there are any
        if (update.getFolders() != null &&
            !update.getFolders()
                   .isEmpty()) {

            // Create new sub folders list
            update.getFolders()
                  .forEach(childFolder -> folderAccess.getSubfoldersList()
                                                      .put(
                                                              UUID.randomUUID(),
                                                              FolderAccess.buildNewFolderAccessHierarchy(
                                                                      getDataAccess(),
                                                                      childFolder,
                                                                      timestamp,
                                                                      ehrId,
                                                                      ((FolderAccess) folderAccess).getContributionAccess())));
        }

        // Send update to access layer which updates the hierarchy recursive
        if (folderAccess.update(new Timestamp(timestamp.getMillis()))) {

            return createDto(folderAccess);
        } else {

            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime delete(UUID folderId) {

        // TODO implement logic
        return LocalDateTime.now();
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
                folderString = new StructuredString(
                        new CanonicalXML().marshal(folder, false),
                        StructuredStringFormat.XML);
                break;
            case JSON:
                folderString = new StructuredString(
                        new CanonicalJson().marshal(folder),
                        StructuredStringFormat.JSON);
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
    public Integer getLastVersionNumber(UUID folderId) {

        return FolderAccess.getLastVersionNumber(getDataAccess(), folderId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getVersionNumberForTimestamp(
            UUID folderId, LocalDateTime timestamp) {

        return 1;
    }

    /**
     * Generates a {@link FolderDto} for response if a folder could be found. In other cases it
     * returns an empty {@link Optional}.
     *
     * @param folderAccess - The {@link I_FolderAccess} containing the data
     * @return {@link Optional<FolderDto>}
     */
    private Optional<FolderDto> createDto(I_FolderAccess folderAccess) {

        if (folderAccess == null) {

            return Optional.empty();
        }

        Folder folder = createFolderObject(folderAccess);

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

        Folder result = new Folder();
        result.setDetails(folderAccess.getFolderDetails());
        result.setArchetypeNodeId(folderAccess.getFolderArchetypeNodeId());
        result.setNameAsString(folderAccess.getFolderName());
        result.setItems(folderAccess.getItems());
        result.setUid(new ObjectVersionId(folderAccess.getFolderId()
                                                      .toString()));

        // Handle subfolder list recursively
        if (!folderAccess.getSubfoldersList()
                         .isEmpty()) {

            result.setFolders(folderAccess.getSubfoldersList()
                                          .values()
                                          .stream()
                                          .map(this::createFolderObject)
                                          .collect(Collectors.toList()));

        } else {
            result.setFolders(null);
        }

        return result;
    }
}
