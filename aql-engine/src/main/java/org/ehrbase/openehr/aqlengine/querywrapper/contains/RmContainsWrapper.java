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
package org.ehrbase.openehr.aqlengine.querywrapper.contains;

import static org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept.ARCHETYPE_PREFIX;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPathUtil;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate.PredicateComparisonOperator;

public final class RmContainsWrapper implements ContainsWrapper {
    private final ContainmentClassExpression containment;
    private ContainsWrapper parent;

    public RmContainsWrapper(ContainmentClassExpression containment) {
        this.containment = containment;
    }

    public List<AndOperatorPredicate> getPredicate() {
        return containment.getPredicates();
    }

    public StructureRmType getStructureRmType() {
        return StructureRmType.byTypeName(containment.getType()).orElse(null);
    }

    @Override
    public String getRmType() {
        return StructureRmType.byTypeName(containment.getType())
                .map(StructureRmType::name)
                .orElse(containment.getType());
    }

    @Override
    public boolean isAtCode() {
        return hasConsistentNodeIdPrefixes("at", "id");
    }

    @Override
    public boolean isArchetype() {
        return hasConsistentNodeIdPrefixes(ARCHETYPE_PREFIX);
    }

    @Override
    public ContainsWrapper getParent() {
        return parent;
    }

    @Override
    public void setParent(ContainsWrapper parent) {
        this.parent = parent;
    }

    @Override
    public String alias() {
        return containment.getIdentifier();
    }

    public ContainmentClassExpression containment() {
        return containment;
    }

    @Override
    public String toString() {
        return "RmContainsWrapper[" + "containment=" + containment + ']';
    }

    private boolean hasConsistentNodeIdPrefixes(String... allowedPrefixes) {
        List<AndOperatorPredicate> predicates = containment.getPredicates();
        if (CollectionUtils.isEmpty(predicates)) {
            return false;
        }

        return predicates.stream()
                .map(andPred -> andPred.getOperands().stream()
                        .filter(p -> AqlObjectPathUtil.ARCHETYPE_NODE_ID.equals(p.getPath())
                                && p.getOperator() == PredicateComparisonOperator.EQ
                                && p.getValue() instanceof StringPrimitive)
                        .map(p -> ((StringPrimitive) p.getValue()).getValue())
                        .map(v -> StringUtils.startsWithAny(v, allowedPrefixes))
                        .reduce(Boolean::logicalAnd)
                        .orElse(false))
                .reduce(true, Boolean::logicalAnd);
    }
}
