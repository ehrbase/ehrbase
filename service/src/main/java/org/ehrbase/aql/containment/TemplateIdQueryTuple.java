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
import java.util.Collection;
import java.util.Objects;
import org.ehrbase.webtemplate.parser.NodeId;

public class TemplateIdQueryTuple implements Serializable {

    private final String templateId;
    private final Collection<NodeId> jsonQueryExpression;

    public TemplateIdQueryTuple(String templateId, Collection<NodeId> jsonQueryExpression) {

        this.templateId = templateId;
        this.jsonQueryExpression = jsonQueryExpression;
    }

    public String getTemplateId() {
        return templateId;
    }

    public Collection<NodeId> getJsonQueryExpression() {
        return jsonQueryExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateIdQueryTuple that = (TemplateIdQueryTuple) o;
        return templateId.equals(that.templateId) && jsonQueryExpression.equals(that.jsonQueryExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId, jsonQueryExpression);
    }
}
