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

import org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery;

/**
 * handles jsquery expression
 */
public class WhereJsQueryExpression {

    TaggedStringBuilder expression;
    Boolean requiresJSQueryClosure;
    Boolean isFollowedBySQLConditionalOperator;

    public WhereJsQueryExpression(
            TaggedStringBuilder expression,
            Boolean requiresJSQueryClosure,
            Boolean isFollowedBySQLConditionalOperator) {
        this.expression = expression;
        this.requiresJSQueryClosure = requiresJSQueryClosure;
        this.isFollowedBySQLConditionalOperator = isFollowedBySQLConditionalOperator;
    }

    /**
     * append the JsQuery tag closure at the right location (e.g. before the following parenthesis!)
     * @return
     */
    private TaggedStringBuilder closeWithJsQueryTag() {
        if (Boolean.FALSE.equals(requiresJSQueryClosure)) return expression;

        if (expression.toString().lastIndexOf(')') == 0) return expression;

        for (int i = expression.toString().lastIndexOf(')') - 1; i >= 0; i--) {
            while (i >= 0 && expression.toString().charAt(i) == ')') {
                i--;
            }
            // replace the last parenthesis not preceded by another
            expression.insert(i + 1, JsonbEntryQuery.JSQUERY_CLOSE);
        }

        return expression;
    }

    public TaggedStringBuilder closure() {
        if (Boolean.TRUE.equals(requiresJSQueryClosure)) {
            if (Boolean.FALSE.equals(isFollowedBySQLConditionalOperator)) {
                if (expression.toString().charAt(expression.length() - 1) == ')') expression = closeWithJsQueryTag();
                else expression.append(JsonbEntryQuery.JSQUERY_CLOSE);
            }
            isFollowedBySQLConditionalOperator = false;
            requiresJSQueryClosure = false;
        }
        return expression;
    }
}
