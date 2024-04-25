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

import java.util.Map;
import javax.annotation.Nullable;

/**
 * Contains additional execution information about the executed AQL query.
 *
 * @param dryRun      indicates if the actual query was performed or not.
 * @param executedSQL raw SQL executed for the given AQL.
 * @param queryPlan   plane of the {@link #executedSQL}.
 */
public record AqlExecutionInfo(boolean dryRun, @Nullable String executedSQL, @Nullable Map<String, Object> queryPlan) {

    public static final AqlExecutionInfo None = new AqlExecutionInfo(false, null, null);
}
