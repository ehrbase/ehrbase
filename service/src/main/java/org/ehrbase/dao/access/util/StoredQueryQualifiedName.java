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
package org.ehrbase.dao.access.util;

import org.springframework.lang.NonNull;

public final class StoredQueryQualifiedName {

    private final String reverseDomainName;
    private final String semanticId;

    private final SemVer semVer;

    public StoredQueryQualifiedName(@NonNull String qualifiedName, @NonNull SemVer version) {

        String[] nameParts = qualifiedName.split("::");

        if (nameParts.length != 2 || qualifiedName.contains("/"))
            throw new IllegalArgumentException(
                    "Qualified name is not valid (https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_query_package):"
                            + qualifiedName);

        this.reverseDomainName = nameParts[0];
        this.semanticId = nameParts[1];
        this.semVer = version;
    }

    public String reverseDomainName() {
        return reverseDomainName;
    }

    public String semanticId() {
        return semanticId;
    }

    public SemVer semVer() {
        return semVer;
    }

    public boolean hasVersion() {
        return !semVer.isNoVersion();
    }

    public String toString() {
        StringBuilder sb =
                new StringBuilder().append(reverseDomainName).append("::").append(semanticId);
        if (!semVer.isNoVersion()) {
            sb.append('/').append(semVer);
        }
        return sb.toString();
    }
}
