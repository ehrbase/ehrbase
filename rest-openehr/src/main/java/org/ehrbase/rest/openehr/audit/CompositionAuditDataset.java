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

import org.openehealth.ipf.commons.audit.codes.EventActionCode;

public class CompositionAuditDataset extends OpenEhrAuditDataset {

    private EventActionCode eventActionCode;

    private String patientNumber;

    private String compositionUri;

    private String compositionTemplateId;

    public EventActionCode getEventActionCode() {
        return eventActionCode;
    }

    public void setEventActionCode(EventActionCode eventActionCode) {
        this.eventActionCode = eventActionCode;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getCompositionUri() {
        return compositionUri;
    }

    public void setCompositionUri(String compositionUri) {
        this.compositionUri = compositionUri;
    }

    public String getCompositionTemplateId() {
        return compositionTemplateId;
    }

    public void setCompositionTemplateId(String compositionTemplateId) {
        this.compositionTemplateId = compositionTemplateId;
    }

    boolean hasPatientNumber() {
        return patientNumber != null;
    }

    boolean hasComposition() {
        return compositionUri != null;
    }
}
