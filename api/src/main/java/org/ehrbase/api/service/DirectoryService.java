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
package org.ehrbase.api.service;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

public interface DirectoryService extends BaseService {

    /**
     * Get the Folder for Ehr with id equal <code>ehrId</code>
     * @param ehrId
     * @param folderId if <code>null</code> latest version will be returned
     * @param path optional return folder at <code>path</code>
     * @return
     */
    Optional<Folder> get(UUID ehrId, @Nullable ObjectVersionId folderId, @Nullable String path);

    /**
     * Get the Folder for Ehr with id equal <code>ehrId</code> for a specific point in time;
     * @param ehrId
     * @param time
     * @param path optional return folder at <code>path</code>
     * @return
     */
    Optional<Folder> getByTime(UUID ehrId, OffsetDateTime time, @Nullable String path);

    /**
     *  Create a new folder for Ehr with id equal <code>ehrId</code>
     * @param ehrId
     * @param folder
     * @return
     */
    Folder create(UUID ehrId, Folder folder);

    /**
     * Update the folder for Ehr with id equal <code>ehrId</code>
     * @param ehrId
     * @param folder
     * @param ifMatches expected version before update for optimistic looking
     * @return
     */
    Folder update(UUID ehrId, Folder folder, ObjectVersionId ifMatches);

    /**
     * delete the folder for Ehr with id equal <code>ehrId</code>
     * @param ehrId
     * @param ifMatches expected version before delete for optimistic looking
     */
    void delete(UUID ehrId, ObjectVersionId ifMatches);

    /**
     * Physical delete a folder with all History
     * @param ehrId
     * @param folderId
     */
    void adminDeleteFolder(UUID ehrId, UUID folderId);
}
