/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.binding;

import java.util.List;

/**
 * deals with sub-query expressions to identify whether correlated query is required (LATERAL join)
 * Sub queries are specified at https://www.postgresql.org/docs/13/functions-subquery.html
 * AQL MATCHES is substituted with IN since it is not a standard SQL operator
 */
public class InSetWhereClause {

    private final List<Object> whereItems;

    private static final String IGNORE_OPERATOR = "NOT|=|>|<|>=|<=|!=";
    private static final String SUB_EXPRESSION_OPERATOR = "IN|ANY|SOME|ALL";

    public InSetWhereClause(List<Object> whereItems) {
        this.whereItems = whereItems;
    }

    public boolean isInSubQueryExpression(int cursor) {

        if (cursor + 1 >= whereItems.size() || cursor + 2 >= whereItems.size()) return false;
        if (whereItems.get(cursor + 1) instanceof String) {
            String lookahead1 = ((String) whereItems.get(cursor + 1)).strip();
            String lookahead2;
            if (lookahead1.toUpperCase().matches(IGNORE_OPERATOR)) {
                lookahead2 = ((String) whereItems.get(cursor + 2)).strip();
            } else {
                lookahead2 = ((String) whereItems.get(cursor + 1)).strip();
            }
            return lookahead2.toUpperCase().matches(SUB_EXPRESSION_OPERATOR);
        }

        return false;
    }
}
