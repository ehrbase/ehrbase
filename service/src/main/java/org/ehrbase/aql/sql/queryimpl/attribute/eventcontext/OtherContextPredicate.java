/*
 * Copyright 2020-2022 vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.aql.sql.queryimpl.attribute.eventcontext;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Christian Chevalley
 * @author Renaud Subiger
 * @since 1.0
 */
public class OtherContextPredicate {

    private static final String PREDICATE = "other_context[";

    private final String path;

    public OtherContextPredicate(String path) {
        this.path = path;
    }

    public String adjustForQuery() {
        if (!path.contains(PREDICATE)) {
            return path;
        }

        var pathSegments = Arrays.stream(path.split("/"))
                .map(pathSegment -> {
                    if (pathSegment.startsWith(PREDICATE)) {
                        return "other_context";
                    }
                    return pathSegment;
                })
                .collect(Collectors.toList());

        return String.join("/", pathSegments);
    }
}
