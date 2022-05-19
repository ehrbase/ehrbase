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
package org.ehrbase.aql.sql.queryimpl;

import java.util.List;
import org.ehrbase.ehr.util.LocatableHelper;

public class VariableAqlPath {

    private static final String JSON_PATH_QUALIFIER_MATCHER = "value|name|time";

    String path;

    public VariableAqlPath(String path) {
        this.path = path;
    }

    public String getSuffix() {
        List<String> segments = LocatableHelper.dividePathIntoSegments(path);
        return segments.get(segments.size() - 1);
    }

    public String getInfix() {
        List<String> segments = LocatableHelper.dividePathIntoSegments(path);
        return String.join("/", segments.subList(0, segments.size() - 1));
    }

    public boolean isPartialAqlDataValuePath() {
        return getSuffix().matches(JSON_PATH_QUALIFIER_MATCHER);
    }
}
