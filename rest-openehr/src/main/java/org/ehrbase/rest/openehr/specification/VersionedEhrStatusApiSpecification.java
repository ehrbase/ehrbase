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
package org.ehrbase.rest.openehr.specification;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.dto.VersionedEhrStatusDto;
import org.ehrbase.openehr.sdk.response.dto.OriginalVersionResponseData;
import org.ehrbase.openehr.sdk.response.dto.RevisionHistoryResponseData;
import org.springframework.http.ResponseEntity;

@Tag(name = "VERSIONED_EHR_STATUS")
@SuppressWarnings("java:S107")
public interface VersionedEhrStatusApiSpecification {

    @Operation(
            summary = "Get versioned EHR_STATUS",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get"))
    ResponseEntity<VersionedEhrStatusDto> retrieveVersionedEhrStatusByEhr(
            String ehrIdString, @ApiParameter.PrettyPrint String pretty);

    @Operation(
            summary = "Get versioned EHR_STATUS revision history",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get-1"))
    ResponseEntity<RevisionHistoryResponseData> retrieveVersionedEhrStatusRevisionHistoryByEhr(
            String ehrIdString, @ApiParameter.PrettyPrint String pretty);

    @Operation(
            summary = "Get versioned EHR_STATUS version by time",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get-2"))
    ResponseEntity<OriginalVersionResponseData<EhrStatusDto>> retrieveVersionOfEhrStatusByTime(
            String ehrIdString, String versionAtTime, @ApiParameter.PrettyPrint String pretty);

    @Operation(
            summary = "Get versioned EHR_STATUS version by id",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get-3"))
    ResponseEntity<OriginalVersionResponseData<EhrStatusDto>> retrieveVersionOfEhrStatusByVersionUid(
            String ehrIdString, String versionUid, @ApiParameter.PrettyPrint String pretty);
}
