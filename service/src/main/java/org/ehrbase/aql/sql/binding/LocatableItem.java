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

import com.nedap.archie.rm.datavalues.DataValue;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryimpl.*;
import org.jooq.impl.DSL;

import java.util.Objects;

/**
 * evaluate the SQL expression for locatables in the ITEM_STRUCTURE: OBSERVATION, INSTRUCTION, CLUSTER etc.
 * NB. At the moment, direct resolution of ELEMENT is not supported.
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class LocatableItem {

    private final CompositionAttributeQuery compositionAttributeQuery;
    private final JsonbEntryQuery jsonbEntryQuery;
    private IQueryImpl.Clause clause;

    public LocatableItem(CompositionAttributeQuery compositionAttributeQuery, JsonbEntryQuery jsonbEntryQuery, IQueryImpl.Clause clause) {
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.clause = clause;
    }

    public MultiFields toSql(String templateId, I_VariableDefinition variableDefinition, String className) {
        MultiFields multiFields;

        multiFields = jsonbEntryQuery.makeField(templateId, variableDefinition.getIdentifier(), variableDefinition, clause);

        //iterate on the found fields
        for (int i = 0; i < multiFields.fieldsSize(); i++) {
            if (!multiFields.isEmpty()) {
                QualifiedAqlField aqlField = multiFields.getQualifiedFieldOrLast(i);

                if (aqlField.isJsonDataBlock())
                    multiFields = decoratedJsonDataBlock(multiFields, aqlField, className, variableDefinition, templateId);
            }
        }
        return multiFields;
    }

    public void setUseEntry() {
        compositionAttributeQuery.setUseEntry(true);
    }

    private MultiFields decoratedJsonDataBlock(MultiFields multiFieldsInitial, QualifiedAqlField aqlField, String className, I_VariableDefinition variableDefinition, String templateId){

        MultiFields multiFields = multiFieldsInitial;

        if (aqlField.getItemType() != null) {
            Class itemClass = ArchieRMInfoLookup.getInstance().getClass(aqlField.getItemType());

            if (itemClass == null && className != null) //this may occur f.e. for itemType 'MULTIPLE'. try we classname
                itemClass = ArchieRMInfoLookup.getInstance().getClass(className);

            if (DataValue.class.isAssignableFrom(itemClass)) {
                VariableAqlPath variableAqlPath = new VariableAqlPath(variableDefinition.getPath());
                if (variableAqlPath.getSuffix().equals("value")) {
                    if (Objects.equals(className, "COMPOSITION")) { //assumes this is a data value within an ELEMENT
                        I_VariableDefinition variableDefinition1 = variableDefinition.duplicate();
                        variableDefinition1.setPath(variableAqlPath.getInfix());
                        multiFields = jsonbEntryQuery.makeField(templateId, variableDefinition.getIdentifier(), variableDefinition1, clause);
                        multiFields.setRootJsonKey(variableAqlPath.getSuffix());
                    } else if (aqlField.getItemCategory().equals("ELEMENT") || aqlField.getItemCategory().equals("CLUSTER")) {
                        int cut = aqlField.getJsonbItemPath().lastIndexOf(",/value");
                        if (cut != -1)
                            //we keep the path that select the json element value block, and call the formatting function
                            //to pass the actual value datatype into the json block
                            multiFields = new MultiFields(variableDefinition, DSL.field("(ehr.js_typed_element_value(" +  aqlField.getJsonbItemPath().substring(0, cut) + "}')::jsonb))"), templateId);

                        if (clause.equals(IQueryImpl.Clause.SELECT)) {
                            String alias = variableDefinition.getAlias();
                            if (alias == null)
                                alias = DefaultColumnId.value(variableDefinition);
                            aqlField.setField(aqlField.getSQLField().as(alias));
                        }
                    }
                }
            }
        }
        else
            throw new IllegalArgumentException("Unresolved aql path:" + variableDefinition.getPath());

        return multiFields;
    }
}
