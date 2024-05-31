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
import java.util.List;
import org.ehrbase.openehr.sdk.response.dto.TemplateResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.springframework.http.ResponseEntity;

@SuppressWarnings("java:S107")
public interface TemplateApiSpecification {

    @Operation(
            tags = "ADL 1.4 TEMPLATE",
            summary = "Upload a template",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definition.html#tag/ADL1.4/operation/definition_template_adl1.4_upload"))
    ResponseEntity<String> createTemplateClassic(
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
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definition.html#tag/ADL1.4/operation/definition_template_adl1.4_list"))
    ResponseEntity<List<TemplateMetaDataDto>> getTemplatesClassic(
            String openehrVersion, String openehrAuditDetails, String accept);

    @Operation(
            tags = "ADL 1.4 TEMPLATE",
            summary = "Get template",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definition.html#tag/ADL1.4/operation/definition_template_adl1.4_get"))
    ResponseEntity<Object> getTemplateClassic(
            String openehrVersion, String openehrAuditDetails, String accept, String templateId);

    @Operation(tags = "ADL 1.4 TEMPLATE", summary = "Get an example composition for the specified template")
    ResponseEntity<String> getTemplateExample(
            String accept,
            String templateId,
            @Parameter(
                            description = "Composition format",
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {"JSON", "XML", "STRUCTURED", "FLAT"}))
                    String format);

    @Operation(
            tags = "ADL 2 TEMPLATE",
            summary = "Upload a template",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definition.html#tag/ADL2/operation/definition_template_adl2_upload"))
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
            summary = "List templates",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definition.html#tag/ADL2/operation/definition_template_adl2_get"))
    ResponseEntity<TemplateResponseData> getTemplatesNew(
            String openehrVersion, String openehrAuditDetails, String accept);

    @Operation(
            tags = "ADL 2 TEMPLATE",
            summary = "Get template",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-stored-query-get-1"))
    ResponseEntity<TemplateResponseData> getTemplateNew(
            String openehrVersion, String openehrAuditDetails, String accept, String templateId, String versionPattern);

    @Operation(
            tags = "TEMPLATE",
            summary = "Deprecated since 2.2.0 and marked for removal",
            description =
                    "Replaced by [/rest/openehr/v1/definition/template/adl1.4/{template_id}](./index.html?urls.primaryName=1.%20openEHR%20API#/ADL%201.4%20TEMPLATE/getTemplateClassic)",
            deprecated = true)
    @Deprecated(since = "2.2.0", forRemoval = true)
    ResponseEntity<WebTemplate> getWebTemplate(String accept, String templateId);
}
