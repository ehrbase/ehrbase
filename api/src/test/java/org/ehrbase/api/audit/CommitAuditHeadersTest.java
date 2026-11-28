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
package org.ehrbase.api.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommitAuditHeadersTest {

    /// Request headers as the controller passes them. Names are case-insensitive and a header may repeat.
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

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
        add("openehr-audit-details", line);

        assertThat(parse().systemId()).isEqualTo("x");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "complete",
                "system_id=x",
                "system_id=\"x",
                "=\"x\"",
                "foo, system_id=\"x\"",
                "foo system_id=\"x\"",
                "system_id=\"x\" description.value=\"d\"",
                "description.value=\"\""
            })
    void rejectsMalformedLines(String line) {
        add("openehr-audit-details", line);

        assertThatThrownBy(this::parse).isInstanceOf(InvalidApiParameterException.class);
    }

    /// Unknown keys are ignored. A misspelt key is lost without an error.
    @Test
    void ignoresUnknownKeys() {
        add("openehr-audit-details", "commiter.name=\"Jane\",foo=\"1\",system_id=\"x\"");

        assertThat(parse()).isEqualTo(new CommitAuditHeaders(null, null, null, "x", null, null, null, null));
    }

    /// The example from ITS-REST 1.1.0, overview, "openehr-version and openehr-audit-details".
    @Test
    void parsesSpecExample() {
        add("openehr-version", "lifecycle_state.code_string=\"532\"");
        add("openehr-audit-details", "change_type.code_string=\"251\"");
        add("openehr-audit-details", "description.value=\"An updated composition contribution description\"");
        add(
                "openehr-audit-details",
                "committer.name=\"John Doe\",committer.external_ref.id=\"BC8132EA-8F4A-11E7-BB31-BE2E44B06B34\","
                        + "committer.external_ref.namespace=\"demographic\",committer.external_ref.type=\"PERSON\"");
        add("openehr-audit-details", "system_id=\"example.openehr.systemid\"");

        assertThat(parse())
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

    /// The example from ITS-REST 1.0.3, which has no `system_id`.
    @Test
    void parsesDeprecatedSpecExample() {
        add("openEHR-VERSION.lifecycle_state", "code_string=\"532\"");
        add("openEHR-AUDIT_DETAILS.change_type", "code_string=\"251\"");
        add("openEHR-AUDIT_DETAILS.description", "value=\"An updated composition contribution description\"");
        add(
                "openEHR-AUDIT_DETAILS.committer",
                "name=\"John Doe\", external_ref.id=\"BC8132EA-8F4A-11E7-BB31-BE2E44B06B34\", "
                        + "external_ref.namespace=\"demographic\", external_ref.type=\"PERSON\"");

        assertThat(parse())
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

    private void add(String name, String line) {
        headers.computeIfAbsent(name, n -> new ArrayList<>()).add(line);
    }

    private CommitAuditHeaders parse() {
        return CommitAuditHeaders.parse(name -> headers.getOrDefault(name, List.of()));
    }
}
