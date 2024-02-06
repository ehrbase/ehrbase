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
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;

@JacksonXmlRootElement
public class TemplateExampleResponseData extends ActionRestResponseData {

    private StructuredString composition;

    public StructuredString getComposition() {
        return composition;
    }

    public void setComposition(StructuredString composition) {
        this.composition = composition;
    }
}
