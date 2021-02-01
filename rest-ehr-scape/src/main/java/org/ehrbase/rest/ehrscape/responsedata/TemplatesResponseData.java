/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.rest.ehrscape.responsedata;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;

import java.util.List;

@JacksonXmlRootElement
public class TemplatesResponseData extends ActionRestResponseData {

    List<TemplateMetaDataDto> templates;

    public List<TemplateMetaDataDto> getTemplates() {
        return templates;
    }

    public void setTemplates(List<TemplateMetaDataDto> templates) {
        this.templates = templates;
    }
}
