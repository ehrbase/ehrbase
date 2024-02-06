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

import com.nedap.archie.rm.ehr.EhrStatus;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.openehr.sdk.response.dto.EhrResponseData;
import org.springframework.http.ResponseEntity;

@Tag(name = "EHR")
@SuppressWarnings("java:S107")
public interface EhrApiSpecification {

    @Operation(
            summary = "Create EHR",
            externalDocs =
                    @ExternalDocumentation(
                            url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr-ehr-post"))
    ResponseEntity createEhr(
            String openehrVersion,
            String openehrAuditDetails,
            String contentType,
            String accept,
            String prefer,
            EhrStatus ehrStatus);

    @Operation(
            summary = "Create EHR with id",
            externalDocs =
                    @ExternalDocumentation(
                            url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr-ehr-put"))
    ResponseEntity<EhrResponseData> createEhrWithId(
            String openehrVersion,
            String openehrAuditDetails,
            String accept,
            String prefer,
            String ehrIdString,
            EhrStatus ehrStatus);

    @Operation(
            summary = "Get EHR summary by id",
            externalDocs =
                    @ExternalDocumentation(
                            url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr-ehr-get"))
    ResponseEntity<EhrResponseData> retrieveEhrById(String accept, String ehrIdString);

    @Operation(
            summary = "Get EHR summary by subject id",
            externalDocs =
                    @ExternalDocumentation(
                            url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr-ehr-get-1"))
    ResponseEntity<EhrResponseData> retrieveEhrBySubject(String accept, String subjectId, String subjectNamespace);
}
