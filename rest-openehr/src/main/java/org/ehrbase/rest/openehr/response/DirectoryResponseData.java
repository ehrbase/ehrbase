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

package org.ehrbase.rest.openehr.response;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * Directory response data class. Will be used each time a response format other
 * than minimal is preferred by client.
 */
@JacksonXmlRootElement
public class DirectoryResponseData {

    @JsonProperty(value = "uid")
    private UIDBasedId uid;
    @JsonProperty(value = "folders")
    private List<Folder> folders;
    @JsonProperty(value = "items")
    private List<ObjectRef> items;
    @JsonProperty(value = "details")
    private ItemStructure details;
    @JsonProperty( value = "name")
    private DvText name;

    public UIDBasedId getUid() {
        return uid;
    }

    public void setUid(UIDBasedId uid) {
        this.uid = uid;
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public void setFolders(List<Folder> folder) {
        this.folders = folder;
    }

    public List<ObjectRef> getItems() {
        return items;
    }

    public void setItems(List<ObjectRef> items) {
        this.items = items;
    }

    public ItemStructure getDetails() {
        return details;
    }

    public void setDetails(ItemStructure details) {
        this.details = details;
    }

    public DvText getName() {
        return name;
    }

    public void setName(DvText name) {
        this.name = name;
    }
}
