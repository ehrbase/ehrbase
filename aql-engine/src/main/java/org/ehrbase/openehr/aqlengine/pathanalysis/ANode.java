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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPathUtil;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;

public class ANode {
    /**
     * null means that the types are not constrained.
     * An empty set means there constrains cannot be satisfied.
     */
    Set<String> candidateTypes;

    public enum NodeCategory {
        /**
         * {@link StructureRmType}
         * with structureEntry == true:
         * LOCATABLEs + EVENT_CONTEXT
         */
        STRUCTURE,
        /**
         * An RM element that may contain structure entries, but is none itself:
         * {@link StructureRmType}.structureEntry == false:
         * FEEDER_AUDIT_DETAILS, INSTRUCTION_DETAILS
         * Candidates are typically PATHABLEs that are not LOCATABLE.
         * EVENT_CONTEXT is mapped as STRUCTURE;
         * ISM_TRANSITION does not contain LOCATABLEs
         */
        STRUCTURE_INTERMEDIATE,
        /**
         * An RM type
         */
        RM_TYPE,
        /**
         * {@link FoundationType}
         */
        FOUNDATION,
        /**
         * FOUNDATION + DV_CODED_TEXT + DV_PARSABLE in ELEMENT/value/value
         * <p>A common operation is retrieving the value of a DATA_VALUE contained in an ELEMENT.</p
         * Usually, the value of a DATA_VALUE is a Foundation type.</p>
         * <p>This does not, however, hold true for DV_TIME_SPECIFICATION subtypes (DV_PERIODIC_TIME_SPECIFICATION and DV_GENERAL_TIME_SPECIFICATION), where value is a DV_PARSABLE
         * and DV_STATE where it is a DV_CODED_TEXT.</p>
         * </p>This may have to be considered for e.g. comparisons and post-processing of results may be required.</p>
         * <p>Furthermore, contrary to DV_STATE, DV_CODED_TEXT.mappings is multiple-valued. This means that the data may be spread over several rows.
         * In order to omit having to query for all sub-rows, in this case the JSONB object should be supplemented with a copy of the full RM hierarchy of <code>mappings</code> could be stored additionally.
         * The first entry may be omitted if the <code>TERM_MAPPING.purpose</code> field is not split into several rows.</p>
         */
        FOUNDATION_EXTENDED
    }

    public Set<NodeCategory> getCategories() {
        if (candidateTypes == null) {
            throw new IllegalStateException("The candidate types have not been calculated");
        }

        Set<NodeCategory> result = EnumSet.noneOf(NodeCategory.class);
        candidateTypes.stream().map(ANode::getCategory).forEach(result::add);
        return result;
    }

    private static NodeCategory getCategory(String typeName) {

        return StructureRmType.byTypeName(typeName)
                .map(t -> t.isStructureEntry() ? NodeCategory.STRUCTURE : NodeCategory.STRUCTURE_INTERMEDIATE)
                .orElseGet(() -> FoundationType.byTypeName(typeName)
                        .map(t -> NodeCategory.FOUNDATION)
                        .orElse(NodeCategory.RM_TYPE));
    }

    final Map<String, ANode> attributes = new LinkedHashMap<>();

    public ANode(String rmType, List<AndOperatorPredicate> parentPredicates, List<AndOperatorPredicate> predicates) {
        this(rmType == null ? null : Set.of(rmType), parentPredicates, predicates);
    }

    public ANode(
            Set<String> rmTypes, List<AndOperatorPredicate> parentPredicates, List<AndOperatorPredicate> predicates) {
        // candidate types by specified RM type
        if (rmTypes == null) {
            candidateTypes = null;
        } else {
            candidateTypes = rmTypes.stream()
                    .flatMap(PathAnalysis::resolveConcreteTypeNames)
                    .collect(Collectors.toSet());
        }

        constrainByArchetype(parentPredicates);
        constrainByArchetype(predicates);

        addPredicateConstraints(parentPredicates);
        addPredicateConstraints(predicates);
    }

    public ANode getAttribute(String attribute) {
        return attributes.get(attribute);
    }

    public Set<String> getCandidateTypes() {
        return new HashSet<>(candidateTypes);
    }

    public void addPredicateConstraints(List<AndOperatorPredicate> predicates) {
        Iterator<ComparisonOperatorPredicate> it = Optional.ofNullable(predicates)
                .filter(p -> p.size() == 1)
                .map(List::getFirst)
                .map(AndOperatorPredicate::getOperands)
                .stream()
                .flatMap(List::stream)
                .filter(p -> !EnumSet.of(
                                ComparisonOperatorPredicate.PredicateComparisonOperator.NEQ,
                                ComparisonOperatorPredicate.PredicateComparisonOperator.MATCHES)
                        .contains(p.getOperator()))
                .iterator();

        while (it.hasNext()) {
            ComparisonOperatorPredicate p = it.next();
            PathAnalysis.appendPath(this, p.getPath(), PathAnalysis.getCandidateTypes(p.getValue()));
        }
    }

    public void constrainByArchetype(List<AndOperatorPredicate> predicates) {
        candidateTypes = constrainByArchetype(candidateTypes, predicates);
    }

    public static Set<String> constrainByArchetype(
            final Set<String> candidateTypes, List<AndOperatorPredicate> predicates) {

        if (predicates == null || (candidateTypes != null && candidateTypes.isEmpty())) {
            return candidateTypes;
        }

        boolean singleAnd = predicates.size() == 1;
        if (singleAnd) {
            // candidateTypes only changes when it has been null before
            return constrainByArchetype(candidateTypes, predicates.getFirst());

        } else {
            // for OR: constrain by union of all AND constraints
            Set<String> constraintUnion = null;

            Iterator<AndOperatorPredicate> it = predicates.iterator();
            while (it.hasNext() || (constraintUnion != null && constraintUnion.isEmpty())) {
                Set<String> candidateSet =
                        Optional.ofNullable(candidateTypes).map(HashSet::new).orElse(null);
                candidateSet = constrainByArchetype(candidateSet, it.next());

                if (candidateSet != null) {
                    if (constraintUnion == null) {
                        constraintUnion = candidateSet;
                    } else {
                        constraintUnion.addAll(candidateSet);
                    }
                }
            }

            if (constraintUnion == null) {
                return candidateTypes;
            } else if (candidateTypes == null) {
                return constraintUnion;
            } else {
                candidateTypes.retainAll(constraintUnion);
                return candidateTypes;
            }
        }
    }

    public static Set<String> constrainByArchetype(Set<String> candidateTypes, AndOperatorPredicate predicates) {
        Set<String> constrained = candidateTypes;

        Iterator<ComparisonOperatorPredicate> it = predicates.getOperands().iterator();
        while (it.hasNext() && !(constrained != null && constrained.isEmpty())) {
            String archetypeNodeId = getArchetypeNodeId(it.next()).orElse(null);
            if (archetypeNodeId != null) {
                constrained = constrainByArchetype(constrained, archetypeNodeId);
            }
        }
        return candidateTypes;
    }

    private static Optional<String> getArchetypeNodeId(ComparisonOperatorPredicate cmpOp) {
        return Optional.of(cmpOp)
                .filter(p -> p.getOperator() == ComparisonOperatorPredicate.PredicateComparisonOperator.EQ)
                .filter(p -> AqlObjectPathUtil.ARCHETYPE_NODE_ID.equals((p.getPath())))
                .map(ComparisonOperatorPredicate::getValue)
                .filter(StringPrimitive.class::isInstance)
                .map(StringPrimitive.class::cast)
                .map(StringPrimitive::getValue);
    }

    /**
     * Remove types not matching archetype
     *
     * @param archetypeNodeId
     */
    static Set<String> constrainByArchetype(Set<String> candidateTypes, String archetypeNodeId) {
        return PathAnalysis.rmTypeFromArchetype(archetypeNodeId)
                .map(PathAnalysis::resolveConcreteTypeNames)
                .map(s -> s.collect(Collectors.toSet()))
                .map(s -> {
                    if (candidateTypes == null) {
                        return s;
                    } else {
                        candidateTypes.retainAll(s);
                        return candidateTypes;
                    }
                })
                .orElse(candidateTypes);
    }
}
