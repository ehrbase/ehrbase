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
package org.ehrbase.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;

public interface QueryService extends BaseService {
    /**
     * simple query where the full json expression contains both query (key = 'q') and optional
     * parameters (key = 'query-parameters')
     *
     * @param queryString
     * @param parameters  optional parameters
     * @param explain
     * @return
     */
    QueryResultDto query(
            String queryString,
            Map<String, Object> parameters,
            boolean explain,
            Map<String, Set<Object>> auditResultMap);

    // === DEFINITION: manage stored queries
    List<QueryDefinitionResultDto> retrieveStoredQueries(String fullyQualifiedName);

    QueryDefinitionResultDto retrieveStoredQuery(String qualifiedName, String version);

    // === DEFINITION: manage stored queries
    QueryDefinitionResultDto createStoredQuery(String qualifiedName, String version, String queryString);

    QueryDefinitionResultDto deleteStoredQuery(String qualifiedName, String version);
}
