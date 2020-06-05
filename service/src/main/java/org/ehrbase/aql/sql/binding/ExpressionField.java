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
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryImpl.*;
import org.ehrbase.aql.sql.queryImpl.attribute.ehr.EhrResolver;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.UUID;

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

    Field<?> toSql(String className, String template_id, UUID comp_id, String identifier) {

        Field<?> field;

        switch (className) {
            case "COMPOSITION":
                if (variableDefinition.getPath() != null && variableDefinition.getPath().startsWith("content")) {
                    field = jsonbEntryQuery.makeField(template_id, comp_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
                    containsJsonDataBlock = jsonbEntryQuery.isJsonDataBlock();
                    jsonbItemPath = jsonbEntryQuery.getJsonbItemPath();
                } else {
                    field = compositionAttributeQuery.makeField(template_id, comp_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
                    containsJsonDataBlock = compositionAttributeQuery.isJsonDataBlock();
                }
                optionalPath = variableDefinition.getPath();
                break;
            case "EHR":
                if (EhrResolver.isEhrAttribute(variableDefinition.getPath()))
                    variableDefinition.setDistinct(true);

                field = compositionAttributeQuery.makeField(template_id, comp_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
                containsJsonDataBlock = compositionAttributeQuery.isJsonDataBlock();
                optionalPath = variableDefinition.getPath();
                break;
            default:
                field = jsonbEntryQuery.makeField(template_id, comp_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
                jsonbItemPath = jsonbEntryQuery.getJsonbItemPath();
                containsJsonDataBlock = containsJsonDataBlock | jsonbEntryQuery.isJsonDataBlock();
                if (jsonbEntryQuery.isJsonDataBlock() ) {

                    if (jsonbEntryQuery.getItemType() != null){
                        Class itemClass = ArchieRMInfoLookup.getInstance().getClass(jsonbEntryQuery.getItemType());

                        if (itemClass == null && className != null) //this may occur f.e. for itemType 'MULTIPLE'. try we classname
                            itemClass = ArchieRMInfoLookup.getInstance().getClass(className);

                        if (DataValue.class.isAssignableFrom(itemClass)) {
                            VariableAqlPath variableAqlPath = new VariableAqlPath(variableDefinition.getPath());
                            if (variableAqlPath.getSuffix().equals("value")){
                                if (className.equals("COMPOSITION")) { //assumes this is a data value within an ELEMENT
                                    try {
                                        I_VariableDefinition variableDefinition1 = variableDefinition.clone();
                                        variableDefinition1.setPath(variableAqlPath.getInfix());
                                        field = jsonbEntryQuery.makeField(template_id, comp_id, identifier, variableDefinition1, I_QueryImpl.Clause.SELECT);
                                        jsonbItemPath = jsonbEntryQuery.getJsonbItemPath();
                                        rootJsonKey = variableAqlPath.getSuffix();
                                    } catch (CloneNotSupportedException e) {
                                        throw new InternalServerException("Couldn't handle variable:" + variableDefinition.toString() + "Code error:" + e);
                                    }
                                }
                                else if (jsonbEntryQuery.getItemCategory().equals("ELEMENT") || jsonbEntryQuery.getItemCategory().equals("CLUSTER")){
                                    int cut = jsonbItemPath.lastIndexOf(",/value");
                                    if (cut != -1)
                                        //we keep the path that select the json element value block, and call the formatting function
                                        //to pass the actual value datatype into the json block
                                        field = DSL.field("(ehr.js_typed_element_value(" + jsonbItemPath.substring(0, cut) + "}')::jsonb))");

                                    String alias = variableDefinition.getAlias();
                                    if (alias == null)
                                        alias = new DefaultColumnId().value(variableDefinition);
                                    field = field.as(alias);
                                }
                            }

                        }
                    }
                }
                break;
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
