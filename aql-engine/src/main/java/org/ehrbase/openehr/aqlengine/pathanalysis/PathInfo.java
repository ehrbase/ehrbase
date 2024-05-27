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
package org.ehrbase.openehr.aqlengine.pathanalysis;

import static org.ehrbase.openehr.aqlengine.AqlQueryUtils.streamWhereConditions;

import com.nedap.archie.rm.datavalues.quantity.DvOrdered;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.RMTypeInfo;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.openehr.aqlengine.AqlQueryUtils;
import org.ehrbase.openehr.aqlengine.pathanalysis.ANode.NodeCategory;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathAnalysis.AttInfo;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathCohesionAnalysis.PathCohesionTreeNode;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.orderby.OrderByExpression;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;

/**
 * Provides an analysis of a Path Cohesion Tree
 */
public final class PathInfo {

    private static final Set<String> DV_ORDERED_TYPES =
            ArchieRMInfoLookup.getInstance().getTypeInfo(DvOrdered.class).getAllDescendantClasses().stream()
                    .map(RMTypeInfo::getRmName)
                    .collect(Collectors.toSet());

    /**
     * The number of (structure) children and if data is retrieved determines how a path node needs to be joined.
     *
     * <table>
     *     <tr><th>ROOT</th><th>noChild</th><th>oneChild</th><th>multipleChildren</th></tr>
     *     <tr><th>data</th><td>ROOT</td><td>ROOT</td><td>ROOT</td></tr>
     *     <tr><th>no data</th><td>∅</td><td>ROOT</td><td>ROOT</td></tr>
     *     <tr></tr>
     *     <tr><th>sub-node</th><th>noChild</th><th>oneChild</th><th>multipleChildren</th></tr>
     *     <tr><th>data</th><td>DATA</td><td>DATA</td><td>DATA</td></tr>
     *     <tr><th>no data</th><td>∅</td><td>INTERNAL_SINGLE_CHILD</td><td>INTERNAL_FORK</td></tr>
     * </table>
     */
    public enum JoinMode {
        /**
         * Root node stemming from the FROM clause.
         * Is already "left-joined".
         * Hence all children need to be left-joined.
         */
        ROOT,
        /**
         * Node that contributes data to the result;
         * The number of children is secondary.
         * The children need to be "left-joined" (P(⟕C<sub>n</sub>)).
         */
        DATA,
        /**
         * Internal node with just a single child.
         * It does not directly contribute data to the result.
         * It must only result in tuples when the child does.
         * It can be joined with the child (P⋈C), or may, under some conditions, be omitted.
         */
        INTERNAL_SINGLE_CHILD,
        /**
         * Internal node with multiple children.
         * It does not directly contribute data to the result.
         * It must only result in tuples when at least one of the children does.
         * <br>
         * This may be considered an inner join of the parent with the result of outer joining all children:
         * P⋈(⟗<sup>i=1</sup><sub>n</sub>(C<sub>i</sub>))
         */
        INTERNAL_FORK
    }

    public record NodeInfo(
            NodeCategory category,
            Set<String> rmTypes,
            List<PathNode> pathFromRoot,
            boolean multipleValued,
            Set<String> dvOrderedTypes) {}

    private final PathCohesionTreeNode cohesionTreeRoot;
    private final Map<IdentifiedPath, Pair<ANode, Map<ANode, Map<String, AttInfo>>>> pathAttributeInfo;

    private final Map<PathCohesionTreeNode, NodeInfo> nodeTypeInfo;
    private final Map<IdentifiedPath, Set<QueryClause>> pathToQueryClause;

    public PathInfo(PathCohesionTreeNode cohesionTreeRoot, Map<IdentifiedPath, Set<QueryClause>> pathToQueryClause) {
        this.cohesionTreeRoot = cohesionTreeRoot;
        this.pathAttributeInfo = cohesionTreeRoot.getPaths().stream().collect(Collectors.toMap(ip -> ip, ip -> {
            AbstractContainmentExpression root = ip.getRoot();
            ANode analyzed = PathAnalysis.analyzeAqlPathTypes(
                    root instanceof ContainmentClassExpression cce ? cce.getType() : RmConstants.ORIGINAL_VERSION,
                    ip.getRootPredicate(),
                    root.getPredicates(),
                    ip.getPath(),
                    null);
            if (analyzed.getCandidateTypes().isEmpty()) {
                throw new IllegalArgumentException("Path %s is not valid".formatted(ip.render()));
            }
            return Pair.of(analyzed, PathAnalysis.createAttributeInfos(analyzed));
        }));

        this.nodeTypeInfo = fillNodeTypeInfo(cohesionTreeRoot, -1, new HashMap<>());
        this.pathToQueryClause = pathToQueryClause;
    }

    private Map<PathCohesionTreeNode, NodeInfo> fillNodeTypeInfo(
            PathCohesionTreeNode currentNode, int level, Map<PathCohesionTreeNode, NodeInfo> nodeTypeInfo) {
        NodeInfo nodeInfo = currentNode.getPaths().stream()
                .map(ip -> nodeTypeInfoForPathAtLevel(ip, level))
                .reduce((a, b) -> new NodeInfo(
                        mergeNodeCategories(a.category(), b.category()),
                        mutableUnion(a.rmTypes(), b.rmTypes()),
                        a.pathFromRoot(),
                        a.multipleValued() || b.multipleValued(),
                        mutableUnion(a.dvOrderedTypes(), b.dvOrderedTypes())))
                .orElseThrow();
        nodeTypeInfo.put(currentNode, nodeInfo);
        currentNode.getChildren().forEach(pcn -> fillNodeTypeInfo(pcn, level + 1, nodeTypeInfo));
        return nodeTypeInfo;
    }

    private static <T> Set<T> mutableUnion(Set<T> a, Set<T> b) {
        return new HashSet<>(SetUtils.union(a, b));
    }

    public static NodeCategory mergeNodeCategories(NodeCategory a, NodeCategory b) {
        if (a == b) {
            return a;
        }

        // Make sure c0 < c1;
        boolean sorted = a.ordinal() < b.ordinal();
        final NodeCategory c0 = sorted ? a : b;
        final NodeCategory c1 = sorted ? b : a;

        // takes advantage of c0 < c1
        return switch (c0) {
            case STRUCTURE, STRUCTURE_INTERMEDIATE -> throw new IllegalArgumentException(
                    "Incompatible node types: %s, %s".formatted(a, b));
            case RM_TYPE, FOUNDATION -> NodeCategory.FOUNDATION_EXTENDED;
            case FOUNDATION_EXTENDED -> throw new IllegalArgumentException(
                    "Inconsistent node types: %s, %s".formatted(a, b));
        };
    }

    public static List<AqlObjectPath.PathNode> pathNodes(AqlObjectPath path) {
        return Optional.ofNullable(path).map(AqlObjectPath::getPathNodes).orElseGet(List::of);
    }

    private NodeInfo nodeTypeInfoForPathAtLevel(IdentifiedPath ip, int level) {
        Pair<ANode, Map<ANode, Map<String, AttInfo>>> aNodeWithInfo = pathAttributeInfo.get(ip);
        ANode aNode = aNodeWithInfo.getLeft();
        Map<ANode, Map<String, AttInfo>> attributeInfos = aNodeWithInfo.getRight();
        List<AqlObjectPath.PathNode> pathNodes = pathNodes(ip.getPath());
        String attribute = null;
        AttInfo attInfo = null;
        for (int i = 0; i <= level; i++) {
            attribute = pathNodes.get(i).getAttribute();
            attInfo = attributeInfos.get(aNode).get(attribute);
            aNode = aNode.getAttribute(attribute);
        }

        NodeCategory nodeCategory = aNode.getCategories().stream()
                .reduce(PathInfo::mergeNodeCategories)
                .orElseThrow();

        return new NodeInfo(
                nodeCategory,
                Optional.ofNullable(attInfo).map(AttInfo::targetTypes).orElse(aNode.getCandidateTypes()),
                level < 0
                        ? List.of()
                        : Collections.unmodifiableList(pathNodes(ip.getPath()).subList(0, level + 1)),
                Optional.ofNullable(attInfo).map(AttInfo::multipleValued).orElse(false),
                Optional.ofNullable(attInfo)
                        .map(AttInfo::targetTypes)
                        .<Set<String>>map(t -> SetUtils.intersection(t, DV_ORDERED_TYPES))
                        .orElse(Collections.emptySet()));
    }

    private enum QueryClause {
        SELECT,
        WHERE,
        ORDER_BY
    }

    public static Map<ContainsWrapper, PathInfo> createPathInfos(
            AqlQuery aqlQuery, Map<AbstractContainmentExpression, ContainsWrapper> containsDescs) {
        Map<AbstractContainmentExpression, PathCohesionTreeNode> pathCohesion =
                PathCohesionAnalysis.analyzePathCohesion(aqlQuery);

        Map<IdentifiedPath, Set<QueryClause>> pathToQueryClause = Collections.unmodifiableMap(Stream.of(
                        aqlQuery.getSelect().getStatement().stream()
                                .flatMap(AqlQueryUtils::allIdentifiedPaths)
                                .map(p -> Pair.of(p, QueryClause.SELECT)),
                        streamWhereConditions(aqlQuery.getWhere())
                                .flatMap(AqlQueryUtils::allIdentifiedPaths)
                                .map(p -> Pair.of(p, QueryClause.WHERE)),
                        Optional.of(aqlQuery).map(AqlQuery::getOrderBy).stream()
                                .flatMap(Collection::stream)
                                .map(OrderByExpression::getStatement)
                                .map(p -> Pair.of(p, QueryClause.ORDER_BY)))
                .flatMap(s -> s)
                .collect(Collectors.groupingBy(
                        Pair::getLeft, LinkedHashMap::new, Collectors.mapping(Pair::getRight, Collectors.toUnmodifiableSet()))));

        return containsDescs.entrySet().stream()
                .filter(e -> pathCohesion.containsKey(e.getKey()))
                .filter(e -> !(e.getKey() instanceof ContainmentClassExpression cce
                        && RmConstants.EHR.equals(cce.getType())))
                .collect(Collectors.toMap(
                        Entry::getValue,
                        e -> new PathInfo(pathCohesion.get(e.getKey()), pathToQueryClause),
                        (a, b) -> null,
                        LinkedHashMap::new));
    }

    public PathCohesionTreeNode getCohesionTreeRoot() {
        return cohesionTreeRoot;
    }

    public NodeCategory getNodeCategory(PathCohesionTreeNode node) {
        return Optional.of(node).map(nodeTypeInfo::get).map(NodeInfo::category).orElseThrow();
    }

    public Set<String> getTargetTypes(PathCohesionTreeNode node) {
        return Optional.of(node).map(nodeTypeInfo::get).map(NodeInfo::rmTypes).orElseThrow();
    }

    public Set<String> getDvOrderedTypes(PathCohesionTreeNode node) {
        return Optional.of(node)
                .map(nodeTypeInfo::get)
                .map(NodeInfo::dvOrderedTypes)
                .orElseThrow();
    }

    public boolean isUsedInSelect(PathCohesionTreeNode node) {
        return Optional.of(node).stream()
                .map(PathCohesionTreeNode::getPathsEndingAtNode)
                .flatMap(List::stream)
                .map(pathToQueryClause::get)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .anyMatch(QueryClause.SELECT::equals);
    }

    public boolean isUsedInWhereOrOrderBy(PathCohesionTreeNode node) {
        return Optional.of(node).stream()
                .map(PathCohesionTreeNode::getPathsEndingAtNode)
                .flatMap(List::stream)
                .map(pathToQueryClause::get)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .anyMatch(c -> QueryClause.WHERE.equals(c) || QueryClause.ORDER_BY.equals(c));
    }

    public boolean isMultipleValued(PathCohesionTreeNode node) {
        return Optional.of(node)
                .map(nodeTypeInfo::get)
                .map(NodeInfo::multipleValued)
                .orElseThrow();
    }

    public List<PathNode> getPathToNode(PathCohesionTreeNode node) {
        return Optional.of(node)
                .map(nodeTypeInfo::get)
                .map(NodeInfo::pathFromRoot)
                .orElseThrow();
    }

    private static boolean isData(NodeCategory nc) {
        return switch (nc) {
            case STRUCTURE, STRUCTURE_INTERMEDIATE -> false;
            case RM_TYPE, FOUNDATION, FOUNDATION_EXTENDED -> true;
        };
    }

    public JoinMode joinMode(PathCohesionTreeNode node) {
        if (node.isRoot()) {
            return JoinMode.ROOT;
        }
        boolean hasData = !node.getPathsEndingAtNode().isEmpty()
                || node.getChildren().stream().anyMatch(c -> isData(getNodeCategory(c)));
        if (hasData) {
            return JoinMode.DATA;
        }
        int structureChildCount = (int) node.getChildren().stream()
                .filter(c -> !isData(getNodeCategory(c)))
                .count();
        return switch (structureChildCount) {
            case 0 -> throw new IllegalArgumentException("Internal node without children: %s".formatted(node));
            case 1 -> JoinMode.INTERNAL_SINGLE_CHILD;
            default -> JoinMode.INTERNAL_FORK;
        };
    }
}
