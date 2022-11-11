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
package org.ehrbase.dao.access.interfaces;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.ehrbase.dao.access.jooq.FolderAccess;
import org.ehrbase.dao.access.jooq.FolderHistoryAccess;
import org.ehrbase.dao.access.util.FolderUtils;
import org.joda.time.DateTime;

/**
 *@Created by Luis Marco-Ruiz on Jun 13, 2019
 */

/**
 * Data Access Object for CRUD operations on instances of {@link  com.nedap.archie.rm.directory.Folder}.
 */
public interface I_FolderAccess extends I_VersionedCRUD, I_Compensatable {

    public static I_FolderAccess retrieveByVersion(I_DomainAccess domainAccess, UUID folderId, int version) {
        return FolderAccess.retrieveByVersion(domainAccess, folderId, version);
    }

    static boolean isDeleted(I_DomainAccess domainAccess, UUID versionedObjectId) {
        return FolderAccess.isDeleted(domainAccess, versionedObjectId);
    }

    UUID getEhrId();

    /**
     * Get the list of subfolders for the {@link  com.nedap.archie.rm.directory.Folder} that corresponds to this {@link  I_FolderAccess}
     * @return Map<UUID, I_FolderAccess> whose key is the UUID of the child {@link  com.nedap.archie.rm.directory.Folder}, and whose value is the I_FolderAccess for the child {@link  com.nedap.archie.rm.directory.Folder}.
     * @throws Exception
     */
    Map<UUID, I_FolderAccess> getSubfoldersList();

    /**
     * Get the items references stored as a part of the given {@link  com.nedap.archie.rm.directory.Folder}
     * @return items of the {@link  com.nedap.archie.rm.directory.Folder} that corresponds to this {@link  I_FolderAccess}
     */
    List<ObjectRef<? extends ObjectId>> getItems();

    /**
     * Builds the {@link I_FolderAccess} for persisting the {@link  com.nedap.archie.rm.directory.Folder} provided as param.
     * @param domainAccess providing the information about the DB connection.
     * @param folder to define the {@link I_FolderAccess} that allows its DB access.
     * @param dateTime that will be set as transaction date when the {@link  com.nedap.archie.rm.directory.Folder} is persisted
     * @param ehrId of the {@link com.nedap.archie.rm.ehr.Ehr} that references the {@link  com.nedap.archie.rm.directory.Folder} provided as param.
     * @return {@link I_FolderAccess} with the information to persist the provided {@link  com.nedap.archie.rm.directory.Folder}
     */
    static I_FolderAccess getNewFolderAccessInstance(
            I_DomainAccess domainAccess, Folder folder, DateTime dateTime, UUID ehrId, String tenantIdentifier) {
        return FolderAccess.getNewFolderAccessInstance(domainAccess, folder, dateTime, ehrId, tenantIdentifier);
    }

    /**
     * Retrieve instance of {@link I_FolderAccess} with the information needed retrieve the folder and its sub-folders.
     * @param domainAccess providing the information about the DB connection.
     * @param folderId {@link java.util.UUID} of the {@link  com.nedap.archie.rm.directory.Folder} to be fetched from the DB.
     * @return the {@link I_FolderAccess} that provides DB access to the {@link  com.nedap.archie.rm.directory.Folder} that corresponds to the provided folderId param.
     * @throws Exception
     */
    static I_FolderAccess retrieveInstanceForExistingFolder(I_DomainAccess domainAccess, UUID folderId) {
        return FolderAccess.retrieveInstanceForExistingFolder(domainAccess, folderId);
    }

    static I_FolderAccess retrieveInstanceForExistingFolder(
            I_DomainAccess domainAccess, UUID folderId, Timestamp timestamp) {
        return FolderHistoryAccess.retrieveInstanceForExistingFolder(domainAccess, folderId, timestamp);
    }

    static I_FolderAccess getInstanceForExistingFolder(I_DomainAccess domainAccess, ObjectVersionId folderId) {
        return FolderAccess.retrieveInstanceForExistingFolder(
                domainAccess, FolderUtils.extractUuidFromObjectVersionId(folderId));
    }

    static I_FolderAccess getInstanceForExistingFolder(
            I_DomainAccess domainAccess, ObjectVersionId folderId, Timestamp timestamp) {
        return FolderAccess.retrieveInstanceForExistingFolder(
                domainAccess, FolderUtils.extractUuidFromObjectVersionId(folderId));
    }

    /**
     * Retrieves the version IDs of all folders, which are linked to the given contribution.
     * @param domainAccess DB access
     * @param contribution Given contribution to query for
     * @param nodeName Node name to build version ID with (access layer doesn't have access to this info)
     * @return Set of {@link ObjectVersionId} for linked folders
     */
    static Set<ObjectVersionId> retrieveFolderVersionIdsInContribution(
            I_DomainAccess domainAccess, UUID contribution, String nodeName) {
        return FolderAccess.retrieveFolderVersionIdsInContribution(domainAccess, contribution, nodeName);
    }

    UUID getFolderId();

    void setFolderId(UUID folderId);

    UUID getInContribution();

    void setInContribution(UUID inContribution);

    String getFolderName();

    void setFolderName(String folderName);

    String getFolderArchetypeNodeId();

    void setFolderNArchetypeNodeId(String folderArchetypeNodeId);

    boolean isFolderActive();

    void setIsFolderActive(boolean folderActive);

    ItemStructure getFolderDetails();

    void setFolderDetails(ItemStructure folderDetails);

    void setFolderSysTransaction(Timestamp folderSysTransaction);

    Timestamp getFolderSysTransaction();

    AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime> getFolderSysPeriod();

    void setFolderSysPeriod(AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime> folderSysPeriod);

    UUID getAudit();

    void setAudit(UUID auditId);

    /**
     * Invoke physical deletion.
     */
    void adminDeleteFolder();
}
