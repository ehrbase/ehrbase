/*
 * Copyright (c) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryimpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery;
import org.jooq.Field;

@SuppressWarnings({"java:S3776","java:S3740","java:S1452"})
class ExpressionField {

    private final I_VariableDefinition variableDefinition;
    private final JsonbEntryQuery jsonbEntryQuery;
    private final CompositionAttributeQuery compositionAttributeQuery;
    private boolean containsJsonDataBlock = false;

    private String rootJsonKey = null;
    private String optionalPath = null;
    private String jsonbItemPath = null;

    public ExpressionField(I_VariableDefinition variableDefinition, JsonbEntryQuery jsonbEntryQuery, CompositionAttributeQuery compositionAttributeQuery) {
        this.variableDefinition = variableDefinition;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.compositionAttributeQuery = compositionAttributeQuery;
    }

    Field<?> toSql(String className, String templateId, String identifier) {

        Field<?> field;

        switch (className) {
            //COMPOSITION attributes
            case "COMPOSITION":
                CompositionAttribute compositionAttribute = new CompositionAttribute(compositionAttributeQuery, jsonbEntryQuery, IQueryImpl.Clause.SELECT);
                field = compositionAttribute.toSql(variableDefinition, templateId, identifier);
                jsonbItemPath = compositionAttribute.getJsonbItemPath();
                containsJsonDataBlock = compositionAttribute.isContainsJsonDataBlock();
                optionalPath = compositionAttribute.getOptionalPath();
                break;
            // EHR attributes
            case "EHR":

                field = compositionAttributeQuery.makeField(templateId, identifier, variableDefinition, IQueryImpl.Clause.SELECT);
                containsJsonDataBlock = compositionAttributeQuery.isJsonDataBlock();
                optionalPath = variableDefinition.getPath();
                break;
            // other, f.e. CLUSTER, ADMIN_ENTRY, OBSERVATION etc.
            default:
                // other_details f.e.
                if (compositionAttributeQuery.isCompositionAttributeItemStructure(templateId, variableDefinition.getIdentifier())) {
                    ContextualAttribute contextualAttribute = new ContextualAttribute(compositionAttributeQuery, jsonbEntryQuery, IQueryImpl.Clause.SELECT);
                    field = contextualAttribute.toSql(templateId, variableDefinition);
                    jsonbItemPath = contextualAttribute.getJsonbItemPath();
                    containsJsonDataBlock = contextualAttribute.isContainsJsonDataBlock();
                    optionalPath = contextualAttribute.getOptionalPath();
                }
                else {
                    // all other that are supported as simpleClassExpr (most common resolution)
                    LocatableItem locatableItem = new LocatableItem(compositionAttributeQuery, jsonbEntryQuery);
                    field = locatableItem.toSql(templateId, variableDefinition, className);
                    jsonbItemPath = locatableItem.getJsonbItemPath();
                    containsJsonDataBlock |= locatableItem.isContainsJsonDataBlock();
                    optionalPath = locatableItem.getOptionalPath();
                    rootJsonKey = locatableItem.getRootJsonKey();
                    jsonbItemPath = locatableItem.getJsonbItemPath();
                    locatableItem.setUseEntry();
                }
                break;
        }

        //this takes care of formatting the json result as only the "value" part (e.g. not "value":{"value":...})
        if (jsonbItemPath != null && (jsonbItemPath.endsWith("/origin")||jsonbItemPath.endsWith("/time"))){
            rootJsonKey = "value";
        }

        return field;
    }

    boolean isContainsJsonDataBlock() {
        return containsJsonDataBlock;
    }

    String getRootJsonKey() {
        return rootJsonKey;
    }

    String getOptionalPath() {
        return optionalPath;
    }

    String getJsonbItemPath() {
        return jsonbItemPath;
    }
}
