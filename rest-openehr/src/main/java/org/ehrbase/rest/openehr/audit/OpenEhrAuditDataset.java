/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.rest.openehr.audit;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.ehrbase.api.exception.InternalServerException;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.springframework.http.HttpMethod;

/**
 * Base class for openEHR audit dataset
 */
public class OpenEhrAuditDataset implements Serializable {

    private EventOutcomeIndicator eventOutcomeIndicator;

    private String eventOutcomeDescription;

    private HttpMethod method;

    private String sourceParticipantUserId;

    private String sourceParticipantNetworkId;

    private Set<String> patientParticipantObjectIds = new HashSet<>();

    public EventOutcomeIndicator getEventOutcomeIndicator() {
        return eventOutcomeIndicator;
    }

    public void setEventOutcomeIndicator(EventOutcomeIndicator eventOutcomeIndicator) {
        this.eventOutcomeIndicator = eventOutcomeIndicator;
    }

    public String getEventOutcomeDescription() {
        return eventOutcomeDescription;
    }

    public void setEventOutcomeDescription(String eventOutcomeDescription) {
        this.eventOutcomeDescription = eventOutcomeDescription;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getSourceParticipantUserId() {
        return sourceParticipantUserId;
    }

    public void setSourceParticipantUserId(String sourceParticipantUserId) {
        this.sourceParticipantUserId = sourceParticipantUserId;
    }

    public String getSourceParticipantNetworkId() {
        return sourceParticipantNetworkId;
    }

    public void setSourceParticipantNetworkId(String sourceParticipantNetworkId) {
        this.sourceParticipantNetworkId = sourceParticipantNetworkId;
    }

    public Set<String> getPatientParticipantObjectIds() {
        return patientParticipantObjectIds;
    }

    public void setPatientParticipantObjectIds(Set<String> patientParticipantObjectIds) {
        this.patientParticipantObjectIds = patientParticipantObjectIds;
    }

    public void addPatientParticipantObjectIds(Collection<String> patientParticipantObjectIds) {
        this.patientParticipantObjectIds.addAll(patientParticipantObjectIds);
    }

    public boolean hasPatientParticipantObjectIds() {
        return patientParticipantObjectIds != null && !patientParticipantObjectIds.isEmpty();
    }

    public String getUniquePatientParticipantObjectId() {
        Set<String> ids = getPatientParticipantObjectIds();
        if (ids.isEmpty()) {
            return null;
        } else if (ids.size() == 1) {
            return ids.iterator().next();
        } else {
            throw new InternalServerException("Non unique patient number result");
        }
    }

    public boolean hasUniqueParticipantObjectIds() {
        return hasPatientParticipantObjectIds() && patientParticipantObjectIds.size() == 1;
    }
}
