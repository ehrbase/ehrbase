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
package org.ehrbase.util;

import java.util.Optional;

/**
 * Represents a stored <code>AQL</code> Query, as described in
 * <a href="https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_query_service">openEHR Platform Service Model: 8. Query Service</a>
 * with format
 * <pre>
 * reverse-domain-name '::' semantic-id [ '/' version ]
 *
 * org.example.departmentx.test::diabetes-patient-overview/1.0.2
 * </pre>
 *
 * @param reverseDomainName reverse domain name like <code> rg.example.departmentx.test::diabetes-patient-overview</code>
 * @param semanticId semantic identifier of the query <code>prod</code>
 * @param semVer <a href="https://semver.org">semantic version</a> of the query like <code>1.2.3</code>
 *
 * @see <a href="https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_query_service">openEHR Platform Service Model: 8. Query Service</a>
 * @see <a href="https://semver.org">semver.org</a>
 */
public record StoredQueryQualifiedName(String reverseDomainName, String semanticId, SemVer semVer) {

    public static StoredQueryQualifiedName create(String qualifiedName, SemVer version) {

        String[] nameParts = qualifiedName.split("::");

        if (nameParts.length != 2 || qualifiedName.contains("/"))
            throw new IllegalArgumentException(
                    "Qualified name is not valid (https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_query_service):"
                            + qualifiedName);

        return new StoredQueryQualifiedName(
                nameParts[0], nameParts[1], Optional.ofNullable(version).orElse(SemVer.NO_VERSION));
    }

    /**
     * Returns the name part of the qualified query name:
     * <pre>
     * reverse-domain-name '::' semantic-id [ '/' version ]
     *
     * org.example.departmentx.test::diabetes-patient-overview/1.0.2
     * </pre>
     *
     * @return name part concatenated <code>{@link #reverseDomainName}::{@link #semanticId}</code>
     */
    public String toName() {
        return reverseDomainName + "::" + semanticId;
    }

    /**
     * Returns the fully qualified query name
     * <pre>
     * reverse-domain-name '::' semantic-id
     *
     * org.example.departmentx.test::diabetes-patient-overview
     * </pre>
     *
     * @return qualifiedName part concatenated <code>{@link #reverseDomainName}::{@link #semanticId}/{@link #semVer}</code>
     */
    public String toQualifiedNameString() {
        StringBuilder sb =
                new StringBuilder().append(reverseDomainName).append("::").append(semanticId);
        if (!semVer.isNoVersion()) {
            sb.append('/').append(semVer);
        }
        return sb.toString();
    }

    /**
     * Uses {@link #toQualifiedNameString()}
     *
     * @return qualifiedName from {@link #toQualifiedNameString()}
     */
    public String toString() {
        return toQualifiedNameString();
    }
}
