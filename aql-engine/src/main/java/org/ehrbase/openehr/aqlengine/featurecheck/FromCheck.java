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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.exception.AqlFeatureNotImplementedException;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.dbformat.AncestorStructureRmType;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.dbformat.StructureRoot;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.Containment;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentNotOperator;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperator;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentVersionExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.util.AqlUtil;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.springframework.util.CollectionUtils;

final class FromCheck implements FeatureCheck {

    private final SystemService systemService;

    private final AqlFeature aqlFeature;

    public FromCheck(SystemService systemService, AqlFeature aqlFeature) {
        this.systemService = systemService;
        this.aqlFeature = aqlFeature;
    }

    @Override
    public void ensureSupported(AqlQuery aqlQuery) {
        Containment currentContainment = aqlQuery.getFrom();
        if (currentContainment == null) {
            throw new AqlFeatureNotImplementedException("FROM must be specified");
        }
        if (currentContainment instanceof ContainmentClassExpression fc && RmConstants.EHR.equals(fc.getType())) {
            currentContainment = fc.getContains();
        } else if (!(currentContainment instanceof AbstractContainmentExpression)) {
            throw new AqlFeatureNotImplementedException("AND/OR/NOT only allowed after CONTAINS");
        }

        // remaining CONTAINS
        ensureContainmentSupported(currentContainment, null);

        // predicates in FROM
        AqlUtil.streamContainments(aqlQuery.getFrom()).forEach(this::ensureContainmentPredicateSupported);
    }

    private Pair<Containment, StructureRoot> ensureStructureContainsSupported(
            ContainmentClassExpression nextContainment, StructureRoot structure) {

        Set<StructureRmType> structureRmTypes = StructureRmType.byTypeName(nextContainment.getType())
                .map(Set::of)
                .or(() -> ensureAbstractStructureContainsSupported(nextContainment, structure)
                        .map(AncestorStructureRmType::getDescendants))
                .orElseThrow(() -> cremateUnsupportedType(nextContainment));

        if (!structureRmTypes.stream().allMatch(StructureRmType::isStructureEntry)) {
            throw new AqlFeatureNotImplementedException(
                    "CONTAINS %s is currently not supported".formatted(nextContainment.getType()));
        }

        if (structure == null
                && structureRmTypes.stream()
                        .map(StructureRmType::getStructureRoot)
                        .anyMatch(Objects::isNull)) {
            throw new IllegalAqlException(
                    "It is unclear if %s targets a COMPOSITION or EHR_STATUS".formatted(nextContainment.getType()));
        }

        // check FOLDERS enabled and contains is supported
        if (aqlFeature.aqlOnFolderEnabled()) {
            if (structure == StructureRoot.FOLDER
                    && !CollectionUtils.containsAny(
                            structureRmTypes, EnumSet.of(StructureRmType.FOLDER, StructureRmType.COMPOSITION))) {
                throw new AqlFeatureNotImplementedException(
                        "FOLDER CONTAINS %s is currently not supported".formatted(nextContainment.getType()));
            }
        }
        // otherwise ensure we are not querying folders
        else if (CollectionUtils.containsAny(structureRmTypes, EnumSet.of(StructureRmType.FOLDER))) {
            throw new AqlFeatureNotImplementedException("CONTAINS %s is an experimental feature and currently disabled."
                    .formatted(nextContainment.getType()));
        }

        StructureRoot structureRoot = structureRmTypes.stream()
                .map(StructureRmType::getStructureRoot)
                .collect(Collectors.reducing((a, b) -> a == b ? a : null))
                .orElse(null);

        return Pair.of(nextContainment.getContains(), structureRoot);
    }

    private static IllegalAqlException cremateUnsupportedType(ContainmentClassExpression nextContainment) {
        return new IllegalAqlException("Type %s is not supported in FROM, only: EHR, %s"
                .formatted(
                        nextContainment.getType(),
                        Stream.of(
                                        Arrays.stream(AncestorStructureRmType.values())
                                                .filter(at -> at.getNonStructureDescendants()
                                                        .isEmpty())
                                                .filter(at -> at.getDescendants().stream()
                                                        .allMatch(StructureRmType::isStructureEntry)),
                                        Arrays.stream(StructureRmType.values())
                                                .filter(StructureRmType::isStructureEntry))
                                .flatMap(s -> s)
                                .map(Enum::name)
                                .collect(Collectors.joining(", "))));
    }

    private void ensureContainmentSupported(Containment c, final StructureRoot parentStructure) {
        switch (c) {
            case null -> {
                /*NOOP*/
            }
            case ContainmentClassExpression cce -> {
                var next = ensureStructureContainsSupported(cce, parentStructure);
                StructureRoot structureRoot =
                        Optional.of(next).map(Pair::getRight).orElse(parentStructure);
                ensureContainmentSupported(next.getLeft(), structureRoot);

                ensureContainmentStructureSupported(parentStructure, cce, structureRoot);
            }
            case ContainmentVersionExpression cve -> ensureVersionContainmentSupported(cve);
            case ContainmentSetOperator cso -> cso.getValues()
                    .forEach(nc -> ensureContainmentSupported(nc, parentStructure));
            case ContainmentNotOperator ignored -> throw new AqlFeatureNotImplementedException("NOT CONTAINS");
            default -> throw new IllegalAqlException(
                    "Unknown containment type: %s".formatted(c.getClass().getSimpleName()));
        }
    }

    private void ensureVersionContainmentSupported(ContainmentVersionExpression cve) {
        Containment nextContainment = cve.getContains();
        if (nextContainment == null) {
            throw new IllegalAqlException("VERSION containment must be followed by another CONTAINS expression");
        }
        if (nextContainment instanceof ContainmentVersionExpression) {
            throw new IllegalAqlException("VERSION cannot contain another VERSION");
        }
        if (nextContainment instanceof ContainmentSetOperator || nextContainment instanceof ContainmentNotOperator) {
            throw new AqlFeatureNotImplementedException("AND/OR/NOT operator as next containment after VERSION");
        }
        ensureContainmentSupported(nextContainment, null);
    }

    private static void ensureContainmentStructureSupported(
            StructureRoot parentStructure, ContainmentClassExpression cce, StructureRoot structure) {
        boolean containmentStructureSupported =
                switch (parentStructure) {
                    case null -> structure != null;
                    case FOLDER -> structure == StructureRoot.FOLDER || structure == StructureRoot.COMPOSITION;
                    case COMPOSITION, EHR_STATUS -> parentStructure == structure;
                    default -> throw new RuntimeException("%s is not root structure".formatted(parentStructure));
                };

        if (!containmentStructureSupported) {
            throw new IllegalAqlException("Structure %s cannot CONTAIN %s (of structure %s)"
                    .formatted(
                            Optional.ofNullable(parentStructure)
                                    .map(Object::toString)
                                    .orElse(RmConstants.EHR),
                            cce.getType(),
                            structure));
        }
    }

    private void ensureContainmentPredicateSupported(AbstractContainmentExpression containment) {
        if (containment instanceof ContainmentVersionExpression cve) {
            ContainmentVersionExpression.VersionPredicateType pType = cve.getVersionPredicateType();
            if (pType != ContainmentVersionExpression.VersionPredicateType.LATEST_VERSION
                    && pType != ContainmentVersionExpression.VersionPredicateType.NONE) {
                throw new AqlFeatureNotImplementedException(
                        "Only VERSION queries without predicate or on LATEST_VERSION supported");
            }
        }
        if (containment.hasPredicates()) {
            List<AndOperatorPredicate> condition = containment.getPredicates();
            AqlUtil.streamPredicates(condition).forEach(predicate -> {
                IdentifiedPath identifiedPath = new IdentifiedPath();
                identifiedPath.setRoot(containment);
                identifiedPath.setPath(predicate.getPath());
                FeatureCheckUtils.PathDetails pathWithType = FeatureCheckUtils.findSupportedIdentifiedPath(
                        identifiedPath, false, ClauseType.FROM_PREDICATE, systemService.getSystemId());
                if (identifiedPath.getPath().equals(AslExtractedColumn.ARCHETYPE_NODE_ID.getPath())
                        && !EnumSet.of(
                                        ComparisonOperatorPredicate.PredicateComparisonOperator.EQ,
                                        ComparisonOperatorPredicate.PredicateComparisonOperator.NEQ)
                                .contains(predicate.getOperator())) {
                    throw new AqlFeatureNotImplementedException(
                            "Predicates on 'archetype_node_id' only support = and !=");
                }
                FeatureCheckUtils.ensureOperandSupported(
                        pathWithType, predicate.getValue(), systemService.getSystemId());
            });
        }
    }

    private static Optional<AncestorStructureRmType> ensureAbstractStructureContainsSupported(
            ContainmentClassExpression nextContainment, final StructureRoot structure) {
        Optional<AncestorStructureRmType> abstractType = AncestorStructureRmType.byTypeName(nextContainment.getType());

        abstractType.ifPresent(at -> {
            if (structure == null && at.getStructureRoot() == null) {
                throw new IllegalAqlException(
                        "It is unclear if %s targets a COMPOSITION or EHR_STATUS".formatted(nextContainment.getType()));
            } else if (!at.getNonStructureDescendants().isEmpty()) {
                throw new AqlFeatureNotImplementedException(
                        "CONTAINS %s: abstract type with non structure descendants (%s) not yet supported"
                                .formatted(
                                        nextContainment.getType(),
                                        at.getNonStructureDescendants().stream()
                                                .map(Class::getSimpleName)
                                                .toList()));
            }
        });

        return abstractType;
    }

    private static void ensureContainmentOnFolderSupported() {}
}
