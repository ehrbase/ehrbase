/*
 * Copyright (c) 2020 Jake Smolka (Hannover Medical School).
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
import com.nedap.archie.rm.changecontrol.Contribution;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.generic.Attestation;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import org.ehrbase.api.dto.ContributionDto;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "original_version")
public class OriginalVersionResponseData<T> {

    @JsonProperty(value = "_type")
    private String type;
    @JsonProperty(value = "uid")
    private ObjectVersionId versionId;
    // contribution, signature and commit_audit could be extracted to "VERSION" super class
    private Contribution contribution;
    private String signature;   // optional
    @JsonProperty(value = "commit_audit")
    private AuditDetails auditDetails;
    @JsonProperty(value = "preceding_version_uid")
    private ObjectVersionId precedingVersionUid;   // optional
    @JsonProperty(value = "other_input_version_uids")
    private List<ObjectVersionId> otherInputVersionUids; // optional
    @JsonProperty(value = "lifecycle_state")
    private DvCodedText lifecycleState;
    private List<Attestation> attestations;     // optional
    private T data;

    public OriginalVersionResponseData(OriginalVersion<T> originalVersion, ContributionDto contributionDto) {
        setType("ORIGINAL_VERSION");
        setVersionId(originalVersion.getUid());

        HierObjectId contributionId = new HierObjectId(contributionDto.getUuid().toString());
        List<ObjectRef> versions = new ArrayList<>();
        contributionDto.getObjectReferences().forEach((k, v) -> versions.add(
                new ObjectRef<>(new HierObjectId(v), "local", k)));
        Contribution contribution = new Contribution(contributionId, versions, contributionDto.getAuditDetails());
        setContribution(contribution);

        setSignature(originalVersion.getSignature());
        setAuditDetails(originalVersion.getCommitAudit());
        setPrecedingVersionUid(originalVersion.getPrecedingVersionUid());
        setOtherInputVersionUids(originalVersion.getOtherInputVersionUids());
        setLifecycleState(originalVersion.getLifecycleState());
        setAttestations(originalVersion.getAttestations());
        setData(originalVersion.getData());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ObjectVersionId getVersionId() {
        return versionId;
    }

    public void setVersionId(ObjectVersionId versionId) {
        this.versionId = versionId;
    }

    public Contribution getContribution() {
        return contribution;
    }

    public void setContribution(Contribution contribution) {
        this.contribution = contribution;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public AuditDetails getAuditDetails() {
        return auditDetails;
    }

    public void setAuditDetails(AuditDetails auditDetails) {
        this.auditDetails = auditDetails;
    }

    public ObjectVersionId getPrecedingVersionUid() {
        return precedingVersionUid;
    }

    public void setPrecedingVersionUid(ObjectVersionId precedingVersionUid) {
        this.precedingVersionUid = precedingVersionUid;
    }

    public List<ObjectVersionId> getOtherInputVersionUids() {
        return otherInputVersionUids;
    }

    public void setOtherInputVersionUids(List<ObjectVersionId> otherInputVersionUids) {
        this.otherInputVersionUids = otherInputVersionUids;
    }

    public DvCodedText getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(DvCodedText lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public List<Attestation> getAttestations() {
        return attestations;
    }

    public void setAttestations(List<Attestation> attestations) {
        this.attestations = attestations;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
