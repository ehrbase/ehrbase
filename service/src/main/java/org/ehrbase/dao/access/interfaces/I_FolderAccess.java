/*
 * Copyright (c) 2019 Vitasystems GmbH,  Hannover Medical School, and Luis Marco-Ruiz (Hannover Medical School).
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

package org.ehrbase.dao.access.interfaces;

import org.ehrbase.dao.access.jooq.FolderAccess;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectRef;
import org.joda.time.DateTime;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/***
 *@Created by Luis Marco-Ruiz on Jun 13, 2019
 */

/**
 * Data Access Object for CRUD operations on instances of {@link  com.nedap.archie.rm.directory.Folder}.
 */
public interface I_FolderAccess extends I_SimpleCRUD<I_EntryAccess, UUID> {

    /**
     * Get the list of subfolders for the {@link  com.nedap.archie.rm.directory.Folder} that corresponds to this {@link  I_FolderAccess}
     * @return Map<UUID, I_FolderAccess> whose key is the UUID of the child {@link  com.nedap.archie.rm.directory.Folder}, and whose value is the I_FolderAccess for the child {@link  com.nedap.archie.rm.directory.Folder}.
     * @throws Exception
     */
    Map<UUID, I_FolderAccess>  getSubfoldersList();

    /**
     * Set the details stored as a part of the given {@link  com.nedap.archie.rm.directory.Folder}
     * @param details
     */
    void setDetails(ItemStructure details);

    /**
     * Get the details  stored as a part of the given {@link  com.nedap.archie.rm.directory.Folder}
     * @return details of the {@link  com.nedap.archie.rm.directory.Folder} that corresponds to this {@link  I_FolderAccess}
     */
    ItemStructure getDetails();

    /**
     * Get the items references stored as a part of the given {@link  com.nedap.archie.rm.directory.Folder}
     * @return items of the {@link  com.nedap.archie.rm.directory.Folder} that corresponds to this {@link  I_FolderAccess}
     */
    List<ObjectRef> getItems();

    /**
     * Builds the {@link I_FolderAccess} for persisting the {@link  com.nedap.archie.rm.directory.Folder} provided as param.
     * @param domainAccess providing the information about the DB connection.
     * @param folder to define the {@link I_FolderAccess} that allows its DB access.
     * @param dateTime that will be set as transaction date when the {@link  com.nedap.archie.rm.directory.Folder} is persisted
     * @param ehrId of the {@link com.nedap.archie.rm.ehr.Ehr} that references the {@link  com.nedap.archie.rm.directory.Folder} provided as param.
     * @return {@link I_FolderAccess} with the information to persist the provided {@link  com.nedap.archie.rm.directory.Folder}
     */
    static I_FolderAccess getNewFolderAccessInstance(I_DomainAccess domainAccess, Folder folder, DateTime dateTime, UUID ehrId){
        return FolderAccess.getNewFolderAccessInstance(domainAccess, folder, dateTime, ehrId);
    }

    /**
     * Retrieve instance of {@link I_FolderAccess} with the information needed retrieve the folder and its sub-folders.
     * @param domainAccess providing the information about the DB connection.
     * @param folderId {@link java.util.UUID} of the {@link  com.nedap.archie.rm.directory.Folder} to be fetched from the DB.
     * @return the {@link I_FolderAccess} that provides DB access to the {@link  com.nedap.archie.rm.directory.Folder} that corresponds to the provided folderId param.
     * @throws Exception
     */
    static I_FolderAccess retrieveInstanceForExistingFolder(I_DomainAccess domainAccess, UUID folderId){
        return FolderAccess.retrieveInstanceForExistingFolder(domainAccess, folderId);
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

    Object getFolderSysPeriod();

    void setFolderSysPeriod(Object folderSysPeriod);
}
