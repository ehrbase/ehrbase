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

import static org.ehrbase.aql.sql.queryimpl.EntryAttributeMapper.OTHER_PARTICIPATIONS;

import java.util.ArrayList;
import java.util.List;

public class NormalizedRmAttributePath {

    protected static final String FEEDER_SYSTEM_ITEM_IDS = "feeder_system_item_ids";
    protected static final String OTHER_CONTEXT = "other_context";
    protected static final String OTHER_DETAILS = "other_details";

    protected final List<String> pathSegments;

    public NormalizedRmAttributePath(List<String> pathSegments) {
        this.pathSegments = pathSegments;
    }

    public List<String> transformStartingAt(int fromIndex) {
        List<String> resultingPaths;

        if (pathSegments.size() == 1
                && pathSegments.get(0).contains(FEEDER_SYSTEM_ITEM_IDS)
                && !pathSegments.get(0).endsWith(FEEDER_SYSTEM_ITEM_IDS))
            return new NormalizedFeederAuditAttributePath(pathSegments).transform();
        if (pathSegments.size() >= 1
                && pathSegments.get(pathSegments.size() - 1).contains(OTHER_PARTICIPATIONS))
            return new NormalizedOtherParticipations(pathSegments).transform();
        else if (!(pathSegments.stream()
                .noneMatch(segment -> segment.contains(OTHER_CONTEXT) || segment.contains(OTHER_DETAILS))))
            return new NormalizedItemStructureAttributePath(pathSegments).transformStartingAt(fromIndex);
        else {
            resultingPaths = new ArrayList<>(pathSegments);
            return resultingPaths;
        }
    }
}
