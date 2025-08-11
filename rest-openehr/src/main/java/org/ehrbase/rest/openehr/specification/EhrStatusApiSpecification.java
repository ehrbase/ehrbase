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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.ehrbase.api.dto.EhrStatusDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * OpenAPI specification for openEHR REST API EHR_STATUS resource.
 */
@Tag(name = "EHR_STATUS")
@SuppressWarnings({"unused", "java:S107"})
public interface EhrStatusApiSpecification {

    // @format:off

    @Operation(
            summary = "Get EHR_STATUS version by time",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "OK, is returned when the requested EHR_STATUS is successfully retrieved.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = EhrStatusDto.class)),
                                    examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                        }),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Bad Request, is returned when the request has invalid content such as an invalid <code>version_at_time</code> format.",
                        content = @Content(schema = @Schema(hidden = true))),
                @ApiResponse(
                        responseCode = "404",
                        description =
                                "Not Found, is returned when an EHR with <code>ehr_id</code> does not exist, or when a version of the resource identified by the request parameters (at specified <code>version_at_time</code>) does not exist.",
                        content = @Content(schema = @Schema(hidden = true)))
            },
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-ehr_status-get"))
    ResponseEntity<EhrStatusDto> getEhrStatusVersionByTime(
            UUID ehrId, String versionAtTime, @ApiParameter.PrettyPrint String pretty);

    @Operation(
            summary = "Get EHR_STATUS by version id",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "OK, is returned when the requested EHR_STATUS is successfully retrieved.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = EhrStatusDto.class)),
                                    examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                        }),
                @ApiResponse(
                        responseCode = "404",
                        description =
                                "Not Found, is returned when an EHR with <code>ehr_id</code> does not exist, or when the <code>version_uid</code> does not exist.",
                        content = @Content(schema = @Schema(hidden = true)))
            },
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-ehr_status-get-1"))
    ResponseEntity<EhrStatusDto> getEhrStatusByVersionId(
            UUID ehrId, String versionUid, @ApiParameter.PrettyPrint String pretty);

    @Operation(
            summary = "Update EHR_STATUS",
            requestBody =
                    @RequestBody(
                            content = {
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        array = @ArraySchema(schema = @Schema(implementation = EhrStatusDto.class)),
                                        examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                            }),
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description =
                                "OK, is returned when the EHR_STATUS is successfully updated, and the updated resource is returned in the body when <code>Prefer</code> header value is <code>return=representation</code>.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = EhrStatusDto.class)),
                                    examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                        }),
                @ApiResponse(
                        responseCode = "204",
                        description =
                                "No Content, is returned when the <code>Prefer</code> header is missing or is set to <code>return=minimal</code>",
                        content = @Content(schema = @Schema(hidden = true))),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Bad Request, is returned when the request URL or body (if provided) could not be parsed or has invalid content.",
                        content = @Content(schema = @Schema(hidden = true))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Not Found, is returned when an EHR with <code>ehr_id</code> does not exist.",
                        content = @Content(schema = @Schema(hidden = true))),
                @ApiResponse(
                        responseCode = "412",
                        description =
                                "Precondition Failed, is returned when <code>If-Match</code> request header doesn't match the latest version on the service side. Returns also latest <code>version_uid</code> in the <code>Location</code> and <codeETag></code> headers.",
                        content = @Content(schema = @Schema(hidden = true)))
            },
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-ehr_status-put"))
    ResponseEntity<EhrStatusDto> updateEhrStatus(
            UUID ehrId,
            String versionUid,
            String prefer,
            @ApiParameter.PrettyPrint String pretty,
            EhrStatusDto ehrStatus);
}
