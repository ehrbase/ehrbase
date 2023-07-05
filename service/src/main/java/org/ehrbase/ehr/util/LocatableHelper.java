/*
 * Copyright (c) 2015-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.ehr.util;

import java.util.List;
import java.util.stream.Collectors;
import org.ehrbase.openehr.sdk.aql.webtemplatepath.AqlPath;

/**
 * Utility class for path manipulation.
 *
 * @author Christian Chevalley
 * @author Renaud Subiger
 * @since 1.0
 */
public class LocatableHelper {

    private LocatableHelper() {}

    public static List<String> dividePathIntoSegments(String path) {
        var aqlPath = AqlPath.parse(path);
        return aqlPath.getNodes().stream()
                .map(aqlNode -> {
                    StringBuilder sb = new StringBuilder();
                    aqlNode.appendFormat(sb, AqlPath.OtherPredicatesFormat.SHORTED);
                    return sb.toString();
                })
                .collect(Collectors.toList());
    }
}
