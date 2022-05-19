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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "CONTRIBUTION")
@SuppressWarnings("java:S107")
public interface ContributionApiSpecification {

    @Operation(
            summary = "Create contribution",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#contribution-contribution-post"))
    ResponseEntity createContribution(
            String openehrVersion,
            String openehrAuditDetails,
            String contentType,
            String accept,
            String prefer,
            String ehrIdString,
            String contribution);

    @Operation(
            summary = "Get contribution by id",
            externalDocs =
                    @ExternalDocumentation(
                            url =
                                    "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#contribution-contribution-get"))
    ResponseEntity getContribution(
            String openehrVersion,
            String openehrAuditDetails,
            String accept,
            String ehrIdString,
            String contributionUidString);
}
