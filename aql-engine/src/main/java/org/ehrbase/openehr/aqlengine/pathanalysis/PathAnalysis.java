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

import com.nedap.archie.aom.ArchetypeHRID;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.RMAttributeInfo;
import com.nedap.archie.rminfo.RMTypeInfo;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.ehrbase.openehr.sdk.aql.dto.operand.BooleanPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.DoublePrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.LongPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.PathPredicateOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.TemporalPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;

/**
 * <p>AQL paths occur in select expressions, where conditions,
 * but also within the predicates of AQL paths.</p>
 *
 * <p>
 * Paths originate from structure RM types from the FROM clause,
 * or from a path node featuring a predicate.
 * FROM roots can be constrained via predicates and additional CONTAINS clauses.
 *
 * <pre>
 * select
 * o[openEHR-EHR-OBSERVATION.blood_pressure.v2]/data[archetype_node_id="at0001" and name/value="History"]/events
 * FROM ENTRY o;
 * </pre>
 *
 * A path originates from a structure RM type from the FROM clause,
 * which is constrained via predicates and additional CONTAINS clauses.
 *
 * or from the node featuring a predicate.
 * It consists of a list of attributes, which can be constrained by additional predicates.
 * The RM model specifies the RM types with their attributes and data types of those attributes.</p>
 *
 * <p>This information can be used to infer
 * <ul>
 * <li>if a path is valid</li>
 * <li>if a path, or a node, is single-valued</li>
 * <li>the possible data types of a path</li>
 * </ul>
 * </p>
 *
 * <p>Since many <code>WhereCondition</code>s only operate on certain data types, these can also constrain the paths.
 * Note that these constraints may, however, not inherently apply to the path if the constraint is part of a <code>OR</code> or <code>NOT</code> condition.</p>
 *
 * <p>The information can be used to determine how the field needs to be accessed in the database structure, e.g.
 * <ul>
 *     <li>What structure nodes need to be joined</li>
 *     <li>If joins can/must be shared between different paths
 *     (For performance reasons, but also in order to prevent cartesian products due to multiple-valued paths sharing common base objects)</li>
 *     <li>If an RM object needs to be reconstructed</li>
 * </ul>
 * </p>
 *
 * <h2>Rules that constrain the base type of a node</h2>
 * <ul>
 *     <li>The candidate base types do not contain abstract classes ( derived classes, instead)</li>
 *     <li>The candidate base types only contain classes that feature the given attribute</li>
 *     <li>The candidate base types only contain classes where the attribute types match</li>
 *     <li>If a base type does not possess the TODO</li>
 * </ul>
 *
 *
 */
public class PathAnalysis {
    static final ArchieRMInfoLookup RM_INFOS = ArchieRMInfoLookup.getInstance();

    public record AttInfo(boolean multipleValued, boolean nullable, Set<String> targetTypes) {}

    public static class AttributeInfos {

        /**
         * All RM types that are relevant in the context of this type
         */
        static final Set<String> rmTypes;

        static final Map<String, Set<String>> baseTypesByAttribute;

        /**
         * Map&lt;attribute_name, Map&lt;parent_type, Set&lt;child_type&gt;&gt;&gt;
         */
        static final Map<String, Map<String, Set<String>>> typedAttributes;

        /**
         * Map&lt;attribute_name, Map&lt;parent_type, AttInfo&gt;&gt;
         */
        static final Map<String, Map<String, AttInfo>> attributeInfos;

        static {
            LinkedHashSet<String> typesModifiable = new LinkedHashSet<>();
            Stream.of(RmConstants.COMPOSITION, RmConstants.EHR_STATUS, RmConstants.ORIGINAL_VERSION)
                    .map(AttributeInfos::calculateContainedTypes)
                    .forEach(typesModifiable::addAll);

            Map<String, Set<String>> baseTypesByAttributeModifiable = calculateBaseTypesByAttribute(typesModifiable);
            Map<String, Map<String, Set<String>>> typedAttributesModifiable = calculateTypedAttributes(typesModifiable);
            Map<String, Map<String, AttInfo>> attributeInfosModifiable =
                    calculateAttributeInfos(typedAttributesModifiable);

            // manually add EHR
            addEhrAttributes(
                    typesModifiable,
                    baseTypesByAttributeModifiable,
                    typedAttributesModifiable,
                    attributeInfosModifiable);

            rmTypes = Collections.unmodifiableSet(typesModifiable);
            baseTypesByAttribute = unmodifiableCopy(baseTypesByAttributeModifiable);
            typedAttributes = unmodifiableCopy(typedAttributesModifiable);
            attributeInfos = unmodifiableCopy(attributeInfosModifiable);
        }

        private static void addEhrAttributes(
                Set<String> rmTypes,
                Map<String, Set<String>> baseTypesByAttribute,
                Map<String, Map<String, Set<String>>> typedAttributes,
                Map<String, Map<String, AttInfo>> attributeInfos) {
            String baseType = "EHR";
            rmTypes.add(baseType);

            // ehrId: HIER_OBJECT_ID
            addAttribute(
                    "ehrId", baseType, Set.of("HIER_OBJECT_ID"), baseTypesByAttribute, typedAttributes, attributeInfos);

            // timeCreated: DV_DATE_TIME,
            addAttribute(
                    "timeCreated",
                    baseType,
                    Set.of("DV_DATE_TIME"),
                    baseTypesByAttribute,
                    typedAttributes,
                    attributeInfos);

            // ehrStatus: OBJECT_REF -> EHR_STATUS
            addAttribute(
                    "ehrStatus", baseType, Set.of("EHR_STATUS"), baseTypesByAttribute, typedAttributes, attributeInfos);

            // compositions: OBJECT_REF -> COMPOSITION
            addAttribute(
                    "compositions",
                    baseType,
                    Set.of("COMPOSITION"),
                    baseTypesByAttribute,
                    typedAttributes,
                    attributeInfos);

            // Not supported:
            // systemId: HIER_OBJECT_ID
            // ehrAccess: OBJECT_REF -> EHR_ACCESS
            // directory: OBJECT_REF -> FOLDER
            // contributions: OBJECT_REF -> CONTRIBUTION
            // folders: OBJECT_REF -> FOLDER
        }

        private static void addAttribute(
                String attribute,
                String baseType,
                Set<String> targetTypes,
                Map<String, Set<String>> baseTypesByAttribute,
                Map<String, Map<String, Set<String>>> typedAttributes,
                Map<String, Map<String, AttInfo>> attributeInfos) {
            baseTypesByAttribute
                    .computeIfAbsent(attribute, k -> new HashSet<>())
                    .add(baseType);
            typedAttributes
                    .computeIfAbsent(attribute, k -> new HashMap<>())
                    .computeIfAbsent(baseType, k -> new HashSet<>())
                    .addAll(targetTypes);
            attributeInfos
                    .computeIfAbsent(attribute, k -> new HashMap<>())
                    .put(baseType, new AttInfo(false, false, targetTypes));
        }

        private static <K, V> Map<K, V> unmodifiableCopy(Map<K, V> map) {
            if (map == null) {
                return null;
            }
            Map ret = new HashMap();
            map.forEach((k, v) -> {
                ret.put(
                        k,
                        switch (v) {
                            case Map m -> unmodifiableCopy(m);
                            case Set s -> unmodifiableCopy(s);
                            default -> v;
                        });
            });
            return Map.copyOf(ret);
        }

        private static <V> Set<V> unmodifiableCopy(Set<V> set) {
            if (set == null) {
                return null;
            }
            Set ret = new HashSet();
            set.forEach(v -> {
                ret.add(
                        switch (v) {
                            case Map m -> unmodifiableCopy(m);
                            case Set s -> unmodifiableCopy(s);
                            default -> v;
                        });
            });
            return Set.copyOf(ret);
        }

        private static List<String> calculateContainedTypes(String rootType) {
            Queue<RMTypeInfo> remainingTypes = new LinkedList<>();
            remainingTypes.add(RM_INFOS.getTypeInfo(rootType));

            Set<RMTypeInfo> seen = new HashSet<>();
            seen.add(remainingTypes.peek());

            List<String> typeNames = new ArrayList<>();

            while (!remainingTypes.isEmpty()) {
                RMTypeInfo typeInfo = remainingTypes.poll();

                typeNames.add(typeInfo.getRmName());

                typeInfo.getDirectDescendantClasses().stream().filter(seen::add).forEach(remainingTypes::add);

                typeInfo.getAttributes().values().stream()
                        .filter(ti -> !ti.isComputed())
                        .map(RMAttributeInfo::getTypeNameInCollection)
                        .map(RM_INFOS::getTypeInfo)
                        .filter(Objects::nonNull)
                        .filter(seen::add)
                        .forEach(remainingTypes::add);
            }
            return typeNames;
        }

        private static Map<String, Set<String>> calculateBaseTypesByAttribute(Set<String> rmTypes) {
            return rmTypes.stream()
                    .map(RM_INFOS::getTypeInfo)
                    .filter(Objects::nonNull)
                    .flatMap(t -> t.getAttributes().values().stream()
                            .filter(a -> !a.isComputed())
                            .map(a -> Pair.of(t.getRmName(), a)))
                    .collect(Collectors.groupingBy(
                            p -> p.getRight().getRmName(), Collectors.mapping(p -> p.getLeft(), Collectors.toSet())));
        }

        static Map<String, Map<String, Set<String>>> calculateTypedAttributes(Set<String> rmTypes) {
            return rmTypes.stream()
                    .map(RM_INFOS::getTypeInfo)
                    .filter(Objects::nonNull)
                    .flatMap(t -> t.getAttributes().values().stream()
                            .filter(a -> !a.isComputed())
                            .flatMap(a -> resolveConcreteTypeNames(a.getTypeNameInCollection())
                                    .map(vt -> Triple.of(t.getRmName(), a.getRmName(), vt))))
                    .collect(Collectors.groupingBy(
                            Triple::getMiddle,
                            Collectors.groupingBy(
                                    Triple::getLeft, Collectors.mapping(Triple::getRight, Collectors.toSet()))));
        }

        private static Map<String, Map<String, AttInfo>> calculateAttributeInfos(
                Map<String, Map<String, Set<String>>> typedAttributes) {
            return typedAttributes.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, f -> {
                                RMAttributeInfo attributeInfo = RM_INFOS.getAttributeInfo(f.getKey(), e.getKey());
                                return new AttInfo(
                                        attributeInfo.isMultipleValued(), attributeInfo.isNullable(), f.getValue());
                            }))));
        }
    }

    static void validateAttributeNamesExist(ANode rootNode) {
        Iterator<ANode> nodeIt = iterateNodes(rootNode);
        while (nodeIt.hasNext()) {
            ANode node = nodeIt.next();
            node.attributes.keySet().forEach(att -> {
                if (!AttributeInfos.attributeInfos.containsKey(att)) {
                    throw new IllegalArgumentException("Unknown attribute: %s".formatted(att));
                }
            });
        }
    }

    private PathAnalysis() {
        // NOOP
    }

    /**
     * For abstract RM-Types all implementations are returned
     *
     * @param abstractType
     * @return
     */
    static Stream<String> resolveConcreteTypeNames(String abstractType) {

        RMTypeInfo typeInfo = RM_INFOS.getTypeInfo(abstractType);
        if (typeInfo == null) {
            // no RM object
            return Stream.of(abstractType);
        }

        Set<RMTypeInfo> concreteTypes = typeInfo.getAllDescendantClasses();
        concreteTypes.add(typeInfo);
        concreteTypes.removeIf(i -> Modifier.isAbstract(i.getJavaClass().getModifiers()));
        return concreteTypes.stream().map(RMTypeInfo::getRmName);
    }

    static Optional<String> rmTypeFromArchetype(String archetypeNodeId) {
        return Optional.ofNullable(archetypeNodeId)
                .filter(s -> s.startsWith("openEHR-EHR-"))
                .map(s -> {
                    try {
                        return new ArchetypeHRID(archetypeNodeId);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .map(ArchetypeHRID::getRmClass);
    }

    /**
     * Determine which types the value is compatible with
     *
     * @param value
     * @return
     */
    static Set<String> getCandidateTypes(PathPredicateOperand value) {

        //            FoundationType.BOOLEAN,
        //            FoundationType.BYTE,
        //            FoundationType.DOUBLE,
        //            FoundationType.INTEGER,
        //            FoundationType.LONG,
        //            FoundationType.STRING,
        //            FoundationType.URI,
        //            FoundationType.TEMPORAL,
        //            FoundationType.TEMPORAL_ACCESSOR,
        //            FoundationType.TEMPORAL_AMOUNT,
        //            FoundationType.CHAR,
        //            FoundationType.OBJECT,

        if (value instanceof Primitive p) {
            if (p.getValue() == null) {
                return null;
            }

            if (value instanceof DoublePrimitive || value instanceof LongPrimitive) {
                return Stream.of(FoundationType.DOUBLE, FoundationType.INTEGER, FoundationType.LONG)
                        .map(Enum::name)
                        .collect(Collectors.toSet());
            } else if (value instanceof BooleanPrimitive) {
                return Stream.of(FoundationType.BOOLEAN).map(Enum::name).collect(Collectors.toSet());
            } else if (value instanceof StringPrimitive) {

                if (value instanceof TemporalPrimitive) {
                    // XXX really all? Or check data?
                    return Stream.of(
                                    FoundationType.STRING,
                                    FoundationType.TEMPORAL,
                                    FoundationType.TEMPORAL_ACCESSOR,
                                    FoundationType.TEMPORAL_AMOUNT)
                            .map(Enum::name)
                            .collect(Collectors.toSet());
                } else {
                    return Stream.of(FoundationType.STRING, FoundationType.CHAR, FoundationType.URI)
                            .map(Enum::name)
                            .collect(Collectors.toSet());
                }
            } else {
                throw new IllegalArgumentException(
                        "Unknown primitive type %s".formatted(value.getClass().getName()));
            }

        } else {
            return null;
        }
    }

    public static Map<ANode, Map<String, AttInfo>> createAttributeInfos(ANode rootNode) {
        Map<ANode, Map<String, AttInfo>> infos = new HashMap<>();

        Iterator<ANode> nodeIt = iterateNodes(rootNode);
        while (nodeIt.hasNext()) {
            ANode node = nodeIt.next();
            Map<String, AttInfo> attInfos = node.attributes.entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), createAttributeInfo(node, e.getKey(), e.getValue())))
                    .filter(p -> p.getValue() != null)
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
            if (!attInfos.isEmpty()) {
                infos.put(node, attInfos);
            }
        }
        return infos;
    }

    private static Iterator<ANode> iterateNodes(ANode rootNode) {
        Queue<ANode> stack = new LinkedList<>();
        stack.add(rootNode);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !stack.isEmpty();
            }

            @Override
            public ANode next() {
                ANode node = stack.remove();
                stack.addAll(node.attributes.values());
                return node;
            }
        };
    }

    private static AttInfo createAttributeInfo(ANode node, String attName, ANode childNode) {
        return AttributeInfos.attributeInfos.getOrDefault(attName, Map.of()).entrySet().stream()
                .filter(e -> node.candidateTypes.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .filter(a -> !Collections.disjoint(childNode.candidateTypes, a.targetTypes))
                .reduce((a, b) -> new AttInfo(
                        a.multipleValued || b.multipleValued,
                        a.nullable || b.nullable,
                        SetUtils.union(a.targetTypes, b.targetTypes)))
                .orElse(null);
    }

    /**
     * Determines for each node of the path (resulting from the path hierarchy directly, or from predicates)
     * a set of possible RM or Foundational types
     *
     * @param rootType
     * @param variablePredicates
     * @param rootPredicates
     * @param path
     * @param candidateTypes
     * @return
     */
    public static ANode analyzeAqlPathTypes(
            String rootType,
            List<AndOperatorPredicate> variablePredicates,
            List<AndOperatorPredicate> rootPredicates,
            AqlObjectPath path,
            Set<String> candidateTypes) {
        // https://specifications.openehr.org/releases/QUERY/latest/AQL.html#_identified_paths

        // c[data[att0]/value = data[att1]/value and data[att1]/value = 1]/data[att0]/value
        // ORDER BY c/data[att3]/value
        ANode rootNode = new ANode(rootType, variablePredicates, rootPredicates);
        appendPath(rootNode, path, candidateTypes);

        validateAttributeNamesExist(rootNode);

        while (applyChildAttributeConstraints(rootNode)) {
            // NOOP
        }
        return rootNode;
    }

    private static boolean applyChildAttributeConstraints(ANode node) {
        if (node.attributes.isEmpty() || (node.candidateTypes != null && node.candidateTypes.isEmpty())) {
            return false;
        }
        boolean changed = false;
        for (Map.Entry<String, ANode> att : node.attributes.entrySet()) {
            changed |= applyAttributeConstraints(node, att.getKey(), att.getValue());
            changed |= applyChildAttributeConstraints(att.getValue());
        }
        return changed;
    }

    private static boolean applyAttributeConstraints(ANode parentNode, String attName, ANode childNode) {

        Map<String, Set<String>> typeConstellations = AttributeInfos.typedAttributes.get(attName);
        if (typeConstellations == null) {
            parentNode.candidateTypes = new HashSet<>();
            childNode.candidateTypes = new HashSet<>();
            return true;
        } else if (parentNode.candidateTypes == null) {
            if (childNode.candidateTypes == null) {
                parentNode.candidateTypes = new HashSet<>(typeConstellations.keySet());
                childNode.candidateTypes = new HashSet<>(typeConstellations.values().stream()
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet()));
            } else {
                Set<String> childConstraints = typeConstellations.values().stream()
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet());
                childNode.candidateTypes.removeIf(t -> !childConstraints.contains(t));
                parentNode.candidateTypes = typeConstellations.entrySet().stream()
                        .filter(e -> CollectionUtils.containsAny(e.getValue(), childNode.candidateTypes))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
            }

            return true;
        } else {
            boolean changed = parentNode.candidateTypes.removeIf(t -> {
                Set<String> supportedChildTypes = typeConstellations.get(t);
                if (CollectionUtils.isEmpty(supportedChildTypes)) {
                    return true;
                }
                if (childNode.candidateTypes == null) {
                    return false;
                }
                return !CollectionUtils.containsAny(childNode.candidateTypes, supportedChildTypes);
            });
            Set<String> childConstraints = typeConstellations.entrySet().stream()
                    .filter(e -> parentNode.candidateTypes.contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            if (childNode.candidateTypes == null) {
                childNode.candidateTypes = childConstraints;
                changed = true;
            } else {
                changed |= childNode.candidateTypes.removeIf(t -> !childConstraints.contains(t));
            }
            return changed;
        }
    }

    static void appendPath(ANode root, AqlObjectPath path, Set<String> candidateTypes) {
        if (path == null) {
            return;
        }
        Iterator<PathNode> nodeIt = path.getPathNodes().iterator();

        ANode n = root;
        while (nodeIt.hasNext()) {
            n = addAttributes(n, nodeIt.next());
        }
        if (candidateTypes != null) {
            if (n.candidateTypes == null) {
                n.candidateTypes = new HashSet<>(candidateTypes);
            } else {
                n.candidateTypes.removeIf(t -> !candidateTypes.contains(t));
            }
        }
    }

    private static ANode addAttributes(ANode root, PathNode child) {
        String attName = child.getAttribute();
        ANode childANode = root.attributes.get(attName);

        if (childANode == null) {
            childANode = new ANode((String) null, null, child.getPredicateOrOperands());
            root.attributes.put(attName, childANode);

        } else {
            // children collide and have to be merged
            childANode.constrainByArchetype(child.getPredicateOrOperands());
            childANode.addPredicateConstraints(child.getPredicateOrOperands());
        }

        return childANode;
    }
}
