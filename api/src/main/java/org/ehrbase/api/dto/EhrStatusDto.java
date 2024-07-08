/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.FeederAudit;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import javax.annotation.Nullable;

/**
 * Request/Response data of an <code>EHR_STATUS</code>.
 */
@JsonRootName(value = "EHR_STATUS")
@JacksonXmlRootElement(localName = "ehr_status")
public record EhrStatusDto(
        @JsonProperty(value = "uid") UIDBasedId uid,
        @JsonProperty(value = "archetype_node_id", required = true) String archetypeNodeId,
        @JsonProperty(value = "name", required = true) DvText name,
        @JsonProperty(value = "archetype_details") @Nullable Archetyped archetypeDetails,
        @JsonProperty(value = "feeder_audit") @Nullable FeederAudit feederAudit,
        @JsonProperty(value = "subject") PartySelf subject,
        @JsonProperty(value = "is_queryable", required = true) Boolean isQueryable,
        @JsonProperty(value = "is_modifiable", required = true) Boolean isModifiable,
        @JsonProperty(value = "other_details") @Nullable ItemStructure otherDetails) {

    @JsonProperty(value = "_type", required = true)
    public String type() {
        return "EHR_STATUS";
    }
}
