/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.containment;

import java.io.Serializable;
import java.util.Objects;

public class TemplateIdAqlTuple implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String templateId;
    private final String aql;
    private final Short sysTenant;

    public TemplateIdAqlTuple(String templateId, String aql, Short sysTenant) {
        this.templateId = templateId;
        this.aql = aql;
        this.sysTenant = sysTenant;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getAql() {
        return aql;
    }

    public Short getTenantIdentifier() {
        return sysTenant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateIdAqlTuple that = (TemplateIdAqlTuple) o;
        return templateId.equals(that.templateId) && sysTenant.equals(that.sysTenant) && aql.equals(that.aql);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId, aql, sysTenant);
    }
}
