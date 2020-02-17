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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;

import java.util.List;

/**
 * Response data for full representational cases, otherwise only metadata will be used.
 */
@JacksonXmlRootElement(localName = "contribution")
public class ContributionResponseData {

    @JsonProperty(value = "uid")
    private HierObjectId uid;

    @JsonProperty(value = "versions")
    @JacksonXmlElementWrapper(localName = "versions")
    @JacksonXmlProperty(localName = "version")
    private List<ObjectRef<ObjectVersionId>> versions;

    @JsonProperty(value = "audit")
    private AuditDetails audit;

    public ContributionResponseData(HierObjectId uid, List<ObjectRef<ObjectVersionId>> versions, AuditDetails audit) {
        this.uid = uid;
        this.versions = versions;
        this.audit = audit;
    }

    public HierObjectId getUid() {
        return uid;
    }

    public void setUid(HierObjectId uid) {
        this.uid = uid;
    }

    public List<ObjectRef<ObjectVersionId>> getVersions() {
        return versions;
    }

    public void setVersions(List<ObjectRef<ObjectVersionId>> versions) {
        this.versions = versions;
    }

    public AuditDetails getAudit() {
        return audit;
    }

    public void setAudit(AuditDetails audit) {
        this.audit = audit;
    }
}
