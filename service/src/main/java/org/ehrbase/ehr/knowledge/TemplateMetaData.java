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

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class TemplateMetaData {
    private OPERATIONALTEMPLATE operationaltemplate;
    private ZonedDateTime createdOn;
    private ZonedDateTime lastAccessTime;
    private ZonedDateTime lastModifiedTime;
    private List<String> errorList;
    private Path path;

    public OPERATIONALTEMPLATE getOperationaltemplate() {
        return operationaltemplate;
    }

    public void setOperationaltemplate(OPERATIONALTEMPLATE operationaltemplate) {
        this.operationaltemplate = operationaltemplate;
    }

    public ZonedDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(ZonedDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public ZonedDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(ZonedDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public ZonedDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(ZonedDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
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

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
