/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.api.service;

import org.ehrbase.api.definitions.QueryMode;
import org.ehrbase.api.dto.QueryDefinitionResultDto;
import org.ehrbase.api.dto.QueryResultDto;

import java.util.List;
import java.util.Map;

public interface QueryService extends BaseService {
    /**
     * simple query where the full json expression contains both query (key = 'q') and optional
     * parameters (key = 'query-parameters')
      * @param queryString
     * @param queryMode
     * @param explain
     * @return
     */
    QueryResultDto query(String queryString, QueryMode queryMode, boolean explain);

    QueryResultDto query(String queryString, Map<String, Object> parameters, QueryMode queryMode, boolean explain);

    //=== DEFINITION: manage stored queries
    List<QueryDefinitionResultDto> retrieveStoredQueries(String fullyQualifiedName);

    QueryDefinitionResultDto retrieveStoredQuery(String qualifiedName, String version);

    //=== DEFINITION: manage stored queries
    QueryDefinitionResultDto createStoredQuery(String qualifiedName, String version, String queryString);

    QueryDefinitionResultDto updateStoredQuery(String qualifiedName, String version, String queryString);

    QueryDefinitionResultDto deleteStoredQuery(String qualifiedName, String version);
}
