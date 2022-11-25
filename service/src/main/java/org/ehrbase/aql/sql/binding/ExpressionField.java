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

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryimpl.*;

@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class ExpressionField {

    private final I_VariableDefinition variableDefinition;
    private final JsonbEntryQuery jsonbEntryQuery;
    private final CompositionAttributeQuery compositionAttributeQuery;

    public ExpressionField(
            I_VariableDefinition variableDefinition,
            JsonbEntryQuery jsonbEntryQuery,
            CompositionAttributeQuery compositionAttributeQuery) {
        this.variableDefinition = variableDefinition;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.compositionAttributeQuery = compositionAttributeQuery;
    }

    MultiFields toSql(String className, String templateId, String identifier, IQueryImpl.Clause clause)
            throws UnknownVariableException {

        MultiFields aqlFields;

        if (new FieldConstantHandler(variableDefinition).isConstant()) {
            return new MultiFields(
                    variableDefinition, new FieldConstantHandler(variableDefinition).field(), templateId);
        }

        switch (className) {
                // COMPOSITION attributes
            case "COMPOSITION":
                CompositionAttribute compositionAttribute =
                        new CompositionAttribute(compositionAttributeQuery, jsonbEntryQuery, clause);
                aqlFields = compositionAttribute.toSql(variableDefinition, templateId, identifier);
                break;
                // EHR attributes
            case "EHR":
                aqlFields = compositionAttributeQuery.makeField(templateId, identifier, variableDefinition, clause);
                break;
                // other, f.e. CLUSTER, ADMIN_ENTRY, OBSERVATION etc.
            default:
                // other_details f.e.
                if (compositionAttributeQuery.isCompositionAttributeItemStructure(
                        templateId, variableDefinition.getIdentifier())) {
                    ContextualAttribute contextualAttribute =
                            new ContextualAttribute(compositionAttributeQuery, jsonbEntryQuery, clause);
                    aqlFields = contextualAttribute.toSql(templateId, variableDefinition);
                } else {
                    // all other that are supported as simpleClassExpr (most common resolution)
                    LocatableItem locatableItem = new LocatableItem(compositionAttributeQuery, jsonbEntryQuery, clause);
                    aqlFields = locatableItem.toSql(templateId, variableDefinition);
                    if (!aqlFields.isEmpty()) aqlFields.setUseEntryTable(true);
                }
                break;
        }

        return aqlFields;
    }
}
