/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.binding;

import java.util.Iterator;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryimpl.*;

/**
 * convert a field that is not identied as an EHR or a COMPOSITION (content or attribute). For example a CLUSTER
 * in other_context
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class ContextualAttribute {

    private final CompositionAttributeQuery compositionAttributeQuery;
    private final JsonbEntryQuery jsonbEntryQuery;
    private final IQueryImpl.Clause clause;

    public ContextualAttribute(
            CompositionAttributeQuery compositionAttributeQuery,
            JsonbEntryQuery jsonbEntryQuery,
            IQueryImpl.Clause clause) {
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.clause = clause;
    }

    public MultiFields toSql(String templateId, I_VariableDefinition variableDefinition)
            throws UnknownVariableException {
        String inTemplatePath =
                compositionAttributeQuery.variableTemplatePath(templateId, variableDefinition.getIdentifier());
        if (inTemplatePath.startsWith("/"))
            inTemplatePath = inTemplatePath.substring(
                    1); // conventionally, composition attribute path have the leading '/' striped.
        String originalPath = variableDefinition.getPath();
        variableDefinition.setPath(
                inTemplatePath + (variableDefinition.getPath() == null ? "" : "/" + variableDefinition.getPath()));
        CompositionAttribute compositionAttribute =
                new CompositionAttribute(compositionAttributeQuery, jsonbEntryQuery, clause);
        MultiFields fields =
                compositionAttribute.toSql(variableDefinition, templateId, variableDefinition.getIdentifier());

        if (clause.equals(IQueryImpl.Clause.SELECT)) {
            for (Iterator<QualifiedAqlField> qualifiedAqlFieldIterator = fields.iterator();
                    qualifiedAqlFieldIterator.hasNext(); ) {
                QualifiedAqlField field = qualifiedAqlFieldIterator.next();
                variableDefinition.setPath(originalPath);
                if (originalPath != null) {
                    if (variableDefinition.getAlias() != null)
                        field.setField(field.getSQLField().as(variableDefinition.getAlias()));
                    else field.setField(field.getSQLField().as("/" + originalPath));
                } else field.setField(field.getSQLField().as(variableDefinition.getIdentifier()));
            }
        }
        return fields;
    }
}
