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
public class JsonbFunctionCall {

    private final List<String> itemPathArray;
    private final String marker;
    private final String function;

    public JsonbFunctionCall(List<String> itemPathArray, String marker, String function) {
        this.itemPathArray = itemPathArray;
        this.function = function;
        this.marker = marker;
    }

    public List<String> resolve() {

        List<String> resultList = new ArrayList<>();
        resultList.addAll(itemPathArray);

        while (resultList.contains(marker)) {
            resultList = resolveIterativeCall(resultList);
        }

        return resultList;
    }


    private List<String> resolveIterativeCall(List<String> itemPathArray) {

        List<String> resultList = new ArrayList<>();
        int startList = 0;

        //check if the list contains an entry with AQL_NODE_NAME_PREDICATE_MARKER
        if (itemPathArray.contains(marker)) {
            StringBuffer expression = new StringBuffer();
            int markerPos = itemPathArray.indexOf(marker);
            //prepare the function call
            expression.append("(");
            expression.append(function);
            expression.append("(");
            //check if the table clause is already in the sequence in a nested call to aql_node_name_predicate
            //TODO: better test really...
            if (!itemPathArray.get(0).contains(function)) {
                expression.append("(");
                expression.append(ENTRY.ENTRY_);
                startList = 0;
            } else {
                expression.append(itemPathArray.get(0));
                startList = 1;
            }
            expression.append("#>>");
            expression.append("'{");
            expression.append(StringUtils.join((itemPathArray.subList(startList, markerPos).toArray(new String[]{})), ","));
            expression.append("}'");
            expression.append(")");
            expression.append("::jsonb");
            expression.append(")");

            //Locate end tag (end of array or next marker)
            int endPos;
            if (itemPathArray.subList(markerPos + 1, itemPathArray.size()).contains(marker)) {
                resultList.add(expression.toString());
//                endPos = markerPos + itemPathArray.subList(markerPos+1, itemPathArray.size()).indexOf(marker) - 1;
                endPos = markerPos + 1;
                resultList.addAll(itemPathArray.subList(endPos, itemPathArray.size()));
            } else {
                expression.append("#>>");
                expression.append("'");
                expression.append("{");
                endPos = itemPathArray.size();
                expression.append(StringUtils.join((itemPathArray.subList(markerPos + 1, endPos).toArray(new String[]{})), ","));
                expression.append("}");
                expression.append("'");
                expression.append(")");
                resultList.add(expression.toString());
            }

            return resultList;
        } else
            return itemPathArray;
    }
}
