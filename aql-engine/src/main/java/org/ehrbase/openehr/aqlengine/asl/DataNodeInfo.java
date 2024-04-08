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
package org.ehrbase.openehr.aqlengine.asl;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.ehrbase.openehr.aqlengine.asl.DataNodeInfo.ExtractedColumnDataNodeInfo;
import org.ehrbase.openehr.aqlengine.asl.DataNodeInfo.JsonRmDataNodeInfo;
import org.ehrbase.openehr.aqlengine.asl.DataNodeInfo.StructureRmDataNodeInfo;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathCohesionAnalysis.PathCohesionTreeNode;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;

sealed interface DataNodeInfo permits ExtractedColumnDataNodeInfo, JsonRmDataNodeInfo, StructureRmDataNodeInfo {
    PathCohesionTreeNode node();

    OwnerProviderTuple parent();

    AslQuery providerSubQuery();

    record JsonRmDataNodeInfo(
            @Override PathCohesionTreeNode node,
            @Override OwnerProviderTuple parent,
            AslEncapsulatingQuery parentJoin,
            @Override AslQuery providerSubQuery,
            List<PathNode> pathInJson,
            boolean multipleValued,
            Stream<DataNodeInfo> dependentPathDataNodes,
            Set<String> dvOrderedTypes,
            Class<?> type)
            implements DataNodeInfo {}

    record ExtractedColumnDataNodeInfo(
            @Override PathCohesionTreeNode node,
            @Override OwnerProviderTuple parent,
            @Override AslQuery providerSubQuery,
            AslExtractedColumn extractedColumn)
            implements DataNodeInfo {}

    record StructureRmDataNodeInfo(
            @Override PathCohesionTreeNode node,
            @Override OwnerProviderTuple parent,
            AslEncapsulatingQuery parentJoin,
            @Override AslQuery providerSubQuery)
            implements DataNodeInfo {}
}
