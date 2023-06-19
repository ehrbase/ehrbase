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
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;

@JacksonXmlRootElement
public class CompositionResponseData extends ActionRestResponseData {

    private StructuredString composition;

    private CompositionFormat format;
    private String templateId;
    private UUID ehrId;
    private String compositionUid;

    public StructuredString getComposition() {
        return composition;
    }

    public void setComposition(StructuredString composition) {
        this.composition = composition;
    }

    public CompositionFormat getFormat() {
        return format;
    }

    public void setFormat(CompositionFormat format) {
        this.format = format;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public UUID getEhrId() {
        return ehrId;
    }

    public void setEhrId(UUID ehrId) {
        this.ehrId = ehrId;
    }

    public String getCompositionUid() {
        return compositionUid;
    }

    public void setCompositionUid(String compositionUid) {
        this.compositionUid = compositionUid;
    }
}
