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

package org.ehrbase.api.service;

import org.ehrbase.api.definitions.StructuredString;
import org.ehrbase.api.definitions.StructuredStringFormat;
import org.ehrbase.api.dto.FolderDto;
import org.ehrbase.api.exception.ObjectNotFoundException;
import com.nedap.archie.rm.directory.Folder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface FolderService extends BaseService {

    /**
     * Creates a new folder entry at the database from content. The provided
     * content from request payload will be serialized before corresponding to
     * the given source format. The folder will be linked to the EHR addressed
     * by the request.
     *
     * @param ehrId - ID for the corresponding EHR
     * @param content - {@link com.nedap.archie.rm.directory.Folder} to persist
     * @return UUID of the new created Folder from database
     */
    UUID create(UUID ehrId, Folder content);

    /**
     * Returns a versioned folder object by target version number. If the
     * version number is missing the latest version will be fetched from
     * folder table. If the version is older than the last one from folder
     * table the target folder will be fetched from folder_history.
     *
     * @param folderId - UUID of the folder to fetch
     * @param version - Target version to fetch
     * @return Data transfer object to return to client
     */
    Optional<FolderDto> retrieve(UUID folderId, Integer version);

    /**
     * Returns a versioned folder object which has been or is current at the
     * given timestamp. Therefore the folder table must be queried if there is
     * an actual folder with that uuid that has been created before the given
     * timestamp. Otherwise the folder_history table will be queried to find
     * a folder created after that timestamp.
     *
     * @param folderId - UUID of the target folder
     * @param timestamp - Given timestamp to look for an actual folder
     * @return - Created folder object
     */
    Optional<FolderDto> retrieveByTimestamp(
            UUID folderId,
            LocalDateTime timestamp
    );

    /**
     * Updates a target folder entry identified by the given folderId with new
     * content. The content string will be serialized from the given source
     * format.
     * TODO: Copy from CompositionService. Must be designed for folder
     *
     * @param folderId - Id of the target folder
     * @param update - Update content from request body
     * @param ehrId - EHR id for contribution creation
     * @return Updated folder entry
     */
    Optional<FolderDto> update(UUID folderId, Folder update, UUID ehrId);

    /**
     * Marks a given folder as deleted and moves it into the history table. The
     * folder will no longer be accessible without time or version information
     * available.
     *
     * @param folderId - Id of the target folder
     * @return Timestamp of successful delete operation
     */
    LocalDateTime delete(UUID folderId);

    /**
     * Serializes folder content from request body into a structured string
     * that can be used by database save mechanism to parse in into jsonb
     * format that will be saved at the database.
     *
     * @param folder - Folder to serialize
     * @param format - Source format of the folder
     * @return Structured string that can be parsed into jsonb
     */
    StructuredString serialize(Folder folder, StructuredStringFormat format);

    /**
     * Searches the last version number of a given folder entry from database.
     * This will be executed against the folder table which contains the latest
     * entry of a folder. The version will be extracted from there.
     *
     * @param folderId - Id of the folder to search for the latest version
     * @return Version number of the latest folder entry
     * @throws ObjectNotFoundException - Folder entry does not exist
     */
    Integer getLastVersionNumber(UUID folderId);

    /**
     * Searches for the folder version that was the current version at the
     * given timestamp. If the entry from folder table has a later timestamp
     * the folder history will be queried to find the version at the timestamp
     *
     * @param folderId - Id of the folder to search the version
     * @param timestamp - Timestamp to look for the version number
     * @return - Version number that was actual at the timestamp
     * @throws ObjectNotFoundException - Folder entry does not exist at the time
     */
    Integer getVersionNumberForTimestamp(UUID folderId, LocalDateTime timestamp);
}
