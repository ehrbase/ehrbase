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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.jspecify.annotations.Nullable;

/// Values from the `openehr-version` and `openehr-audit-version` headers. Field is null when not sent.
public record CommitAuditHeaders(
        @Nullable String lifecycleState,
        @Nullable String changeType,
        @Nullable String description,
        @Nullable String systemId,
        @Nullable String committerName,
        @Nullable String committerExternalRefId,
        @Nullable String committerExternalRefNamespace,
        @Nullable String committerExternalRefType) {

    /// The headers of a direct commit, bound around its service call.
    /// Commits without it keep the server defaults.
    public static final ScopedValue<CommitAuditHeaders> SENT = ScopedValue.newInstance();

    /// Supported headers for both REST 1.1.0 and 1.0.3 specs
    private static final List<Map.Entry<String, String>> HEADERS = List.of(
            Map.entry("openehr-version", ""),
            Map.entry("openehr-audit-details", ""),
            Map.entry("openEHR-VERSION.lifecycle_state", "lifecycle_state."),
            Map.entry("openEHR-AUDIT_DETAILS.change_type", "change_type."),
            Map.entry("openEHR-AUDIT_DETAILS.description", "description."),
            Map.entry("openEHR-AUDIT_DETAILS.committer", "committer."));

    /// Reads the supported headers; pairs with other keys are ignored, but still have to be well-formed,
    /// non-empty and not repeated.
    ///
    /// @param headerValues the lines of a header by name, e.g. `HttpHeaders::getOrEmpty`
    /// @throws InvalidApiParameterException at the first malformed line, empty value or duplicate key
    public static CommitAuditHeaders parse(Function<String, List<String>> headerValues) {
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<String, String> header : HEADERS) {
            for (String line : headerValues.apply(header.getKey())) {
                read(header.getKey(), header.getValue(), line, values);
            }
        }
        return new CommitAuditHeaders(
                values.get("lifecycle_state.code_string"),
                values.get("change_type.code_string"),
                values.get("description.value"),
                values.get("system_id"),
                values.get("committer.name"),
                values.get("committer.external_ref.id"),
                values.get("committer.external_ref.namespace"),
                values.get("committer.external_ref.type"));
    }

    /// Reads one line of `key="value"` pairs separated by commas into `values`, with `prefix` in front of each key.
    /// Whitespace around `=` is accepted.
    private static void read(String header, String prefix, String line, Map<String, String> values) {
        int pos = skipSeparators(line, 0);
        while (pos < line.length()) {
            int equals = line.indexOf('=', pos);
            int openingQuote = equals < 0 ? -1 : skipWhitespace(line, equals + 1);
            int closingQuote =
                    openingQuote < 0 || !line.startsWith("\"", openingQuote) ? -1 : line.indexOf('"', openingQuote + 1);
            if (closingQuote < 0) {
                throw new InvalidApiParameterException("%s: expected key=\"value\" list".formatted(header));
            }
            String sentKey = line.substring(pos, equals).strip();
            String value = line.substring(openingQuote + 1, closingQuote);
            if (!isKey(sentKey)) {
                throw new InvalidApiParameterException("%s: expected key=\"value\" list".formatted(header));
            }
            if (value.isEmpty()) {
                throw new InvalidApiParameterException("%s: empty value for [%s]".formatted(header, sentKey));
            }
            if (values.putIfAbsent(prefix + sentKey, value) != null) {
                throw new InvalidApiParameterException("%s: duplicate key [%s]".formatted(header, sentKey));
            }
            pos = skipWhitespace(line, closingQuote + 1);
            if (pos < line.length() && line.charAt(pos) != ',') {
                throw new InvalidApiParameterException("%s: expected key=\"value\" list".formatted(header));
            }
            pos = skipSeparators(line, pos);
        }
    }

    /// A key is not empty and has no whitespace, comma or quote.
    private static boolean isKey(String text) {
        return !text.isEmpty() && text.chars().noneMatch(c -> c == ',' || c == '"' || c == ' ' || c == '\t');
    }

    /// The index of the first character from `from` on that isn't a space or tab.
    private static int skipWhitespace(String line, int from) {
        int pos = from;
        while (pos < line.length() && (line.charAt(pos) == ' ' || line.charAt(pos) == '\t')) {
            pos++;
        }
        return pos;
    }

    /// The index of the first character from `from` on that isn't a comma, space or tab: HTTP lists allow
    /// whitespace around commas and empty elements, e.g. a trailing comma
    private static int skipSeparators(String line, int from) {
        int pos = from;
        while (pos < line.length()
                && (line.charAt(pos) == ',' || line.charAt(pos) == ' ' || line.charAt(pos) == '\t')) {
            pos++;
        }
        return pos;
    }
}
