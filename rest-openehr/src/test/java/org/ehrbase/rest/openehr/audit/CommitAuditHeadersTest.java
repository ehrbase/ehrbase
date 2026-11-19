/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.rest.openehr.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

class CommitAuditHeadersTest {

    /// Whitespace around commas and empty list elements (RFC 9110, 5.6.1.2), and whitespace around `=` (11.2).
    @ParameterizedTest
    @ValueSource(
            strings = {
                "system_id=\"x\"",
                "system_id = \"x\"",
                "system_id\t=\t\"x\"",
                " system_id=\"x\" ,",
                ", ,system_id=\"x\",,",
                "description.value=\"d\" , system_id=\"x\""
            })
    void acceptsOptionalWhitespaceAndEmptyElements(String line) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("openehr-audit-details", line);

        assertThat(CommitAuditHeaders.parse(headers).systemId()).isEqualTo("x");
    }

    /// Unknown keys are ignored; a misspelt key is lost without an error.
    @Test
    void ignoresUnknownKeys() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("openehr-audit-details", "commiter.name=\"Jane\",foo=\"1\",system_id=\"x\"");

        assertThat(CommitAuditHeaders.parse(headers))
                .isEqualTo(new CommitAuditHeaders(null, null, null, "x", null, null, null, null));
    }

    /// The example from ITS-REST 1.1.0, overview, "openehr-version and openehr-audit-details".
    @Test
    void parsesSpecExample() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("openehr-version", "lifecycle_state.code_string=\"532\"");
        headers.add("openehr-audit-details", "change_type.code_string=\"251\"");
        headers.add("openehr-audit-details", "description.value=\"An updated composition contribution description\"");
        headers.add(
                "openehr-audit-details",
                "committer.name=\"John Doe\",committer.external_ref.id=\"BC8132EA-8F4A-11E7-BB31-BE2E44B06B34\","
                        + "committer.external_ref.namespace=\"demographic\",committer.external_ref.type=\"PERSON\"");
        headers.add("openehr-audit-details", "system_id=\"example.openehr.systemid\"");

        assertThat(CommitAuditHeaders.parse(headers))
                .isEqualTo(new CommitAuditHeaders(
                        "532",
                        "251",
                        "An updated composition contribution description",
                        "example.openehr.systemid",
                        "John Doe",
                        "BC8132EA-8F4A-11E7-BB31-BE2E44B06B34",
                        "demographic",
                        "PERSON"));
    }

    /// The example from ITS-REST 1.0.3, overview, "openEHR-VERSION and openEHR-AUDIT_DETAILS"; 1.0.3 has no
    /// `system_id`.
    @Test
    void parsesDeprecatedSpecExample() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("openEHR-VERSION.lifecycle_state", "code_string=\"532\"");
        headers.add("openEHR-AUDIT_DETAILS.change_type", "code_string=\"251\"");
        headers.add("openEHR-AUDIT_DETAILS.description", "value=\"An updated composition contribution description\"");
        headers.add(
                "openEHR-AUDIT_DETAILS.committer",
                "name=\"John Doe\", external_ref.id=\"BC8132EA-8F4A-11E7-BB31-BE2E44B06B34\", "
                        + "external_ref.namespace=\"demographic\", external_ref.type=\"PERSON\"");

        assertThat(CommitAuditHeaders.parse(headers))
                .isEqualTo(new CommitAuditHeaders(
                        "532",
                        "251",
                        "An updated composition contribution description",
                        null,
                        "John Doe",
                        "BC8132EA-8F4A-11E7-BB31-BE2E44B06B34",
                        "demographic",
                        "PERSON"));
    }
}
