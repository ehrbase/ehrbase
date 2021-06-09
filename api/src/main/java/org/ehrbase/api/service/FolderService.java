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

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.response.ehrscape.FolderDto;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.response.ehrscape.StructuredStringFormat;

import java.sql.Timestamp;
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
    ObjectVersionId create(UUID ehrId, Folder content);

    /**
     * Creates a new folder entry at the database from content. The provided
     * content from request payload will be serialized before corresponding to
     * the given source format. The folder will be linked to the EHR addressed
     * by the request.
     *
     * @param ehrId - ID for the corresponding EHR
     * @param content - {@link com.nedap.archie.rm.directory.Folder} to persist
     * @param contribution Optional (can be null) custom contribution to use, instead of a generic new one
     * @return UUID of the new created Folder from database
     */
    ObjectVersionId create(UUID ehrId, Folder content, UUID contribution);

    /**
     * Retrieves a folder from database identified by object_version_uid and
     * extracts the given sub path of existing. If the object_version_uid does
     * not contain a version number the latest entry will be returned. A path
     * with common unix like root notation '/' will be treated as if there is
     * no path specified and the full tree will be returned.
     *
     * @param folderId - object_version_uid for target folder
     * @param path - Optional path to sub folder to extract
     * @return FolderDTO for further usage in upper layers
     */
    Optional<FolderDto> get(ObjectVersionId folderId, String path);

    /**
     * Fetches the latest entry from database. This will be
     * @param folderId - object_version_uid for target folder
     * @param path - Optional path to sub folder to extract
     * @return FolderDTO for further usage in other layers
     */
    Optional<FolderDto> getLatest(ObjectVersionId folderId, String path);

    /**
     * Fetches an folder entry from database identified by the root folder uid
     * and the given timestamp. If the current version has ben modified after
     * the given timestamp the folder will be searched inside the folder history
     * table.
     *
     * @param folderId - object_version_uid for target folder
     * @param timestamp - Timestamp of folder version to find
     * @param path - Optional path to sub folder to extract
     * @return FolderDTO for further usage in other layers
     */
    Optional<FolderDto> getByTimeStamp(ObjectVersionId folderId, Timestamp timestamp, String path);

    /**
     * Updates a target folder entry identified by the given folderId with new
     * content. The content string will be serialized from the given source
     * format.
     *
     * @param folderId - Full version_uid for folder including system id and version
     * @param update - Update content from request body
     * @param ehrId - EHR id for contribution creation
     * @param committerId - ID of committer for audit
     * @return Updated folder entry
     */
    Optional<FolderDto> update(ObjectVersionId folderId, Folder update, UUID ehrId, UUID committerId);

    /**
     * Updates a target folder entry identified by the given folderId with new
     * content. The content string will be serialized from the given source
     * format.
     *
     * @param folderId - Full version_uid for folder including system id and version
     * @param update - Update content from request body
     * @param ehrId - EHR id for contribution creation
     * @param contribution - Optional (can be set null) custom contribution to use for this update
     * @param committerId - ID of committer for audit
     * @param description  - Optional description test for audit
     * @return Updated folder entry
     */
    Optional<FolderDto> update(ObjectVersionId folderId, Folder update, UUID ehrId, UUID contribution, UUID committerId,
        String description);

    /**
     * Marks a given folder as deleted and moves it into the history table. The
     * folder will no longer be accessible without time or version information
     * available.
     *
     * @param folderId - Id of the target folder
     * @param contribution - Optional (can be set null) custom contribution to use for this update
     * @param committerId - ID of committer for audit
     * @param description  - Optional description test for audit
     * @return Timestamp of successful delete operation
     */
    LocalDateTime delete(ObjectVersionId folderId, UUID contribution, UUID committerId, String description);

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
    Integer getLastVersionNumber(ObjectVersionId folderId);


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
    Integer getVersionNumberForTimestamp(ObjectVersionId folderId, Timestamp timestamp);

    /**
     * Admin method to delete a Folder from the DB. See EHRbase Admin API specification for details.
     * @param folderId Folder to delete
     */
    void adminDeleteFolder(UUID folderId);
}
