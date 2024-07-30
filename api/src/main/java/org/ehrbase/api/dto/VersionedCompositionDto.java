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
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;

/**
 * Response data of a <code>VERSIONED_COMPOSITION</code>.
 */
@JsonRootName(value = "VERSIONED_COMPOSITION")
@JacksonXmlRootElement(localName = "versioned_composition")
public record VersionedCompositionDto(
        @JsonProperty(value = "uid") HierObjectId uid,
        @JsonProperty(value = "owner_id") ObjectRef<? extends ObjectId> ownerId,
        @JsonProperty(value = "time_created") String timeCreated) {

    @JsonProperty(value = "_type")
    public String type() {
        return "VERSIONED_COMPOSITION";
    }
}
