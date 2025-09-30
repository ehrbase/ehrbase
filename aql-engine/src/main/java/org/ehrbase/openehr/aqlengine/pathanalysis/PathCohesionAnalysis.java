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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ehrbase.openehr.aqlengine.aql.AqlQueryUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentVersionExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.PathPredicateOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPathUtil;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.util.TreeNode;

/**
 *
 *  <h2>Cohesion of attribute paths</h2>
 *
 * Given an object type that has several attributes
 * and a query that selects those attributes,
 * the result list must not contain a combination of values from different ("not same") objects.
 *
 * <h2>Constrained ATTRIBUTEs</h32>
 *
 * <p>In Archetypes and Templates the content of attributes of type ATTRIBUTE can be constrained.
 * Effectively each constraint, identified by its node_id, constitutes a sub-attribute of the base attribute.
 *
 * ARCHETYPE_SLOT or ARCHETYPE_ROOT constraints also possess a node_id, but in the object representation
 * the <a href="https://specifications.openehr.org/releases/RM/Release-1.1.0/common.html#_the_locatable_class">archetype_node_id</a>
 * features the archetype id, instead.
 * Since multiple ARCHETYPE_SLOT and ARCHETYPE_ROOT constraints may allow the same archetype_id,
 * it may not be sufficient to identify a specific sub-attribute. In this case, name/value can be used as additional identification criterion.
 * Other predicates are merely acting as filters.
 * </p>
 * <p>If multiple paths target the same base attribute, it must be determined at which resolution attributes are indicated:
 *
 * <ol>
 *    <li>attribute without identifying predicates: If present, a base attribute is indicated: Identifying predicates in other paths at as filters</li>
 *    <li>name/value: if all paths (only) have name/value predicates, they induce sub-attributes. Otherwise, a base attribute is indicated.</li>
 *    <li>node_id: name/value acts as filter</li>
 *    <li>archetype_id: if a path with a certain archetype_id has no name/value predicate, name/value of other paths with the same archetype_id act as filter</li>
 *    <li>archetype_id + name/value: identifies a sub-attribute</li>
 * </ol>
 * </p>
 */
public final class PathCohesionAnalysis {

    private PathCohesionAnalysis() {
        // NOOP
    }

    /**
     * For each containment expression that is referenced in the query, the paths are analyzed and a tree of its attributes is returned.
     *
     * @param query
     * @return
     */
    public static Map<AbstractContainmentExpression, PathCohesionTreeNode> analyzePathCohesion(AqlQuery query) {

        Map<AbstractContainmentExpression, List<IdentifiedPath>> roots = AqlQueryUtils.allIdentifiedPaths(query)
                .distinct()
                .collect(Collectors.groupingBy(IdentifiedPath::getRoot, IdentityHashMap::new, Collectors.toList()));

        return roots.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            PathNode rootNode = createRootNode(e);

                            PathCohesionTreeNode joinTree = PathCohesionTreeNode.root(rootNode, e.getValue());
                            fillJoinTree(joinTree, 0);
                            return joinTree;
                        },
                        (a, b) -> {
                            throw new UnsupportedOperationException();
                        },
                        IdentityHashMap::new));
    }

    private static PathNode createRootNode(Map.Entry<AbstractContainmentExpression, List<IdentifiedPath>> e) {
        String rootType;
        AbstractContainmentExpression root = e.getKey();
        List<AndOperatorPredicate> rootPredicates;
        if (root instanceof ContainmentVersionExpression cv) {
            rootType = "VERSION";
            rootPredicates = new ArrayList<>();
        } else if (root instanceof ContainmentClassExpression cc) {
            rootType = cc.getType();
            rootPredicates = Optional.of(cc)
                    .map(ContainmentClassExpression::getPredicates)
                    .orElseGet(ArrayList::new);
        } else {
            throw new IllegalArgumentException("Unsupported type: %s".formatted(root));
        }

        /*
         * Note: IdentifiedPath.rootPredicates does not produce attributes, and merely acts as filter.
         * Therefore, it needs not be merged (and no forest data structure is needed).
         */
        var attributeType = AttributeType.getAttributeType(rootPredicates);
        attributeType.cleanupPredicates(rootPredicates);

        return new PathNode(rootType, rootPredicates);
    }

    private static void fillJoinTree(PathCohesionTreeNode node, int level) {
        Map<String, List<IdentifiedPath>> baseAttributes = node.getPaths().stream()
                .filter(o -> PathInfo.pathNodes(o.getPath()).size() > level)
                .collect(Collectors.groupingBy(
                        p -> p.getPath().getPathNodes().get(level).getAttribute()));

        baseAttributes.forEach((k, v) -> {
            var attributeType = v.stream()
                    .map(IdentifiedPath::getPath)
                    .map(AqlObjectPath::getPathNodes)
                    .map(n -> n.get(level))
                    .map(PathNode::getPredicateOrOperands)
                    .map(AttributeType::getAttributeType)
                    .reduce(AttributeType::merge)
                    .get();

            if (attributeType == AttributeType.BASE) {
                node.addChild(new PathNode(k), v);
            } else {
                Map<List<AndOperatorPredicate>, List<IdentifiedPath>> byAttType = v.stream()
                        .collect(Collectors.groupingBy(
                                p -> attributeType.cleanupPredicates(
                                        p.getPath().getPathNodes().get(level).getPredicateOrOperands()),
                                LinkedHashMap::new,
                                Collectors.toList()));
                byAttType.forEach((cleanPredicates, paths) -> node.addChild(new PathNode(k, cleanPredicates), paths));
            }
        });

        node.getChildren().forEach(c -> fillJoinTree(c, level + 1));
    }

    enum AttributeType {
        BASE,
        ARCHETYPE,
        NODE,
        NAME;

        /**
         * Remove predicates that are not relevant to this AttributeType
         *
         * @param predicateOrOperands
         * @return
         */
        public List<AndOperatorPredicate> cleanupPredicates(List<AndOperatorPredicate> predicateOrOperands) {
            return predicateOrOperands.stream()
                    .map(and -> {
                        Optional<ComparisonOperatorPredicate> archetypeNodeId = getOperand(
                                        and, AqlObjectPathUtil.ARCHETYPE_NODE_ID)
                                .filter(this::prefilter)
                                .findFirst();
                        Optional<ComparisonOperatorPredicate> nameValue = getOperand(and, AqlObjectPathUtil.NAME_VALUE)
                                .filter(this::prefilter)
                                .findFirst();
                        if (this == NODE) {
                            // remove name/value for nodeId entries
                            boolean isNodeId = archetypeNodeId
                                    .map(ComparisonOperatorPredicate::getValue)
                                    .map(Primitive.class::cast)
                                    .map(Primitive::getValue)
                                    .map(String.class::cast)
                                    .filter(v -> !v.startsWith("openEHR-"))
                                    .isPresent();
                            if (isNodeId) {
                                nameValue = Optional.empty();
                            }
                        }
                        if (archetypeNodeId.isEmpty() && nameValue.isEmpty()) {
                            return null;
                        } else {
                            return new AndOperatorPredicate(Stream.of(archetypeNodeId, nameValue)
                                    .flatMap(Optional::stream)
                                    .collect(Collectors.toList()));
                        }
                    })
                    .filter(Objects::nonNull)
                    // sort lists by archetypeNodeId and nameValue
                    .sorted(Comparator.<AndOperatorPredicate, String>comparing(
                                    and -> getStringValue(and, AqlObjectPathUtil.ARCHETYPE_NODE_ID))
                            .thenComparing(and -> getStringValue(and, AqlObjectPathUtil.NAME_VALUE)))
                    .toList();
        }

        private static String getStringValue(AndOperatorPredicate and, AqlObjectPath archetypeNodeId) {
            return getOperand(and, archetypeNodeId)
                    .findFirst()
                    .map(p -> (String) ((Primitive) p.getValue()).getValue())
                    .orElse(null);
        }

        private static Stream<ComparisonOperatorPredicate> getOperand(AndOperatorPredicate and, AqlObjectPath path) {
            return and.getOperands().stream().filter(op -> path.equals(op.getPath()));
        }

        private boolean prefilter(ComparisonOperatorPredicate op) {
            if (this == BASE) {
                return false;
            }
            ComparisonOperatorPredicate.PredicateComparisonOperator operator = op.getOperator();
            if (operator != ComparisonOperatorPredicate.PredicateComparisonOperator.EQ) {
                return false;
            }
            AqlObjectPath path = op.getPath();
            if (AqlObjectPathUtil.NAME_VALUE.equals(path)) {
                // Note: for NODE, if a node_id is given, the name has to be removed later
                return this != ARCHETYPE;
            } else if (AqlObjectPathUtil.ARCHETYPE_NODE_ID.equals(path)) {
                return this != NAME;
            } else {
                return false;
            }
        }

        public AttributeType merge(AttributeType type) {
            if (this == type) {
                return this;
            }
            return switch (this) {
                case BASE -> this;
                case ARCHETYPE -> type == NODE ? this : BASE;
                case NODE -> type == NAME ? BASE : type;
                case NAME -> BASE;
            };
        }

        public AttributeType mergeAndPredicates(AttributeType type) {
            if (this == BASE || type == BASE) {
                throw new IllegalArgumentException("BASE cannot be merged");
            }
            if (this == type) {
                return this;
            }
            // Assumption: no duplicates, e.g. [name/value='a' and name/value='b']
            return NODE;
        }

        public static AttributeType getAttributeType(List<AndOperatorPredicate> predicateOrOperands) {
            return predicateOrOperands.stream()
                    .map(AttributeType::getAttributeType)
                    .reduce(AttributeType::merge)
                    .orElse(BASE);
        }

        public static AttributeType getAttributeType(AndOperatorPredicate and) {
            return and.getOperands().stream()
                    .map(AttributeType::getAttributeType)
                    .filter(Objects::nonNull)
                    .reduce(AttributeType::mergeAndPredicates)
                    .orElse(BASE);
        }

        private static AttributeType getAttributeType(ComparisonOperatorPredicate op) {
            ComparisonOperatorPredicate.PredicateComparisonOperator operator = op.getOperator();
            if (operator != ComparisonOperatorPredicate.PredicateComparisonOperator.EQ) {
                return null;
            }
            AqlObjectPath path = op.getPath();
            if (AqlObjectPathUtil.NAME_VALUE.equals(path)) {
                return NAME;
            } else if (AqlObjectPathUtil.ARCHETYPE_NODE_ID.equals(path)) {
                PathPredicateOperand<?> value = op.getValue();
                if (value instanceof Primitive p && p.getValue() instanceof String v) {
                    if (v.startsWith("openEHR-")) {
                        return ARCHETYPE;
                    } else {
                        return NODE;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public static class PathCohesionTreeNode extends TreeNode<PathCohesionTreeNode> {

        private PathNode attribute;
        private final List<IdentifiedPath> paths;
        private final List<IdentifiedPath> pathsEndingAtNode;
        private final List<IdentifiedPath> pathsEndingAtNodeView;
        private final boolean root;

        private PathCohesionTreeNode(PathNode attribute, List<IdentifiedPath> paths, boolean root) {
            this.attribute = attribute;
            this.paths = paths;
            this.pathsEndingAtNode = new ArrayList<>(paths);
            this.pathsEndingAtNodeView = Collections.unmodifiableList(this.pathsEndingAtNode);
            this.root = root;
        }

        PathCohesionTreeNode addChild(PathNode attribute, List<IdentifiedPath> paths) {
            pathsEndingAtNode.removeAll(paths);
            return addChild(new PathCohesionTreeNode(attribute, paths, false));
        }

        public static PathCohesionTreeNode root(PathNode attribute, List<IdentifiedPath> paths) {
            return new PathCohesionTreeNode(attribute, paths, true);
        }

        public PathNode getAttribute() {
            return attribute;
        }

        public void setAttribute(PathNode attribute) {
            this.attribute = attribute;
        }

        /**
         * all paths this attribute belongs to
         */
        public List<IdentifiedPath> getPaths() {
            return paths;
        }

        public List<IdentifiedPath> getPathsEndingAtNode() {
            return pathsEndingAtNodeView;
        }

        public boolean isRoot() {
            return root;
        }

        @Override
        public String toString() {
            return "PathCohesionTreeNode{" + "attribute="
                    + attribute + ", paths="
                    + paths + ", pathsEndingAtNode="
                    + pathsEndingAtNode + ", root="
                    + root + '}';
        }
    }
}
