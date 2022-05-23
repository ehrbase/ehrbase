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

import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION;

public class SetReturningFunction {

    private final String expression;

    private static final String SUPPORTED_SET_RETURNING_FUNCTION =
            // ignore new line, any char, check for boundary
            "(?s).*\\b(" +
                    // json array
                    AQL_NODE_ITERATIVE_FUNCTION
                    +
                    // String array
                    "|regexp_match|regexp_matches|regexp_split_to_array|regexp_split_to_table"
                    + ")\\b.*";

    public SetReturningFunction(String expression) {
        this.expression = expression;
    }

    public boolean isUsed() {
        if (expression == null) return false;
        return expression.toLowerCase().matches(SUPPORTED_SET_RETURNING_FUNCTION);
    }
}
