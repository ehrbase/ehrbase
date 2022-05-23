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

import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathItemAsText;
import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_MARKER;
import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER;
import static org.ehrbase.aql.sql.queryimpl.value_field.Functions.apply;
import static org.ehrbase.jooq.pg.Routines.aqlNodeNamePredicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.TableField;
import org.jooq.impl.DSL;

/**
 * Created by christian on 5/9/2018.
 * build expression to call node name resolution on jsonb
 * specifically deals with function based sql encoding (used for composition attributes at this stage)
 * The function call syntax is:
 * ehr.aql_node_name_predicate(<jsonb structure>,'<a node name predicate>','the path of node to check for name/value predicate')
 */
public class FunctionBasedNodePredicateCall {

    private final List<String> itemPathArray;
    private final FieldResolutionContext fieldContext;

    public FunctionBasedNodePredicateCall(FieldResolutionContext fieldContext, List<String> itemPathArray) {
        this.fieldContext = fieldContext;
        this.itemPathArray = itemPathArray;
    }

    public Field resolve(Object function, TableField... tableFields) {
        List<String> expression = new ArrayList<>();
        expression.addAll(patchItemArray(itemPathArray));

        while (expression.contains(AQL_NODE_NAME_PREDICATE_MARKER)) {
            expression = resolveNodePredicateCall(expression, function, tableFields);
        }

        return DSL.field(expression.get(0));
    }

    /**
     * substitute with the node/name predicate function
     * @param itemPathArray
     * @return
     */
    private List<String> resolveNodePredicateCall(
            List<String> itemPathArray, Object function, TableField... tableFields) {
        Configuration configuration = fieldContext.getContext().configuration();
        int startList;

        List<String> expression = new ArrayList<>();

        Field nodeField;

        if (!itemPathArray.get(0).replace("\"", "").startsWith(QueryImplConstants.AQL_NODE_NAME_PREDICATE_FUNCTION)) {
            nodeField = DSL.field(apply(function, tableFields).toString()).cast(JSONB.class);
            startList = 0;
        } else {
            nodeField = DSL.field(itemPathArray.get(0));
            startList = 1;
        }

        int markerPos = ArrayUtils.indexOf(
                itemPathArray.toArray(new String[] {}), QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER, startList);

        if (markerPos < 0) // not found
        markerPos = itemPathArray.size();

        expression.add(aqlNodeNamePredicate(
                        DSL.field(jsonpathItemAsText(
                                        configuration,
                                        nodeField,
                                        itemPathArray
                                                .subList(startList, markerPos)
                                                .toArray(new String[] {})))
                                .cast(JSONB.class),
                        DSL.val(itemPathArray.get(markerPos + 1).replace("'", "")),
                        DSL.val(""))
                .toString());

        // Locate end tag (end of array or next marker)
        int endPos = itemPathArray.size();

        expression.addAll(Arrays.asList(rightPathExpression(itemPathArray, markerPos, endPos)));

        // end of iteration, wrap up
        String[] extractPathArguments = expression.subList(1, expression.size()).toArray(new String[] {});

        if (!expression.contains(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER)
                && extractPathArguments.length > 0) {
            List<String> resultList = new ArrayList<>();
            resultList.add(DSL.field(jsonpathItemAsText(
                            configuration, DSL.field(expression.get(0)).cast(JSONB.class), extractPathArguments))
                    .toString());
            expression = resultList;
        }

        return expression;
    }

    private String[] rightPathExpression(List<String> itemPathArray, int from, int to) {
        return itemPathArray
                .subList(
                        // test if the starting item is an index, then skip it as it is mutually exclusive with node
                        // name predicate node selection
                        itemPathArray.get(from + 2).matches("'[0-9]*'|#") ? from + 3 : from + 2, to)
                .toArray(new String[] {});
    }

    /**
     * remove json array marker and replace it with a dummy index '0'
     *
     * @return
     */
    private List<String> patchItemArray(List<String> itemPathArray) {

        List<String> items = new ArrayList<>();

        for (String item : itemPathArray) {
            if (item.equals(AQL_NODE_ITERATIVE_MARKER)) items.add("0");
            else items.add(item);
        }

        return items;
    }
}
