/*
 * Copyright (c) 2021-2022 vitasystems GmbH and Hannover Medical School.
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

import com.nedap.archie.rm.ehr.EhrStatus;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.ehrbase.openehr.sdk.response.dto.EhrStatusResponseData;
import org.springframework.http.ResponseEntity;

/**
 * OpenAPI specification for openEHR REST API EHR_STATUS resource.
 *
 * @author Renaud Subiger
 * @since 1.0
 */
@Tag(name = "EHR_STATUS")
@SuppressWarnings({"unused", "java:S107"})
public interface EhrStatusApiSpecification {

    @Operation(
            summary = "Get EHR_STATUS version by time",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-ehr_status-get"),
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "400"),
                @ApiResponse(responseCode = "404")
            })
    ResponseEntity<EhrStatusResponseData> getEhrStatusVersionByTime(UUID ehrId, String versionAtTime, String accept);

    @Operation(
            summary = "Get EHR_STATUS by version id",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-ehr_status-get-1"),
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404")})
    ResponseEntity<EhrStatusResponseData> getEhrStatusByVersionId(UUID ehrId, String versionUid, String accept);

    @Operation(
            summary = "Update EHR_STATUS",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-ehr_status-put"),
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "204"),
                @ApiResponse(responseCode = "400"),
                @ApiResponse(responseCode = "404"),
                @ApiResponse(responseCode = "412")
            })
    ResponseEntity<EhrStatusResponseData> updateEhrStatus(
            UUID ehrId, String versionUid, String prefer, String accept, String contentType, EhrStatus ehrStatus);
}
