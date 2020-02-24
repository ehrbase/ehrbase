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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.support.identification.HierObjectId;
import org.ehrbase.api.dto.CompositionDto;
import org.ehrbase.api.dto.ContributionDto;

import java.util.List;

/**
 * Basic set of response data regarding EHR operations. Used as default or when `PREFER` header requests minimal response.
 */
@JacksonXmlRootElement(localName = "ehr")
public class EhrResponseData {


    @JsonProperty(value = "system_id")
    private HierObjectId systemId;
    @JsonProperty(value = "ehr_id")
    private HierObjectId ehrId;
    @JsonProperty(value = "ehr_status")
    private EhrStatus ehrStatus;
    @JsonProperty(value = "time_created")
    private String timeCreated;
    @JsonProperty
    private List<CompositionDto> compositions;
    @JsonProperty
    private List<ContributionDto> contributions;

    public HierObjectId getSystemId() {
        return systemId;
    }

    public void setSystemId(HierObjectId systemId) {
        this.systemId = systemId;
    }

    public HierObjectId getEhrId() {
        return ehrId;
    }

    public void setEhrId(HierObjectId ehrId) {
        this.ehrId = ehrId;
    }

    public EhrStatus getEhrStatus() {
        return ehrStatus;
    }

    public void setEhrStatus(EhrStatus ehrStatus) {
        this.ehrStatus = ehrStatus;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

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
