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

package org.ehrbase.ehr.knowledge;

import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class TemplateMetaData {
    private OPERATIONALTEMPLATE operationaltemplate;
    private OffsetDateTime createdOn;

    private List<String> errorList;


    public OPERATIONALTEMPLATE getOperationaltemplate() {
        return operationaltemplate;
    }

    public void setOperationaltemplate(OPERATIONALTEMPLATE operationaltemplate) {
        this.operationaltemplate = operationaltemplate;
    }

    public OffsetDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(OffsetDateTime createdOn) {
        this.createdOn = createdOn;
    }


    public List<String> getErrorList() {
        if (this.errorList == null) {
            this.errorList = new ArrayList<>();
        }
        return errorList;
    }

    public void addError(String error) {
        if (this.errorList == null) {
            this.errorList = new ArrayList<>();
        }
        this.errorList.add(error);
    }


}
