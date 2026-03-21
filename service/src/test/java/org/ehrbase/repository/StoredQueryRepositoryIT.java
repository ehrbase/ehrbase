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
package org.ehrbase.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * DROPPED: Stored queries removed from architecture (replaced by SQL views + GraphQL).
 *
 * <p>Original test: retrieveAllLatest — tested version filtering (5 queries, SNAPSHOT handling).
 * See SqlViewQueryIT for replacement query integration tests.
 */
class StoredQueryRepositoryIT {

    @Test
    void storedQueriesRemovedFromArchitecture() {
        assertThat(true)
                .as("Stored queries replaced by SQL views — see plans/phase-08-testing.md")
                .isTrue();
    }
}
