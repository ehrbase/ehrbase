/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;

public interface AqlQueryService {

    String SQL_COMMENTS_KEY = "SQL_COMMENTS";

    /**
     * simple query where the full json expression contains both query (key = 'q') and optional
     * parameters (key = 'query-parameters')
     *
     * @param aqlQueryRequest to perform
     * @return aqlQueryResult
     */
    QueryResultDto query(AqlQueryRequest aqlQueryRequest);

    static void addQueryNameComment(AqlQueryContext aqlQueryContext) {
        AqlQueryRequest aqlQueryRequest = aqlQueryContext.getAqlQueryRequest();
        Optional.ofNullable(aqlQueryRequest).map(AqlQueryRequest::queryName).ifPresent(n -> {
            List<String> comments = aqlQueryContext.getProperty(SQL_COMMENTS_KEY);
            if (comments == null) {
                comments = new ArrayList<>();
                aqlQueryContext.setProperty(SQL_COMMENTS_KEY, comments);
            }
            comments.add("Stored Query: " + n);
        });
    }
}
