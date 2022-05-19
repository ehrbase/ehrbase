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
package org.ehrbase.aql.sql.queryimpl;

public class QueryImplConstants {

    public static final String AQL_NODE_NAME_PREDICATE_MARKER = "$AQL_NODE_NAME_PREDICATE$";
    public static final String AQL_NODE_ITERATIVE_MARKER = "$AQL_NODE_ITERATIVE$";
    public static final String AQL_NODE_NAME_PREDICATE_FUNCTION = "ehr.aql_node_name_predicate";
    // we use an extended jsonb array elements function that returns a null jsonb object instead of an empty resultset
    // see  https://www.postgresql.org/docs/current/static/functions-json.html for more on usage
    public static final String AQL_NODE_ITERATIVE_FUNCTION = "ehr.xjsonb_array_elements";

    private QueryImplConstants() {}
}
