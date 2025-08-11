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

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.ehrbase.openehr.sdk.response.dto.DirectoryResponseData;
import org.springframework.http.ResponseEntity;

/**
 * OpenAPI specification for openEHR REST API DIRECTORY resource.
 */
@Tag(name = "DIRECTORY")
@SuppressWarnings({"unused", "java:S107"})
public interface DirectoryApiSpecification {

    @Operation(
            summary = "Create directory",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-post"),
            responses = {
                @ApiResponse(responseCode = "201"),
                @ApiResponse(responseCode = "400"),
                @ApiResponse(responseCode = "404")
            })
    ResponseEntity<DirectoryResponseData> createDirectory(
            String prefer,
            String openEhrVersion,
            String openEhrAuditDetails,
            String contentType,
            String accept,
            UUID ehrId,
            @ApiParameter.PrettyPrint String pretty,
            Folder folder);

    @Operation(
            summary = "Update directory",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-put"),
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "204"),
                @ApiResponse(responseCode = "400"),
                @ApiResponse(responseCode = "404"),
                @ApiResponse(responseCode = "412")
            })
    ResponseEntity<DirectoryResponseData> updateDirectory(
            String openEhrAuditDetails,
            ObjectVersionId folderId,
            String contentType,
            String accept,
            String prefer,
            String openEhrVersion,
            UUID ehrId,
            @ApiParameter.PrettyPrint String pretty,
            Folder folder);

    @Operation(
            summary = "Delete directory",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-delete"),
            responses = {
                @ApiResponse(responseCode = "204"),
                @ApiResponse(responseCode = "400"),
                @ApiResponse(responseCode = "404"),
                @ApiResponse(responseCode = "412")
            })
    ResponseEntity<DirectoryResponseData> deleteDirectory(
            String accept,
            ObjectVersionId folderId,
            String openEhrAuditDetails,
            String openEhrVersion,
            UUID ehrId,
            @ApiParameter.PrettyPrint String pretty);

    @Operation(
            summary = "Get folder in directory version",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-get"),
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404")})
    ResponseEntity<DirectoryResponseData> getFolderInDirectory(
            String accept,
            ObjectVersionId versionUid,
            String path,
            UUID ehrId,
            @ApiParameter.PrettyPrint String pretty);

    @Operation(
            summary = "Get folder in directory version at time",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-get-1"),
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "204"),
                @ApiResponse(responseCode = "404")
            })
    ResponseEntity<DirectoryResponseData> getFolderInDirectoryVersionAtTime(
            String accept, String versionAtTime, String path, UUID ehrId, @ApiParameter.PrettyPrint String pretty);
}
