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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.api.dto.EhrDto;
import org.ehrbase.api.dto.EhrStatusDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "EHR")
@SuppressWarnings("java:S107")
public interface EhrApiSpecification {

    @Operation(
            summary = "Create EHR",
            requestBody =
                    @RequestBody(
                            content = {
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        array = @ArraySchema(schema = @Schema(implementation = EhrDto.class)),
                                        examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                            }),
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description =
                                "Created, is returned when the EHR has been successfully created. The new EHR resource is returned in the body when the request's <code>Prefer</code>  header value is <code>return=representation</code> , otherwise only headers are returned.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = EhrDto.class)),
                                    examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                        }),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Bad Request, is returned when the request URL or body (if provided) could not be parsed or has invalid content.",
                        content = @Content(schema = @Schema(hidden = true))),
                @ApiResponse(
                        responseCode = "409",
                        description =
                                "Conflict, Unable to create a new EHR due to a conflict with an already existing EHR with the same subject id, namespace pair, whenever EHR_STATUS is supplied.",
                        content = @Content(schema = @Schema(hidden = true)))
            },
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#tag/EHR/operation/ehr_create"))
    ResponseEntity<EhrDto> createEhr(
            String openehrVersion,
            String openehrAuditDetails,
            String prefer,
            @ApiParameter.PrettyPrint String pretty,
            @Parameter(hidden = true, allowEmptyValue = true) EhrStatusDto ehrStatusDto);

    @Operation(
            summary = "Create EHR with id",
            requestBody =
                    @RequestBody(
                            content = {
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        array = @ArraySchema(schema = @Schema(implementation = EhrDto.class)),
                                        examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                            }),
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description =
                                "Created, is returned when the EHR has been successfully created. The new EHR resource is returned in the body when the request's <code>Prefer</code> header value is <code>return=representation</code> , otherwise only headers are returned.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = EhrDto.class)),
                                    examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                        }),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Bad Request, is returned when the request URL or body (if provided) could not be parsed or has invalid content.",
                        content = @Content(schema = @Schema(hidden = true))),
                @ApiResponse(
                        responseCode = "409",
                        description =
                                "Conflict, Unable to create a new EHR due to a conflict with an already existing EHR with the same subject id, namespace pair, whenever EHR_STATUS is supplied.",
                        content = @Content(schema = @Schema(hidden = true)))
            },
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/1.0.3/ehr.html#tag/EHR/operation/ehr_get_by_id"))
    ResponseEntity<EhrDto> createEhrWithId(
            String openehrVersion,
            String openehrAuditDetails,
            String prefer,
            String ehrIdString,
            @ApiParameter.PrettyPrint String pretty,
            EhrStatusDto ehrStatusDto);

    @Operation(
            summary = "Get EHR by id",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "OK, is returned when the requested EHR resource is successfully retrieved.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = EhrDto.class)),
                                    examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                        }),
                @ApiResponse(
                        responseCode = "404",
                        description = "Not Found, is returned when an EHR with <code>ehr_id</code>  does not exist.",
                        content = @Content(schema = @Schema(hidden = true)))
            },
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#tag/EHR/operation/ehr_get_by_id"))
    ResponseEntity<EhrDto> getEhrById(String ehrIdString, @ApiParameter.PrettyPrint String pretty);

    @Operation(
            summary = "Get EHR summary by subject id and namespace",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "OK, is returned when the requested EHR resource is successfully retrieved.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = EhrDto.class)),
                                    examples = @ExampleObject(ApiExample.EHR_STATUS_JSON))
                        }),
                @ApiResponse(
                        responseCode = "404",
                        description =
                                "Not Found, is returned when an EHR with supplied subject parameters does not exist.",
                        content = @Content(schema = @Schema(hidden = true)))
            },
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#tag/EHR/operation/ehr_get_by_subject"))
    ResponseEntity<EhrDto> getEhrBySubject(
            String subjectId, String subjectNamespace, @ApiParameter.PrettyPrint String pretty);
}
