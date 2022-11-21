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

/**
 * evaluate the SQL expression for locatables in the ITEM_STRUCTURE: OBSERVATION, INSTRUCTION, CLUSTER etc.
 * NB. At the moment, direct resolution of ELEMENT is not supported.
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class LocatableItem {

    private final CompositionAttributeQuery compositionAttributeQuery;
    private final JsonbEntryQuery jsonbEntryQuery;
    private final IQueryImpl.Clause clause;

    public LocatableItem(
            CompositionAttributeQuery compositionAttributeQuery,
            JsonbEntryQuery jsonbEntryQuery,
            IQueryImpl.Clause clause) {
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.clause = clause;
    }

    public MultiFields toSql(String templateId, I_VariableDefinition variableDefinition)
            throws UnknownVariableException {
        MultiFields multiFields;

        multiFields =
                jsonbEntryQuery.makeField(templateId, variableDefinition.getIdentifier(), variableDefinition, clause);

        if (multiFields == null) {
            compositionAttributeQuery.setUseEntry(true);
            return MultiFields.asNull(variableDefinition, templateId, clause);
        }

        return multiFields;
    }

    public void setUseEntry() {
        compositionAttributeQuery.setUseEntry(true);
    }
}
