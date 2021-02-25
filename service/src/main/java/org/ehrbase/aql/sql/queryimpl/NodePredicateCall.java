/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.aql.sql.queryimpl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

/**
 * Created by christian on 5/9/2018.
 * build expression to call node name resolution on jsonb
 * The function call syntax is:
 * ehr.aql_node_name_predicate(<jsonb structure>,'<a node name predicate>','the path of node to check for name/value predicate')
 */
public class NodePredicateCall {

    List<String> itemPathArray;

    public NodePredicateCall(List<String> itemPathArray) {
        this.itemPathArray = itemPathArray;
    }

    public List<String> resolve() {

        List<String> resultList = new ArrayList<>();
        resultList.addAll(patchItemArray(itemPathArray));

        while (resultList.contains(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER)) {
            resultList = resolveNodePredicateCall(resultList);
        }

        return resultList;
    }


    private List<String> resolveNodePredicateCall(List<String> itemPathArray) {

        List<String> resultList = new ArrayList<>();
        int startList;

        //check if the list contains an entry with AQL_NODE_NAME_PREDICATE_MARKER
        if (itemPathArray.contains(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER)) {
            StringBuilder expression = new StringBuilder();
             //prepare the function call
            expression.append(QueryImplConstants.AQL_NODE_NAME_PREDICATE_FUNCTION);
            expression.append("(");
            //check if the table clause is already in the sequence in a nested call to aql_node_name_predicate
            //initial
            if (!itemPathArray.get(0).startsWith("("+QueryImplConstants.AQL_NODE_NAME_PREDICATE_FUNCTION)) {
                expression.append(ENTRY.ENTRY_);
                startList = 0;
            } else {
                expression.append(itemPathArray.get(0));
                startList = 1;
            }
            expression.append(",");

            int markerPos = ArrayUtils.indexOf(itemPathArray.toArray(new String[]{}), QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER, startList);

            if (markerPos < 0) //not found
                markerPos = itemPathArray.size();

            expression.append(itemPathArray.get(markerPos + 1)); //node name predicate
            expression.append(",");
            expression.append("'");
            expression.append(StringUtils.join((itemPathArray.subList(startList, markerPos).toArray(new String[]{})), ",")); //path of node as a list
            expression.append("'");
            expression.append(")");

            //Locate end tag (end of array or next marker)
            int endPos;
            //check if path segments contains a name node predicate tag for next iteration
            if (itemPathArray.subList(markerPos + 1, itemPathArray.size()).contains(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER)) {
                //resolve the path selection to the next marker
                endPos = ArrayUtils.indexOf(itemPathArray.toArray(new String[]{}), QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER, markerPos + 1);
                appendRightPathExpression(itemPathArray, expression, markerPos, endPos);
                //cast as jsonb for next iteration
                resultList.add(DSL.field("("+expression.toString()+")::jsonb").toString()); //insert the result in the path list
                //add the remaining part to the list for the next iteration
                resultList.addAll(itemPathArray.subList(endPos, itemPathArray.size()));
            } else {
                endPos = itemPathArray.size();
                if (markerPos+2 < endPos) {
                    appendRightPathExpression(itemPathArray, expression, markerPos, endPos);
                }
                resultList.add(expression.toString());
            }


            return resultList;
        } else
            return itemPathArray;
    }

    private StringBuilder appendRightPathExpression(List<String> itemPathArray, StringBuilder expression, int from, int to){
        expression.append("#>>");
        expression.append("'");
        expression.append("{");
        expression.append(StringUtils.join(
                (itemPathArray.subList(
                        //test if the starting item is an index, then skip it as it is mutually exclusive with node name predicate node selection
                        itemPathArray.get(from + 2).matches("[0-9]*|#") ? from + 3 : from + 2,
                        to
                ).toArray(new String[]{})), ","));
        expression.append("}");
        expression.append("'");

        return expression;
    }

    /**
     * remove json array marker and replace it with a dummy index '0'
     *
     * @return
     */
    private List<String> patchItemArray(List<String> itemPathArray) {

        List<String> items = new ArrayList<>();

        for (String item : itemPathArray) {
            if (item.equals(QueryImplConstants.AQL_NODE_ITERATIVE_MARKER))
                items.add("0");
            else
                items.add(item);
        }

        return items;
    }
}
