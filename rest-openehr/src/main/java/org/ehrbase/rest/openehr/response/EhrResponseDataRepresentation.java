/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

import org.ehrbase.api.dto.CompositionDto;
import org.ehrbase.api.dto.ContributionDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * Extends the basic or minimal representation of EhrResponseData with more specific data. Used when `PREFER` header requests the full representation.
 */
@JacksonXmlRootElement(localName = "ehr")
public class EhrResponseDataRepresentation extends EhrResponseData {

    @JsonProperty
    private List<CompositionDto> compositions;
    @JsonProperty
    private List<ContributionDto> contributions;

    public List<CompositionDto> getCompositions() {
        return compositions;
    }

    public void setCompositions(List<CompositionDto> compositions) {
        this.compositions = compositions;
    }

    public List<ContributionDto> getContributions() {
        return contributions;
    }

    public void setContributions(List<ContributionDto> contributions) {
        this.contributions = contributions;
    }
}
