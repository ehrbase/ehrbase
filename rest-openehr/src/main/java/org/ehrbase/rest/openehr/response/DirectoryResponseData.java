/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

import org.ehrbase.api.definitions.StructuredString;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Directory response data class. Will be used each time a response format other
 * than minimal is preferred by client.
 */
@JacksonXmlRootElement
public class DirectoryResponseData {

    // TODO: Replace basic types with RM types. Currently causing startup problems due to complex nested structures with swagger-ui

    @JsonProperty(value = "uid")
    private String uid;
    @JsonProperty(value = "folder")
    private StructuredString folder;

    public String getUid() { return this.uid; }
    public void setUid(String uid) { this.uid = uid; }

    public StructuredString getFolder() { return this.folder; }
    public void setFolder(StructuredString folder ) { this.folder = folder; }
}
