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
import org.ehrbase.aql.sql.queryimpl.*;

/**
 * convert a select or where AQL field into its SQL equivalent for a composition attribute.
 * This applies to standard attributes f.e. c/name/value etc.
 */
@SuppressWarnings({"java:S1452","java:S3740"})
public class CompositionAttribute {

    private final CompositionAttributeQuery compositionAttributeQuery;
    private final JsonbEntryQuery jsonbEntryQuery;
    private final IQueryImpl.Clause clause;
    private boolean containsJsonDataBlock;
    private String jsonbItemPath;
    private String optionalPath;

    public CompositionAttribute(CompositionAttributeQuery compositionAttributeQuery, JsonbEntryQuery jsonbEntryQuery, IQueryImpl.Clause clause) {
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.clause = clause;
    }

    public MultiFields toSql(I_VariableDefinition variableDefinition, String templateId, String identifier){
        MultiFields qualifiedAqlFields;

        if (variableDefinition.getPath() != null && variableDefinition.getPath().startsWith("content")) {
            qualifiedAqlFields = jsonbEntryQuery.makeField(templateId, identifier, variableDefinition, clause);
            qualifiedAqlFields.setUseEntryTable(true);
//            compositionAttributeQuery.setUseEntry(true);
        } else {
            qualifiedAqlFields = compositionAttributeQuery.makeField(templateId, identifier, variableDefinition, clause);
//            containsJsonDataBlock = compositionAttributeQuery.isJsonDataBlock();
        }
        optionalPath = variableDefinition.getPath();
        return qualifiedAqlFields;
    }

//    public boolean isContainsJsonDataBlock() {
//        return containsJsonDataBlock;
//    }

//    public String getJsonbItemPath() {
//        return jsonbItemPath;
//    }

//    public String getOptionalPath() {
//        return optionalPath;
//    }
}
