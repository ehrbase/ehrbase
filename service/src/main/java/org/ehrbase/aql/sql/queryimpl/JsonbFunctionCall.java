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

/**
 * Created by christian on 5/9/2018.
 */
public class JsonbFunctionCall {

    private final List<String> resolvedPath;
    private final String marker;
    private final String function;
    private String rightJsonbExpressionPart;

    public JsonbFunctionCall(List<String> itemPathArray, String marker, String function) {
        this.function = function;
        this.marker = marker;

        var p = itemPathArray;
        // check if the list contains an entry with AQL_NODE_NAME_PREDICATE_MARKER
        while (p.contains(marker)) {
            p = resolveIterativeCall(p);
        }
        this.resolvedPath = p;
    }

    public List<String> resolve() {
        return resolvedPath;
    }

    private List<String> resolveIterativeCall(List<String> itemPathArray) {

        StringBuilder expression = new StringBuilder();
        int markerPos = itemPathArray.indexOf(marker);
        // prepare the function call
        expression.append("(");
        expression.append(function);
        expression.append("(");
        int startPos;
        // check if the table clause is already in the sequence in a nested call to aql_node_name_predicate
        if (!itemPathArray.get(0).contains(function)) {
            expression.append("(");
            expression.append(ENTRY.ENTRY_);
            startPos = 0;
        } else {
            expression.append(itemPathArray.get(0));
            startPos = 1;
        }
        expression.append("#>>");
        expression.append("'{");
        expression.append(String.join(",", itemPathArray.subList(startPos, markerPos)));
        expression.append("}'");
        expression.append(")");
        expression.append("::jsonb");
        expression.append(")");

        // Locate end tag (end of array or next marker)

        List<String> resultList = new ArrayList<>();
        List<String> rightList = itemPathArray.subList(markerPos + 1, itemPathArray.size());
        if (rightList.contains(marker)) {
            resultList.add(expression.toString());
            resultList.addAll(rightList);
        } else {
            rightJsonbExpressionPart = rightJsonExpression(rightList);
            expression.append(")");
            resultList.add(expression.toString());
        }

        return resultList;
    }

    private String rightJsonExpression(List<String> pathItems) {
        if (pathItems.isEmpty()) {
            return "";
        }

        StringBuilder expression = new StringBuilder();
        expression.append("#>>");
        expression.append("'");
        expression.append("{");
        expression.append(String.join(",", pathItems));
        expression.append("}");
        expression.append("'");

        return expression.toString();
    }

    public boolean hasRightMostJsonbExpression() {
        return rightJsonbExpressionPart != null;
    }

    public String getRightMostJsonbExpression() {
        return rightJsonbExpressionPart;
    }
}
