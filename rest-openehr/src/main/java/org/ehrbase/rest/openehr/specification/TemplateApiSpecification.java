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

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import org.ehrbase.openehr.sdk.response.dto.TemplateResponseData;
import org.springframework.http.ResponseEntity;

@SuppressWarnings("java:S107")
public interface TemplateApiSpecification {

    @Operation(
            tags = "ADL 1.4 TEMPLATE",
            summary = "Upload a template",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-adl-1.4-template-post"))
    ResponseEntity createTemplateClassic(
            String openehrVersion,
            String openehrAuditDetails,
            String contentType,
            String accept,
            String prefer,
            String template);

    @Operation(
            tags = "ADL 1.4 TEMPLATE",
            summary = "List templates",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-adl-1.4-template-get"))
    ResponseEntity getTemplatesClassic(String openehrVersion, String openehrAuditDetails, String accept);

    @Operation(
            tags = "ADL 1.4 TEMPLATE",
            summary = "Get template",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-adl-1.4-template-get-1"))
    ResponseEntity getTemplateClassic(
            String openehrVersion, String openehrAuditDetails, String accept, String templateId);

    @Operation(tags = "ADL 1.4 TEMPLATE", summary = "Get an example composition for the specified template")
    ResponseEntity<String> getTemplateExample(String accept, String templateId);

    @Operation(
            tags = "ADL 2 TEMPLATE",
            summary = "Upload a template",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-adl-2-template-post"))
    ResponseEntity<TemplateResponseData> createTemplateNew(
            String openehrVersion,
            String openehrAuditDetails,
            String contentType,
            String accept,
            String prefer,
            String version,
            String template);

    @Operation(
            tags = "ADL 2 TEMPLATE",
            summary = "Get template",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-stored-query-get-1"))
    ResponseEntity<TemplateResponseData> getTemplateNew(
            String openehrVersion, String openehrAuditDetails, String accept, String templateId, String versionPattern);
}
