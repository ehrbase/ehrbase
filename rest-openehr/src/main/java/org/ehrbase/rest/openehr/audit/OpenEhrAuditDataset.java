/*
 * Copyright (c) 2021 Vitasystems GmbH.
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

package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;

import java.io.Serializable;

public abstract class OpenEhrAuditDataset implements Serializable {

    private EventOutcomeIndicator eventOutcomeIndicator;

    private String eventOutcomeDescription;

    private String sourceUserId;

    private String sourceAddress;

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

    public String getSourceUserId() {
        return sourceUserId;
    }

    public void setSourceUserId(String sourceUserId) {
        this.sourceUserId = sourceUserId;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }
}
