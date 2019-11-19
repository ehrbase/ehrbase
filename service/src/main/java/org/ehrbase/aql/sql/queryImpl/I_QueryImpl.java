/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.jooq.Field;

import java.util.UUID;

/**
 * Created by christian on 5/6/2016.
 */
public interface I_QueryImpl {

    String AQL_NODE_NAME_PREDICATE_MARKER = "$AQL_NODE_NAME_PREDICATE$";
    String AQL_NODE_ITERATIVE_MARKER = "$AQL_NODE_ITERATIVE$";

    String AQL_NODE_NAME_PREDICATE_FUNCTION = "ehr.aql_node_name_predicate";

    //we use the standard jsonb array elements function
    //see  https://www.postgresql.org/docs/current/static/functions-json.html for more on usage
    String AQL_NODE_ITERATIVE_FUNCTION = "jsonb_array_elements";

    //set the list of prefixes of nodes that are ignored when building the json_array expression (default is /content and /events)
    String ENV_AQL_ARRAY_IGNORE_NODE = "aql.arrays.ignoreNodeRegexp";
    //set the depth of embedded arrays in json_array expression (default is 1)
    String ENV_AQL_ARRAY_DEPTH = "aql.arrays.depth";

    boolean isJsonDataBlock();

    boolean isContainsJqueryPath();

    String getJsonbItemPath();

    enum Clause {SELECT, WHERE, ORDERBY, FROM}

    Field<?> makeField(String templateId, UUID compositionId, String identifier, I_VariableDefinition variableDefinition, Clause clause);

    Field<?> whereField(String templateId, UUID compositionId, String identifier, I_VariableDefinition variableDefinition);

    public String getItemType();
}
