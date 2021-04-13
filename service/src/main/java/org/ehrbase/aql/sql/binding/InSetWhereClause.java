/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryimpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;

import java.util.ArrayList;
import java.util.List;

import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION;

/**
 * deals with "transparent" swap of arguments whenever the left operand is an IdentifiedPath in a (NOT)'IN' clause:
 * | OPEN_PAR* identifiedOperand NOT? IN OPEN_PAR  (identifiedOperand|matchesOperand) CLOSE_PAR CLOSE_PAR*
 * The expected result is an expression compatible with scalar 'IN' (set).
 * This is hack is currently (12.4.21) required due to SDK limitation (IN is not supported). It should be removed whenever updated
 * NB. MATCHES is already substituted as IN by QueryCompilerPass2
 * TODO: deprecate whenever SDK supports the IN operator
 */

public class InSetWhereClause {

    private final List<Object> whereItems;
    private final PathResolver pathResolver;
    private final I_DomainAccess domainAccess;
    private final JsonbEntryQuery jsonbEntryQuery;
    private final CompositionAttributeQuery compositionAttributeQuery;

    public InSetWhereClause(List<Object> whereItems, PathResolver pathResolver, I_DomainAccess domainAccess, JsonbEntryQuery jsonbEntryQuery, CompositionAttributeQuery compositionAttributeQuery) {
        this.whereItems = whereItems;
        this.pathResolver = pathResolver;
        this.domainAccess = domainAccess;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.compositionAttributeQuery = compositionAttributeQuery;
    }

    public List<Object> swapIfRequired(String templateId, String compositionName){

        List<Object> updatedList = new ArrayList<>(whereItems);

        for (int cursor = 0; cursor < updatedList.size(); cursor++) {

            if (updatedList.get(cursor) instanceof I_VariableDefinition){
                //we check if the variable encoding implies set returning function
                TaggedStringBuilder taggedStringBuilder = new WhereVariable(pathResolver, domainAccess, jsonbEntryQuery, compositionAttributeQuery).encode(templateId, (I_VariableDefinition) updatedList.get(cursor), true, compositionName);
                if (!taggedStringBuilder.toString().contains(AQL_NODE_ITERATIVE_FUNCTION))
                    break;
                //lookahead to check if the condition deals with IN | NOT IN set containment condition
                if (cursor+1 >= updatedList.size()||cursor+2 >= updatedList.size())
                    break;
                if (updatedList.get(cursor + 1) instanceof String){
                    String lookahead1 = ((String)updatedList.get(cursor + 1)).strip();
                    if (lookahead1.equalsIgnoreCase("NOT")){
                        String lookahead2 = ((String)updatedList.get(cursor + 2)).strip();
                        if (lookahead2.equalsIgnoreCase("IN")){
                            //perform swap of argument at location
                            swapItemsAtOffset(updatedList, cursor, 4);
                        }
                    }
                    else {
                        String lookahead2 = ((String) updatedList.get(cursor + 1)).strip();
                        if (lookahead2.equalsIgnoreCase("IN")){
                            //perform swap of argument at location
                            swapItemsAtOffset(updatedList, cursor, 3);
                        }
                    }
                }
            }
        }
        return updatedList;
    }

    private void swapItemsAtOffset(List<Object> itemList, int cursor, int offset){
        //perform swap of argument at location
        Object arg1 = itemList.get(cursor);
        itemList.set(cursor, itemList.get(cursor+offset));
        itemList.set(cursor+offset, arg1);
    }
}
