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

import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_MARKER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NormalizedFeederAuditAttributePath extends NormalizedRmAttributePath {

    public NormalizedFeederAuditAttributePath(List<String> pathSegments) {
        super(pathSegments);
    }

    public List<String> transform() {
        List<String> resultingPaths = new ArrayList<>();

        if (pathSegments.size() == 1
                && pathSegments.get(0).contains(FEEDER_SYSTEM_ITEM_IDS)
                && !pathSegments.get(0).endsWith(FEEDER_SYSTEM_ITEM_IDS)) {
            resultingPaths.addAll(Arrays.asList(pathSegments.get(0).split(",")));
            int i = resultingPaths.indexOf(FEEDER_SYSTEM_ITEM_IDS);
            if (i >= 0) resultingPaths.add(i + 1, AQL_NODE_ITERATIVE_MARKER);
        }

        return resultingPaths;
    }
}
