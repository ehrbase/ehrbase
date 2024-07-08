/*
 * Copyright (c) 2019 vitasystems GmbH.
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
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.support.identification.HierObjectId;
import java.util.List;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;

/**
 * Basic set of response data regarding <code>EHR_STATUS</code>. operations. Used as default or when <code>PREFER</code>
 * header requests <code>minimal</code> response.
 */
@JsonRootName(value = "EHR")
@JacksonXmlRootElement(localName = "ehr")
public record EhrDto(
        @JsonProperty(value = "system_id") HierObjectId systemId,
        @JsonProperty(value = "ehr_id") HierObjectId ehrId,
        @JsonProperty(value = "ehr_status") EhrStatusDto ehrStatus,
        @JsonProperty(value = "time_created") DvDateTime timeCreated,
        @JsonProperty(value = "compositions") List<CompositionDto> compositions,
        @JsonProperty(value = "contributions") List<ContributionDto> contributions) {}
