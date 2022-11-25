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

/**
 * environment variable definitions. Generally not used as provided in application YAML profiles
 */
public class IterativeNodeConstants {

    private IterativeNodeConstants() {}

    // set the list of prefixes of nodes that are ignored when building the json_array expression (default is /content
    // and /events)
    public static final String ENV_AQL_ARRAY_IGNORE_NODE = "aql.ignoreIterativeNodeList";
    // set the depth of embedded arrays in json_array expression (default is 1)
    public static final String ENV_AQL_ARRAY_DEPTH = "aql.iterationScanDepth";
    // True force to use jsQuery
    public static final String ENV_AQL_USE_JSQUERY = "aql.useJsQuery";
}
