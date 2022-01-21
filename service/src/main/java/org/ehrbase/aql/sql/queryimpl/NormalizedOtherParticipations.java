/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.queryimpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ehrbase.aql.sql.queryimpl.EntryAttributeMapper.OTHER_PARTICIPATIONS;
import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_MARKER;

public class NormalizedOtherParticipations extends NormalizedRmAttributePath{

    public NormalizedOtherParticipations(List<String> pathSegments) {
        super(pathSegments);
    }

    public List<String> transform() {
        List<String> resultingPaths = new ArrayList<>();

        if (pathSegments.size() >= 1 && pathSegments.get(pathSegments.size() - 1).contains(OTHER_PARTICIPATIONS)) {
            String otherParticipationsField = pathSegments.get(pathSegments.size() - 1);
            List<String> otherParticipations = new ArrayList<>(List.of(otherParticipationsField.split(",")));
            resultingPaths.addAll(pathSegments.subList(0, pathSegments.size() - 2));
            resultingPaths.addAll(otherParticipations);
        }

        return resultingPaths;
    }

}
