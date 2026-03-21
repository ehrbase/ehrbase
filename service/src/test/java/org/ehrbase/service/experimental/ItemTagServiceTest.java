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
package org.ehrbase.service.experimental;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * REWRITTEN for new architecture. ItemTagServiceImp and ItemTagRepository removed.
 * Item tags now handled via ItemTagController → DSLContext → ehr_system.item_tag directly.
 *
 * <p>Original 16 test scenarios migrated to rest-api module:
 * - bulkUpsert/find/delete operations → ItemTagControllerTest
 * - validTagKey/invalidTagKey/validTagValue/invalidTagValue → ItemTagControllerTest
 * - validTargetPathDeclaration → ItemTagControllerTest
 * - bulkUpdaterChangeOfOwnerError/buildUpdateTargetTypeMissmatch → ItemTagControllerTest
 *
 * <p>See rest-api/src/test/java/org/ehrbase/rest/api/controller/ItemTagControllerTest.java
 */
class ItemTagServiceTest {

    @Test
    void itemTagServiceRemovedInArchitectureRewrite() {
        assertThat(true)
                .as("ItemTagServiceImp removed — see ItemTagControllerTest in rest-api module")
                .isTrue();
    }
}
