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
package org.ehrbase.aql.sql.queryimpl;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.impl.DSL;

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

        // check if the list contains an entry with AQL_NODE_NAME_PREDICATE_MARKER
        long namePredicateCount =
                IterableUtils.countMatches(itemPathArray, QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER::equals);
        if (namePredicateCount == 0) {
            return itemPathArray;
        }

        StringBuilder expression = new StringBuilder();
        // prepare the function call
        expression.append(QueryImplConstants.AQL_NODE_NAME_PREDICATE_FUNCTION);
        expression.append("(");
        // check if the table clause is already in the sequence in a nested call to aql_node_name_predicate
        // initial
        int startList;
        if (!itemPathArray.get(0).startsWith("(" + QueryImplConstants.AQL_NODE_NAME_PREDICATE_FUNCTION)) {
            expression.append(ENTRY.ENTRY_);
            startList = 0;
        } else {
            expression.append(itemPathArray.get(0));
            startList = 1;
        }
        expression.append(",");

        int markerPos = itemPathArray
                .subList(startList, itemPathArray.size())
                .indexOf(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER);
        if (markerPos < 0) { // not found
            markerPos = itemPathArray.size();
        } else {
            markerPos += startList;
        }

        expression.append(itemPathArray.get(markerPos + 1)); // node name predicate
        expression.append(",");
        expression.append("'");
        // path of node as a list
        expression.append(itemPathArray.subList(startList, markerPos).stream().collect(Collectors.joining(",")));
        expression.append("'");
        expression.append(")");

        // Locate end tag (end of array or next marker)
        List<String> resultList = new ArrayList<>();
        // check if path segments contains a name node predicate tag for next iteration
        if (namePredicateCount > 1) {
            // resolve the path selection to the next marker
            int endPos = markerPos
                    + 1
                    + itemPathArray
                            .subList(markerPos + 1, itemPathArray.size())
                            .indexOf(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER);
            appendRightPathExpression(itemPathArray, expression, markerPos, endPos, false);
            // cast as jsonb for next iteration
            // redundant cast potentially needed for WhereBinder::hackItem
            resultList.add(DSL.field("(" + expression.toString() + ")::jsonb")
                    .toString()); // insert the result in the path list
            // add the remaining part to the list for the next iteration
            resultList.addAll(itemPathArray.subList(endPos, itemPathArray.size()));

        } else {
            int endPos = itemPathArray.size();
            if (markerPos + 2 < endPos) {
                appendRightPathExpression(itemPathArray, expression, markerPos, endPos, true);
            }
            resultList.add(expression.toString());
        }

        return resultList;
    }

    private StringBuilder appendRightPathExpression(
            List<String> itemPathArray, StringBuilder expression, int from, int to, boolean resolveAsText) {
        expression.append(resolveAsText ? "#>>" : "#>");
        expression.append("'");
        expression.append("{");
        expression.append(StringUtils.join(
                (itemPathArray
                        .subList(
                                // test if the starting item is an index, then skip it as it is mutually exclusive with
                                // node name predicate node selection
                                itemPathArray.get(from + 2).matches("[0-9]*|#") ? from + 3 : from + 2, to)
                        .toArray(String[]::new)),
                ","));
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
            if (item.equals(QueryImplConstants.AQL_NODE_ITERATIVE_MARKER)) items.add("0");
            else items.add(item);
        }

        return items;
    }
}
