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
package org.ehrbase.rest.api.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.RequestContext;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

class DirectoryControllerTest {

    private static final UUID EHR_ID = UUID.fromString("7d44b88c-4199-4bad-97dc-d78268e01398");

    private final DSLContext mockDsl = mock();
    private final EhrService mockEhrService = mock();
    private final RequestContext mockRequestContext = mock();

    private final DirectoryController controller = new DirectoryController(mockDsl, mockEhrService, mockRequestContext);

    @Test
    void createFolderInvalidEhrId() {
        assertThatThrownBy(() -> controller.createFolder("not-a-uuid", Map.of("name", "test")))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    @Test
    void createFolderEhrNotModifiable() {
        doThrow(new StateConflictException("EHR not modifiable"))
                .when(mockEhrService)
                .checkEhrExistsAndIsModifiable(EHR_ID);

        assertThatThrownBy(() -> controller.createFolder(EHR_ID.toString(), Map.of("name", "test")))
                .isInstanceOf(StateConflictException.class);
    }

    @Test
    void updateFolderInvalidFolderId() {
        assertThatThrownBy(() -> controller.updateFolder(EHR_ID.toString(), "not-a-uuid", Map.of("name", "new-name")))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    @Test
    void deleteFolderInvalidFolderId() {
        assertThatThrownBy(() -> controller.deleteFolder(EHR_ID.toString(), "not-a-uuid"))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    @Test
    void addItemInvalidCompositionId() {
        UUID folderId = UUID.randomUUID();
        assertThatThrownBy(() ->
                        controller.addItem(EHR_ID.toString(), folderId.toString(), Map.of("composition_id", "invalid")))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    @Test
    void getFolderHierarchyInvalidEhrId() {
        assertThatThrownBy(() -> controller.getFolderHierarchy("not-a-uuid", null))
                .isInstanceOf(InvalidApiParameterException.class);
    }
}
