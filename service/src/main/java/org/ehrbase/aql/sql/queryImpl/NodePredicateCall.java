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

package org.ehrbase.aql.sql.queryImpl;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

/**
 * Created by christian on 5/9/2018.
 */
public class NodePredicateCall {

    List<String> itemPathArray;

    public NodePredicateCall(List<String> itemPathArray) {
        this.itemPathArray = itemPathArray;
    }

    public List<String> resolve() {

        List<String> resultList = new ArrayList<>();
        resultList.addAll(patchItemArray(itemPathArray));

        while (resultList.contains(I_QueryImpl.AQL_NODE_NAME_PREDICATE_MARKER)) {
            resultList = resolveNodePredicateCall(resultList);
        }

        return resultList;
    }


    private List<String> resolveNodePredicateCall(List<String> itemPathArray) {

        List<String> resultList = new ArrayList<>();
        int startList = 0;

        //check if the list contains an entry with AQL_NODE_NAME_PREDICATE_MARKER
        if (itemPathArray.contains(I_QueryImpl.AQL_NODE_NAME_PREDICATE_MARKER)) {
            StringBuffer expression = new StringBuffer();
            int markerPos = itemPathArray.indexOf(I_QueryImpl.AQL_NODE_NAME_PREDICATE_MARKER);
            //prepare the function call
            expression.append(I_QueryImpl.AQL_NODE_NAME_PREDICATE_FUNCTION);
            expression.append("(");
            //check if the table clause is already in the sequence in a nested call to aql_node_name_predicate
            //TODO: better test really...
            if (!itemPathArray.get(0).startsWith(I_QueryImpl.AQL_NODE_NAME_PREDICATE_FUNCTION)) {
                expression.append(ENTRY.ENTRY_);
                startList = 0;
            } else {
                expression.append(itemPathArray.get(0));
                startList = 1;
            }
            expression.append(",");

            expression.append(itemPathArray.get(markerPos + 1)); //predicate
            expression.append(",");
            expression.append("'");
            expression.append(StringUtils.join((itemPathArray.subList(startList, markerPos).toArray(new String[]{})), ","));
            //skip position
            expression.append("'");
            expression.append(")");

            //Locate end tag (end of array or next marker)
            int endPos;
            if (itemPathArray.subList(markerPos + 1, itemPathArray.size()).contains(I_QueryImpl.AQL_NODE_NAME_PREDICATE_MARKER)) {
                resultList.add(expression.toString());
                endPos = markerPos + itemPathArray.subList(markerPos + 1, itemPathArray.size()).indexOf(I_QueryImpl.AQL_NODE_NAME_PREDICATE_MARKER) - 1;
                resultList.addAll(itemPathArray.subList(endPos, itemPathArray.size()));
            } else {
                expression.append("#>>");
                expression.append("'");
                expression.append("{");
                endPos = itemPathArray.size();
                expression.append(StringUtils.join((itemPathArray.subList(markerPos + 3, endPos).toArray(new String[]{})), ","));
                expression.append("}");
                expression.append("'");
                resultList.add(expression.toString());
            }


            return resultList;
        } else
            return itemPathArray;
    }

    /**
     * remove json array marker and replace it with a dummy index '0'
     *
     * @return
     */
    private List<String> patchItemArray(List<String> itemPathArray) {

        List<String> items = new ArrayList<>();

        for (String item : itemPathArray) {
            if (item.equals(I_QueryImpl.AQL_NODE_ITERATIVE_MARKER))
                items.add("0");
            else
                items.add(item);
        }

        return items;
    }
}
