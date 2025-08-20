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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.rest.openehr.specification.ApiParameter.PrettyPrint;
import org.springframework.http.ResponseEntity;

@Tag(name = "COMPOSITION")
@SuppressWarnings("java:S107")
public interface CompositionApiSpecification {

    @Operation(
            summary = "Create composition",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-composition-post"))
    ResponseEntity createComposition(
            String openehrVersion,
            String openehrAuditDetails,
            String contentType,
            String accept,
            String prefer,
            String ehrIdString,
            String templateId,
            @Parameter(
                            description = "Composition format",
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {"JSON", "XML", "STRUCTURED", "FLAT"}))
                    String format,
            @PrettyPrint String pretty,
            String composition);

    @Operation(
            summary = "Update composition",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-composition-put"))
    ResponseEntity updateComposition(
            String openehrVersion,
            String openehrAuditDetails,
            String contentType,
            String accept,
            String prefer,
            String ifMatch,
            String ehrIdString,
            String versionedObjectUidString,
            String templateId,
            @Parameter(
                            description = "Composition format",
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {"JSON", "XML", "STRUCTURED", "FLAT"}))
                    String format,
            @PrettyPrint String pretty,
            String composition);

    @Operation(
            summary = "Delete composition",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-composition-delete"))
    ResponseEntity deleteComposition(
            String openehrVersion, String openehrAuditDetails, String ehrIdString, String precedingVersionUid);

    @Operation(
            summary = "Get composition at time",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-composition-get"))
    ResponseEntity getComposition(
            String accept,
            String ehrIdString,
            String versionedObjectUid,
            @Parameter(
                            description = "Composition format",
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {"JSON", "XML", "STRUCTURED", "FLAT"}))
                    String format,
            @PrettyPrint String pretty,
            String versionAtTime);
}
