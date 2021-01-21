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
import org.ehrbase.aql.sql.queryimpl.*;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.Objects;

/**
 * evaluate the SQL expression for locatables in the ITEM_STRUCTURE: OBSERVATION, INSTRUCTION, CLUSTER etc.
 * NB. At the moment, direct resolution of ELEMENT is not supported.
 */
@SuppressWarnings({"java:S3776","java:S3740","java:S1452"})
public class LocatableItem {

    private final CompositionAttributeQuery compositionAttributeQuery;
    private final JsonbEntryQuery jsonbEntryQuery;
    private boolean containsJsonDataBlock;
    private String jsonbItemPath;
    private String optionalPath;
    private String rootJsonKey;

    public LocatableItem(CompositionAttributeQuery compositionAttributeQuery, JsonbEntryQuery jsonbEntryQuery) {
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.jsonbEntryQuery = jsonbEntryQuery;
    }

    public Field<?> toSql(String templateId, I_VariableDefinition variableDefinition, String className){
        Field<?> field;

        field = jsonbEntryQuery.makeField(templateId, variableDefinition.getIdentifier(), variableDefinition, IQueryImpl.Clause.SELECT);
        jsonbItemPath = jsonbEntryQuery.getJsonbItemPath();
        containsJsonDataBlock |= jsonbEntryQuery.isJsonDataBlock();
        if (jsonbEntryQuery.isJsonDataBlock() ) {

            if (jsonbEntryQuery.getItemType() != null){
                Class itemClass = ArchieRMInfoLookup.getInstance().getClass(jsonbEntryQuery.getItemType());

                if (itemClass == null && className != null) //this may occur f.e. for itemType 'MULTIPLE'. try we classname
                    itemClass = ArchieRMInfoLookup.getInstance().getClass(className);

                if (DataValue.class.isAssignableFrom(itemClass)) {
                    VariableAqlPath variableAqlPath = new VariableAqlPath(variableDefinition.getPath());
                    if (variableAqlPath.getSuffix().equals("value")){
                        if (Objects.equals(className, "COMPOSITION")) { //assumes this is a data value within an ELEMENT
                            try {
                                I_VariableDefinition variableDefinition1 = variableDefinition.clone();
                                variableDefinition1.setPath(variableAqlPath.getInfix());
                                field = jsonbEntryQuery.makeField(templateId, variableDefinition.getIdentifier(), variableDefinition1, IQueryImpl.Clause.SELECT);
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
                                alias = DefaultColumnId.value(variableDefinition);
                            field = field.as(alias);
                        }
                    }

                }
            }
            else
                throw new IllegalStateException("Internal: unsupported item type for:"+variableDefinition);
        }
        return field;
    }

    public boolean isContainsJsonDataBlock() {
        return containsJsonDataBlock;
    }

    public String getJsonbItemPath() {
        return jsonbItemPath;
    }

    public String getOptionalPath() {
        return optionalPath;
    }

    public String getRootJsonKey() {
        return rootJsonKey;
    }

    public void setUseEntry() {
        compositionAttributeQuery.setUseEntry(true);
    }
}
