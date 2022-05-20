/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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

import java.util.ArrayList;
import java.util.List;

public class NormalizedItemStructureAttributePath extends NormalizedRmAttributePath {

    public NormalizedItemStructureAttributePath(List<String> pathSegments) {
        super(pathSegments);
    }

    @Override
    public List<String> transformStartingAt(int fromIndex) {
        List<String> resultingPaths = new ArrayList<>();

        resultingPaths.addAll(pathSegments);
        for (int i = fromIndex; i < pathSegments.size(); i++) {
            String segment = pathSegments.get(i);

            resultingPaths.set(i, segment.replaceFirst("/", ""));

            if (segment.contains(OTHER_CONTEXT) || segment.contains(OTHER_DETAILS))
                break; // keep the rest of the path segments unchanged.
        }

        return resultingPaths;
    }
}
