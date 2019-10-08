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

package org.ehrbase.api.dto;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectRef;

import java.util.List;

public class FolderDto {

    private final Folder folder;
    private final List<Folder> folders;
    private final List<ObjectRef> items;
    private final String name;
    private final String uuid;


    public FolderDto(
            Folder folder,
            List<Folder> folders,
            List<ObjectRef> items,
            String name,
            String uuid
    ) {
        this.folder = folder;
        this.folders = folders;
        this.items = items;
        this.name = name;
        this.uuid = uuid;
    }

    public Folder getFolder() {
        return folder;
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public List<ObjectRef> getItems() { return items; }

    public String getName() { return name; }

    public String getUuid() {
        return uuid;
    }
}
