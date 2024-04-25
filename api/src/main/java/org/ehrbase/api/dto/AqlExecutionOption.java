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

/**
 * Additional instruction options for the AQL execution.
 *
 * @param dryRun            instruct to only perform a dry that does not execute the final query
 * @param returnExecutedSQL instruct to return the final SQL statement
 */
public record AqlExecutionOption(boolean dryRun, boolean returnExecutedSQL, boolean returnQueryPlan) {

    public static final AqlExecutionOption None = new AqlExecutionOption(false, false, false);

    public boolean isPresent() {
        return dryRun || returnExecutedSQL || returnQueryPlan;
    }
}