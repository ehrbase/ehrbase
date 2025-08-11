/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.changecontrol.VersionedObject;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.dto.VersionedEhrStatusDto;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.response.dto.OriginalVersionResponseData;
import org.ehrbase.openehr.sdk.response.dto.RevisionHistoryResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;

class OpenehrVersionedEhrStatusControllerTest {

    private static final String CONTEXT_PATH = "https://test.versioned-ehr-status.controller/ehrbase/rest";

    private final EhrService mockEhrService = mock();

    private final ContributionService mockContributionService = mock();

    private final EhrStatusDto mockEhrStatus = mock();

    private final OpenehrVersionedEhrStatusController spyController =
            spy(new OpenehrVersionedEhrStatusController(mockEhrService, mockContributionService));

    @BeforeEach
    void setUp() {
        Mockito.reset(mockEhrService, spyController, mockEhrStatus);
        doReturn(CONTEXT_PATH).when(spyController).getContextPath();
    }

    @AfterEach
    void tearDown() {
        // ensure the context is clean after each test
        RequestContextHolder.resetRequestAttributes();
    }

    private OpenehrVersionedEhrStatusController controller() {
        return spyController;
    }

    @Test
    void retrieveVersionedEhrStatusByEhrErrorEhrUUID() {

        OpenehrVersionedEhrStatusController controller = controller();
        assertThatThrownBy(() -> controller.retrieveVersionedEhrStatusByEhr("nopt-an-ehr-id", null))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("EHR not found, in fact, only UUID-type IDs are supported");
    }

    @Test
    void retrieveVersionedEhrStatusByEhr() {

        UUID ehrId = UUID.fromString("8994182c-517d-43d2-addf-f5abbf07cef2");
        VersionedObject<Object> versionedObject = new VersionedObject<>(
                new HierObjectId("337167e4-f325-47c1-8e9b-e9fb9fd136df::test::42"),
                new ObjectRef<>(new HierObjectId(ehrId.toString()), "local", "ehr"),
                new DvDateTime(OffsetDateTime.parse("2024-07-16T15:20:00Z")));

        doReturn(versionedObject).when(mockEhrService).getVersionedEhrStatus(ehrId);
        ResponseEntity<VersionedEhrStatusDto> response =
                controller().retrieveVersionedEhrStatusByEhr(ehrId.toString(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        VersionedEhrStatusDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.uid()).isEqualTo(versionedObject.getUid());
        assertThat(body.ownerId()).isEqualTo(versionedObject.getOwnerId());
        assertThat(OffsetDateTime.parse(body.timeCreated()).toInstant())
                .isEqualTo(Instant.from(versionedObject.getTimeCreated().getValue()));
    }

    @Test
    void retrieveVersionedEhrStatusRevisionHistoryByEhrErrorEhrUUID() {

        OpenehrVersionedEhrStatusController controller = controller();
        assertThatThrownBy(() -> controller.retrieveVersionedEhrStatusRevisionHistoryByEhr("nopt-an-ehr-id", null))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("EHR not found, in fact, only UUID-type IDs are supported");
    }

    @Test
    void retrieveVersionedEhrStatusRevisionHistoryByEhr() {

        UUID ehrId = UUID.fromString("0f0bd1c1-c210-410a-9420-1e4c432bd494");
        RevisionHistory revisionHistory = new RevisionHistory();
        revisionHistory.addItem(new RevisionHistoryItem(
                new ObjectVersionId("ae668b81-9d73-4a49-befa-9e18cb1b83cd::test::42"), List.of()));

        doReturn(revisionHistory).when(mockEhrService).getRevisionHistoryOfVersionedEhrStatus(ehrId);
        ResponseEntity<RevisionHistoryResponseData> response =
                controller().retrieveVersionedEhrStatusRevisionHistoryByEhr(ehrId.toString(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        RevisionHistoryResponseData body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getRevisionHistory()).isEqualTo(revisionHistory);
    }

    @Test
    void retrieveVersionOfEhrStatusByTimeErrorEhrUUID() {

        OpenehrVersionedEhrStatusController controller = controller();
        assertThatThrownBy(() -> controller.retrieveVersionOfEhrStatusByTime("nopt-an-ehr-id", null, null))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("EHR not found, in fact, only UUID-type IDs are supported");
    }

    @Test
    void retrieveVersionOfEhrStatusByTimeLatest() {

        OpenehrVersionedEhrStatusController controller = controller();
        runRetrieveOriginalVersionTest(
                (ehrId, originalVersion) -> controller.retrieveVersionOfEhrStatusByTime(ehrId.toString(), null, null));
    }

    @Test
    void retrieveVersionOfEhrStatusByTimeAt() {

        runRetrieveOriginalVersionTest((ehrId, originalVersion) -> {
            OffsetDateTime versionAtTime = OffsetDateTime.parse("2015-01-20T19:30:22.765+01:00");
            doReturn(originalVersion.getUid())
                    .when(mockEhrService)
                    .getEhrStatusVersionByTimestamp(ehrId, versionAtTime);
            return controller()
                    .retrieveVersionOfEhrStatusByTime(ehrId.toString(), "2015-01-20T19:30:22.765+01:00", null);
        });
    }

    @Test
    void retrieveVersionOfEhrStatusByVersionUidErrorEhrUUID() {

        OpenehrVersionedEhrStatusController controller = controller();
        assertThatThrownBy(() -> controller.retrieveVersionOfEhrStatusByVersionUid("nopt-an-ehr-id", null, null))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("EHR not found, in fact, only UUID-type IDs are supported");
    }

    @Test
    void retrieveVersionOfEhrStatusByVersionUidErrorVersionId() {

        OpenehrVersionedEhrStatusController controller = controller();
        assertThatThrownBy(() -> controller.retrieveVersionOfEhrStatusByVersionUid(
                        "efaae175-b3bd-44a1-9b99-79bc1191b200", "not-a-version-id", null))
                .isInstanceOf(InvalidApiParameterException.class)
                .hasMessage("VERSION UID parameter has wrong format: Invalid UUID string: not-a-version-id");
    }

    @Test
    void retrieveVersionOfEhrStatusByVersionUid() {

        OpenehrVersionedEhrStatusController controller = controller();
        runRetrieveOriginalVersionTest((ehrId, originalVersion) -> controller.retrieveVersionOfEhrStatusByVersionUid(
                ehrId.toString(), originalVersion.getUid().getValue(), null));
    }

    private void runRetrieveOriginalVersionTest(
            BiFunction<UUID, OriginalVersion<EhrStatusDto>, ResponseEntity<OriginalVersionResponseData<EhrStatusDto>>>
                    invocation) {

        UUID ehrId = UUID.fromString("77c7df3a-fe41-41e1-9ccd-f1140c2f54b4");
        ObjectVersionId objectVersionId = new ObjectVersionId("337167e4-f325-47c1-8e9b-e9fb9fd136df", "test", "42");
        HierObjectId contributionId = new HierObjectId("f23c4a76-4a76-46c0-8e85-a0d1591d5195");

        OriginalVersion<EhrStatusDto> originalVersion = new OriginalVersion<>();
        originalVersion.setUid(objectVersionId);
        originalVersion.setData(mockEhrStatus);
        originalVersion.setContribution(new ObjectRef<>(contributionId, "namespace", "EHR_STATUS"));

        doReturn(objectVersionId).when(mockEhrService).getLatestVersionUidOfStatus(ehrId);
        doReturn(Optional.of(originalVersion))
                .when(mockEhrService)
                .getEhrStatusAtVersion(
                        ehrId,
                        UUID.fromString(objectVersionId.getObjectId().getValue()),
                        Integer.parseInt(objectVersionId.getVersionTreeId().getValue()));

        ContributionDto contribution = new ContributionDto(
                UUID.fromString(originalVersion.getContribution().getId().getValue()), Map.of(), null);
        doReturn(contribution).when(mockContributionService).getContribution(ehrId, contribution.getUuid());

        ResponseEntity<OriginalVersionResponseData<EhrStatusDto>> response = invocation.apply(ehrId, originalVersion);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        OriginalVersionResponseData<EhrStatusDto> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getVersionId()).isEqualTo(objectVersionId);
        assertThat(body.getData()).isSameAs(originalVersion.getData());
        assertThat(body.getContribution().getUid()).isEqualTo(contributionId);
    }
}
