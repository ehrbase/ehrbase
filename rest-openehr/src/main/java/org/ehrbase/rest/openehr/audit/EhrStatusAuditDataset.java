/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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

/**
 * {@link OpenEhrAuditDataset} used for Ehr Status audit messages
 */
public class EhrStatusAuditDataset extends OpenEhrAuditDataset {

    private String ehrStatusUri;

    public String getEhrStatusUri() {
        return ehrStatusUri;
    }

    public void setEhrStatusUri(String ehrStatusUri) {
        this.ehrStatusUri = ehrStatusUri;
    }

    public boolean hasEhrStatusUri() {
        return ehrStatusUri != null;
    }
}
