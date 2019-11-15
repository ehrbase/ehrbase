/*
 * Copyright (c) 2019 Jake Smolka (Hannover Medical School).
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.UIDBasedId;

@JacksonXmlRootElement(localName = "ehr_status")
public class EhrStatusResponseData {

    @JsonProperty(value = "_type")
    private final String type = "EHR_STATUS";

    @JsonProperty(value = "archetype_node_id")
    private String archetypeNodeId;
    @JsonProperty
    private DvText name;
    @JsonProperty
    private UIDBasedId uid;
    @JsonProperty
    private PartySelf subject;
    @JsonProperty(value = "other_details")
    private ItemStructure otherDetails;
    private Boolean isModifiable;
    private Boolean isQueryable;

    public String getArchetypeNodeId() {
        return archetypeNodeId;
    }

    public void setArchetypeNodeId(String archetypeNodeId) {
        this.archetypeNodeId = archetypeNodeId;
    }

    public DvText getName() {
        return name;
    }

    public void setName(DvText name) {
        this.name = name;
    }

    public UIDBasedId getUid() {
        return uid;
    }

    public void setUid(UIDBasedId uid) {
        this.uid = uid;
    }

    public PartySelf getSubject() {
        return subject;
    }

    public void setSubject(PartySelf subject) {
        this.subject = subject;
    }

    public ItemStructure getOtherDetails() {
        return otherDetails;
    }

    public void setOtherDetails(ItemStructure otherDetails) {
        this.otherDetails = otherDetails;
    }

    @JsonProperty(value = "is_modifiable")
    public Boolean getModifiable() {
        return isModifiable;
    }

    public void setModifiable(Boolean modifiable) {
        isModifiable = modifiable;
    }

    @JsonProperty(value = "is_queryable")
    public Boolean getQueryable() {
        return isQueryable;
    }

    public void setQueryable(Boolean queryable) {
        isQueryable = queryable;
    }

    public String getType() {
        return type;
    }
}
