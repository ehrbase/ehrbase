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
package org.ehrbase.api.dto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ehrbase.api.service.AqlQueryService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryResultDto;

/**
 * The result AQL executed by {@link AqlQueryService#query(AqlQueryRequest)}.
 *
 * @param result        of the query
 * @param executionInfo additional {@link ExecutionInfo}
 */
public record AqlQueryResult(@Nonnull QueryResultDto result, @Nonnull ExecutionInfo executionInfo) {

    public AqlQueryResult(@Nonnull QueryResultDto result) {
        this(result, new ExecutionInfo());
    }

    /**
     * Contains additional execution information for the executed AQL query.
     *
     * @param executedSQL   raw SQL executed for the given AQL.
     */
    public record ExecutionInfo(@Nullable String executedSQL, boolean dryRun) {
        public ExecutionInfo() {
            this(null, false);
        }
    }
}
