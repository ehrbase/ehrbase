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
package org.ehrbase.rest.api.controller.compliance;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.RequestContext;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HipaaControllerTest {

    private final DSLContext mockDsl = mock(DSLContext.class, Mockito.RETURNS_DEEP_STUBS);
    private final EhrService mockEhrService = mock();
    private final RequestContext mockRequestContext = mock();

    private final HipaaController controller = new HipaaController(mockDsl, mockEhrService, mockRequestContext);

    @Test
    void accountingOfDisclosuresInvalidEhrId() {
        assertThatThrownBy(() -> controller.getAccountingOfDisclosures("not-a-uuid", null, null, null, null))
                .isInstanceOf(InvalidApiParameterException.class);
    }
}
