/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.openehr.specification;

import com.nedap.archie.rm.composition.Composition;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import org.ehrbase.openehr.sdk.response.dto.OriginalVersionResponseData;
import org.ehrbase.openehr.sdk.response.dto.RevisionHistoryResponseData;
import org.ehrbase.openehr.sdk.response.dto.VersionedObjectResponseData;
import org.springframework.http.ResponseEntity;

@Tag(name = "VERSIONED_COMPOSITION")
@SuppressWarnings("java:S107")
public interface VersionedCompositionApiSpecification {

    @Operation(
            summary = "Get versioned composition",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-versioned_composition-get"))
    ResponseEntity<VersionedObjectResponseData<Composition>> retrieveVersionedCompositionByVersionedObjectUid(
            String accept, String ehrIdString, String versionedObjectUid);

    @Operation(
            summary = "Get versioned composition revision history",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-versioned_composition-get-1"))
    ResponseEntity<RevisionHistoryResponseData> retrieveVersionedCompositionRevisionHistoryByEhr(
            String accept, String ehrIdString, String versionedObjectUid);

    @Operation(
            summary = "Get versioned composition version by id",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-versioned_composition-get-2"))
    ResponseEntity<OriginalVersionResponseData<Composition>> retrieveVersionOfCompositionByVersionUid(
            String accept, String ehrIdString, String versionedObjectUid, String versionUid);

    @Operation(
            summary = "Get versioned composition version at time",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get-3"))
    ResponseEntity<OriginalVersionResponseData<Composition>> retrieveVersionOfCompositionByTime(
            String accept, String ehrIdString, String versionedObjectUid, LocalDateTime versionAtTime);
}
