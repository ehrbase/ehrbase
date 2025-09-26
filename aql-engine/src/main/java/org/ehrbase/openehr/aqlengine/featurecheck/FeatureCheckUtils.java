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
package org.ehrbase.openehr.aqlengine.featurecheck;

import com.nedap.archie.rm.datavalues.quantity.DvOrdered;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.RMTypeInfo;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.exception.AqlFeatureNotImplementedException;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.openehr.aqlengine.pathanalysis.ANode;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathAnalysis;
import org.ehrbase.openehr.dbformat.AncestorStructureRmType;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentVersionExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.NullPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.ehrbase.openehr.sdk.aql.util.AqlUtil;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;

final class FeatureCheckUtils {
    public static final ArchieRMInfoLookup RM_INFO_LOOKUP = ArchieRMInfoLookup.getInstance();
    private static final Set<String> DV_ORDERED_TYPES =
            RM_INFO_LOOKUP.getTypeInfo(DvOrdered.class).getAllDescendantClasses().stream()
                    .filter(t -> !Modifier.isAbstract(t.getJavaClass().getModifiers()))
                    .map(RMTypeInfo::getRmName)
                    .collect(Collectors.toSet());
    private static final Pattern OBJECT_VERSION_ID_REGEX =
            Pattern.compile("([a-fA-F0-9-]{36})(::([^:]*)::([1-9]\\d*))?");

    // TODO performance: change data structure EnumMap<ClauseType, Set<List<String>>  ; Set<Pair<ClauseType,
    // List<String>> ;  Index ClauseType,
    // path...
    private static final List<Pair<List<String>, Set<ClauseType>>> SUPPORTED_VERSION_PATHS = Stream.of(
                    Pair.of("uid/value", Set.of(ClauseType.SELECT, ClauseType.WHERE, ClauseType.ORDER_BY)),
                    Pair.of(
                            "commit_audit/time_committed",
                            Set.of(ClauseType.SELECT, ClauseType.WHERE, ClauseType.ORDER_BY)),
                    Pair.of("commit_audit/time_committed/value",
                            Set.of(ClauseType.SELECT, ClauseType.WHERE, ClauseType.ORDER_BY)),
                    Pair.of("commit_audit/system_id", Set.of(ClauseType.SELECT, ClauseType.WHERE)),
                    Pair.of("commit_audit/description", Set.of(ClauseType.SELECT)),
                    Pair.of(
                            "commit_audit/description/value",
                            Set.of(ClauseType.SELECT, ClauseType.WHERE, ClauseType.ORDER_BY)),
                    Pair.of("commit_audit/change_type", Set.of(ClauseType.SELECT)),
                    Pair.of(
                            "commit_audit/change_type/value",
                            Set.of(ClauseType.SELECT, ClauseType.WHERE, ClauseType.ORDER_BY)),
                    Pair.of(
                            "commit_audit/change_type/defining_code/code_string",
                            Set.of(ClauseType.SELECT, ClauseType.WHERE, ClauseType.ORDER_BY)),
                    Pair.of(
                            "commit_audit/change_type/defining_code/preferred_term",
                            Set.of(ClauseType.SELECT, ClauseType.WHERE, ClauseType.ORDER_BY)),
                    Pair.of(
                            "commit_audit/change_type/defining_code/terminology_id/value",
                            Set.of(ClauseType.SELECT, ClauseType.WHERE)),
                    Pair.of("contribution/id/value", Set.of(ClauseType.SELECT, ClauseType.WHERE, ClauseType.ORDER_BY)))
            .map(p -> Pair.of(Arrays.asList(p.getLeft().split("/")), p.getRight()))
            .toList();

    record PathDetails(AslExtractedColumn extractedColumn, Set<String> targetTypes) {
        public boolean targetsDvOrdered() {
            return targetTypes.stream().anyMatch(DV_ORDERED_TYPES::contains);
        }

        public boolean targetsPrimitive() {
            return targetTypes.stream().map(RM_INFO_LOOKUP::getTypeInfo).anyMatch(Objects::isNull);
        }
    }

    private FeatureCheckUtils() {}

    public static boolean startsWith(IdentifiedPath successor, IdentifiedPath predecessor) {
        if (successor == predecessor) {
            return true;
        }
        if (successor == null || predecessor == null) {
            return false;
        }
        if (!Objects.equals(successor.getRoot(), predecessor.getRoot())) {
            return false;
        }
        if (!Objects.equals(successor.getRootPredicate(), predecessor.getRootPredicate())) {
            return false;
        }

        List<AqlObjectPath.PathNode> successorPathNodes = Optional.of(successor)
                .map(IdentifiedPath::getPath)
                .map(AqlObjectPath::getPathNodes)
                .orElse(List.of());
        List<AqlObjectPath.PathNode> predecessorPathNodes = Optional.of(predecessor)
                .map(IdentifiedPath::getPath)
                .map(AqlObjectPath::getPathNodes)
                .orElse(List.of());
        int predecessorSize = predecessorPathNodes.size();
        if (successorPathNodes.size() < predecessorSize) {
            return false;
        }
        return predecessorPathNodes.equals(successorPathNodes.subList(0, predecessorSize));
    }

    private static void ensurePathPredicateSupported(
            AqlObjectPath path, String nodeType, List<AndOperatorPredicate> predicate, String systemId) {
        AqlUtil.streamPredicates(predicate).forEach(p -> {
            Optional<AslExtractedColumn> extractedColumn = AslExtractedColumn.find(nodeType, p.getPath());
            if (extractedColumn.isEmpty()) {
                throw new AqlFeatureNotImplementedException("Path predicate %s in path %s contains unsupported path %s"
                        .formatted(AqlRenderer.renderPredicate(predicate), path, p.getPath()));
            }
            if (extractedColumn.get() == AslExtractedColumn.ARCHETYPE_NODE_ID
                    && !EnumSet.of(
                                    ComparisonOperatorPredicate.PredicateComparisonOperator.EQ,
                                    ComparisonOperatorPredicate.PredicateComparisonOperator.NEQ)
                            .contains(p.getOperator())) {
                throw new AqlFeatureNotImplementedException("Predicates on 'archetype_node_id' only support = and !=");
            }
            ensureOperandSupported(new PathDetails(extractedColumn.get(), Set.of(nodeType)), p.getValue(), systemId);
        });
    }

    public static PathDetails findSupportedIdentifiedPath(
            IdentifiedPath ip, boolean allowEmpty, ClauseType clauseType, String systemId) {
        AqlObjectPath path = ip.getPath();
        AbstractContainmentExpression root = ip.getRoot();
        String containmentType =
                switch (root) {
                    case ContainmentClassExpression cce -> cce.getType();
                    case ContainmentVersionExpression __ -> RmConstants.ORIGINAL_VERSION;
                };
        boolean isVersionPath = RmConstants.ORIGINAL_VERSION.equals(containmentType);

        Set<String> containmentTargetTypes = AncestorStructureRmType.byTypeName(containmentType)
                .map(AncestorStructureRmType::getDescendants)
                .map(s -> s.stream().map(StructureRmType::name).collect(Collectors.toSet()))
                .orElse(Set.of(containmentType));

        if (CollectionUtils.isNotEmpty(ip.getRootPredicate())) {
            for (final String parentTargetType : containmentTargetTypes) {
                ensurePathPredicateSupported(path, parentTargetType, ip.getRootPredicate(), systemId);
            }
        }
        if (path == null) {
            if (allowEmpty) {
                if (isVersionPath) {
                    throw new AqlFeatureNotImplementedException(
                            "selecting the full VERSION object (%s)".formatted(root.getIdentifier()));
                }
                if (RmConstants.EHR.equals(containmentType)) {
                    throw new AqlFeatureNotImplementedException(
                            "selecting the full EHR object (%s)".formatted(root.getIdentifier()));
                }
                return new PathDetails(
                        null,
                        AncestorStructureRmType.byTypeName(containmentType)
                                .map(AncestorStructureRmType::getDescendants)
                                .map(s -> s.stream().map(StructureRmType::name).collect(Collectors.toSet()))
                                .orElse(Set.of(containmentType)));
            } else {
                throw new AqlFeatureNotImplementedException(
                        "%s: identified path for type %s is missing".formatted(clauseType, containmentType));
            }
        }

        if (RmConstants.EHR.equals(containmentType)) {
            if (CollectionUtils.isNotEmpty(ip.getRootPredicate())) {
                throw new AqlFeatureNotImplementedException(
                        "%s: root predicate for path %s".formatted(clauseType, ip.render()));
            }

            return AslExtractedColumn.find(containmentType, path)
                    .filter(ec -> !EnumSet.of(AslExtractedColumn.EHR_SYSTEM_ID_DV)
                                    .contains(ec)
                            || clauseType == ClauseType.SELECT)
                    .map(ec -> new PathDetails(ec, Set.of("String")))
                    .orElseThrow(() ->
                            new AqlFeatureNotImplementedException("%s: identified path '%s' for type %s not supported"
                                    .formatted(clauseType, path.render(), containmentType)));
        }

        List<String> pathAttributes = path.getPathNodes().stream()
                .map(AqlObjectPath.PathNode::getAttribute)
                .toList();
        // if VERSION check supported paths list first
        if (isVersionPath
                && SUPPORTED_VERSION_PATHS.stream()
                        .filter(p -> p.getRight().contains(clauseType))
                        .map(Pair::getLeft)
                        .noneMatch(p -> p.equals(pathAttributes))) {
            throw new AqlFeatureNotImplementedException("%s: VERSION path %s/%s is not supported"
                    .formatted(clauseType, root.getIdentifier(), path.render()));
        }

        int level = -1;
        ANode analyzed = PathAnalysis.analyzeAqlPathTypes(
                containmentType, ip.getRootPredicate(), root.getPredicates(), path, null);
        if (analyzed.getCandidateTypes().isEmpty()) {
            throw new IllegalAqlException("%s is not a valid RM path".formatted(ip.render()));
        }
        Map<ANode, Map<String, PathAnalysis.AttInfo>> attributeInfos = PathAnalysis.createAttributeInfos(analyzed);

        Set<String> targetTypes = new HashSet<>();
        Set<String> parentTargetTypes = containmentTargetTypes;

        final List<AqlObjectPath.PathNode> pathNodes = path.getPathNodes();
        for (int i = 0; i < pathNodes.size(); i++) {
            AqlObjectPath.PathNode pathNode = pathNodes.get(i);
            String attribute = pathAttributes.get(i);
            ANode analyzedParent = analyzed;
            analyzed = analyzed.getAttribute(attribute);
            level++;
            targetTypes = attributeInfos.get(analyzedParent).get(attribute).targetTypes().stream()
                    .filter(t ->
                            !isVersionPath || !attribute.equals("commit_audit") || RmConstants.AUDIT_DETAILS.equals(t))
                    .collect(Collectors.toSet());
            Set<ANode.NodeCategory> categories = analyzed.getCategories();
            if (categories.contains(ANode.NodeCategory.STRUCTURE_INTERMEDIATE)) {
                throw new AqlFeatureNotImplementedException("%s: path %s contains STRUCTURE_INTERMEDIATE attribute %s"
                        .formatted(clauseType, path.render(), attribute));
            }

            if (clauseType == ClauseType.WHERE
                    && i == pathNodes.size() - 1
                    && targetTypes.stream()
                            .map(RM_INFO_LOOKUP::getTypeInfo)
                            .noneMatch(t -> t == null || DV_ORDERED_TYPES.contains(t.getRmName()))) {
                throw new AqlFeatureNotImplementedException(
                        "%s: path %s only targets types that are not derived from DV_ORDERED and not primitive"
                                .formatted(clauseType, path.render()));
            }
            if (categories.size() != 1 || Collections.disjoint(categories, Set.of(ANode.NodeCategory.STRUCTURE))) {
                // (path ends with) extracted column?
                AqlObjectPath subPath = new AqlObjectPath(
                        path.getPathNodes().stream().skip(level).toList());

                // TODO CDR-1663 FOLDER.items is not yet supported
                if (parentTargetTypes.contains(RmConstants.FOLDER) && "items".equals(attribute)) {
                    throw new AqlFeatureNotImplementedException("Path FOLDER/items");
                }

                final Set<String> currentParentTargetTypes = parentTargetTypes;
                Optional<AslExtractedColumn> extractedColumn = AslExtractedColumn.find(
                                currentParentTargetTypes.iterator().next(), subPath)
                        .filter(ec -> ec.getAllowedRmTypes().containsAll(currentParentTargetTypes));

                if (extractedColumn.isEmpty()) {
                    List<AndOperatorPredicate> condition = pathNode.getPredicateOrOperands();
                    if (AqlUtil.streamPredicates(condition).findAny().isPresent()) {
                        throw new AqlFeatureNotImplementedException(
                                "%s: path %s contains a non-structure attribute (%s) with at least one predicate"
                                        .formatted(clauseType, path.render(), attribute));
                    }
                } else {
                    List<AqlObjectPath.PathNode> nodes = subPath.getPathNodes();
                    for (int j = 1; j < nodes.size(); j++) {
                        AqlObjectPath.PathNode node = nodes.get(j);
                        analyzedParent = analyzed;
                        analyzed = analyzed.getAttribute(node.getAttribute());
                        targetTypes = attributeInfos
                                .get(analyzedParent)
                                .get(node.getAttribute())
                                .targetTypes();
                    }
                    return new PathDetails(extractedColumn.get(), targetTypes);
                }
            }
            targetTypes.forEach(
                    t -> ensurePathPredicateSupported(path, t, pathNode.getPredicateOrOperands(), systemId));
            parentTargetTypes = targetTypes;
        }

        return new PathDetails(null, targetTypes);
    }

    public static void ensureOperandSupported(PathDetails pathWithType, Object operand, String systemId) {
        if (!(operand instanceof Primitive)) {
            throw new AqlFeatureNotImplementedException("Only primitive operands are supported");
        }
        if (operand instanceof NullPrimitive) {
            throw new AqlFeatureNotImplementedException("NULL is not supported");
        }
        if (pathWithType.extractedColumn() == AslExtractedColumn.VO_ID) {
            if (!(operand instanceof StringPrimitive sp)) {
                throw new IllegalAqlException("/uid/value comparisons require a string operand");
            }
            String value = sp.getValue();
            Matcher matcher = OBJECT_VERSION_ID_REGEX.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalAqlException("%s is not a valid OBJECT_VERSION_ID/UID".formatted(value));
            }
            try {
                // Check syntax
                UUID.fromString(matcher.group(1));
            } catch (IllegalArgumentException e) {
                throw new IllegalAqlException("%s does not start with a valid UID".formatted(value));
            }
            if (matcher.group(2) != null) {
                String system = matcher.group(3);
                if (StringUtils.isNotEmpty(system) && !system.equals(systemId)) {
                    throw new IllegalAqlException(
                            "CREATING_SYSTEM_ID of %s does not match this server (%s)".formatted(value, systemId));
                }
            }
        } else if (pathWithType.extractedColumn() == AslExtractedColumn.ARCHETYPE_NODE_ID) {
            if (!(operand instanceof StringPrimitive sp)) {
                throw new IllegalAqlException("%s comparisons require a string operand"
                        .formatted(
                                AslExtractedColumn.ARCHETYPE_NODE_ID.getPath().render()));
            }
            try {
                // Check syntax & type support
                AslRmTypeAndConcept.fromArchetypeNodeId(sp.getValue());
            } catch (IllegalArgumentException e) {
                throw new IllegalAqlException(e);
            }
        }
    }
}
