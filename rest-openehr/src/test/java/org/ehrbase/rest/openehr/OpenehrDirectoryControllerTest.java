/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.rest.openehr;

import static org.ehrbase.rest.openehr.OpenehrEhrControllerTest.assertSentOnlyDuringTheCommit;
import static org.ehrbase.rest.openehr.OpenehrEhrControllerTest.auditHeaders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.ehrbase.api.audit.CommitAuditHeaders;
import org.ehrbase.api.service.DirectoryService;
import org.ehrbase.rest.BaseController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;

class OpenehrDirectoryControllerTest {

    private static final UUID EHR_ID = UUID.fromString("7d44b88c-4199-4bad-97dc-d78268e01398");
    private static final String FOLDER_VERSION_ID = "f8ac7f6e-0a6f-4c2b-9ff6-8b5ef4b2dfa1::test.ehr.controller::1";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final DirectoryService mockDirectoryService = mock();
    private final OpenehrDirectoryController controller = spy(new OpenehrDirectoryController(mockDirectoryService));
    private final AtomicReference<CommitAuditHeaders> sent = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        doReturn("https://test.ehrbase.org/ehrbase").when(controller).getContextPath();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createDirectoryBindsTheAuditHeadersForTheCommit() {
        doAnswer(captureSent(folder())).when(mockDirectoryService).create(any(), any());

        controller.createDirectory(EHR_ID, auditHeaders(), JSON, JSON, BaseController.RETURN_MINIMAL, folder());

        assertSentOnlyDuringTheCommit(sent);
    }

    @Test
    void updateDirectoryBindsTheAuditHeadersForTheCommit() {
        doAnswer(captureSent(folder())).when(mockDirectoryService).update(any(), any(), any());

        controller.updateDirectory(
                EHR_ID,
                new ObjectVersionId(FOLDER_VERSION_ID),
                JSON,
                JSON,
                BaseController.RETURN_MINIMAL,
                auditHeaders(),
                folder());

        assertSentOnlyDuringTheCommit(sent);
    }

    @Test
    void deleteDirectoryBindsTheAuditHeadersForTheCommit() {
        doAnswer(captureSent(null)).when(mockDirectoryService).delete(any(), any());

        controller.deleteDirectory(EHR_ID, auditHeaders(), JSON, new ObjectVersionId(FOLDER_VERSION_ID));

        assertSentOnlyDuringTheCommit(sent);
    }

    /// Answers the service call with `result` and remembers the bound `CommitAuditHeaders.SENT`.
    private <T> Answer<T> captureSent(T result) {
        return invocation -> {
            sent.set(CommitAuditHeaders.SENT.get());
            return result;
        };
    }

    private static Folder folder() {
        Folder folder = new Folder();
        folder.setUid(new ObjectVersionId(FOLDER_VERSION_ID));
        folder.setArchetypeNodeId("openEHR-EHR-FOLDER.generic.v1");
        folder.setName(new DvText("root"));
        return folder;
    }
}
