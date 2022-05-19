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

public class StoredQueryQualifiedName {

    String name;

    public StoredQueryQualifiedName(String name) {

        if (!name.contains("::"))
            throw new IllegalArgumentException(
                    "Qualified name is not valid (https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_query_package):"
                            + name);

        this.name = name;
    }

    public StoredQueryQualifiedName(String reverseDomainName, String semanticId, String semVer) {
        this.name = reverseDomainName + "::" + semanticId + "/" + semVer;
    }

    public String reverseDomainName() {
        return name.split("::")[0];
    }

    public String semanticId() {
        return (name.split("::")[1]).split("/")[0];
    }

    public String semVer() {
        String semVer = null;

        if (name.contains("/")) semVer = name.split("/")[1];

        return semVer;
    }

    public boolean isSetSemVer() {
        return name.contains("/");
    }

    public String toString() {
        return name;
    }
}
