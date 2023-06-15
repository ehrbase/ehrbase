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
package org.ehrbase.rest.ehrscape.responsedata;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.UUID;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.EhrStatusDto;

@JacksonXmlRootElement
public class EhrResponseData extends ActionRestResponseData {
    UUID ehrId;
    EhrStatusDto ehrStatus;

    public UUID getEhrId() {
        return ehrId;
    }

    public void setEhrId(UUID ehrId) {
        this.ehrId = ehrId;
    }

    public EhrStatusDto getEhrStatus() {
        return ehrStatus;
    }

    public void setEhrStatus(EhrStatusDto ehrStatus) {
        this.ehrStatus = ehrStatus;
    }
}
