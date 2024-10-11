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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.jooq.pg.Tables;
import org.ehrbase.openehr.aqlengine.asl.AslUtils.AliasProvider;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslDescendantCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotNullQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition.AslConditionOperator;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslFolderItemIdValuesColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslAbstractJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslFolderItemJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoin;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery.AslSourceRelation;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsChain;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsSetOperationWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.RmContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.VersionContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper.LogicalConditionOperator;
import org.ehrbase.openehr.dbformat.AncestorStructureRmType;
import org.ehrbase.openehr.dbformat.RmTypeAlias;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.containment.Containment;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperatorSymbol;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.jooq.JoinType;

final class AslFromCreator {

    private final AliasProvider aliasProvider;
    private final KnowledgeCacheService knowledgeCacheService;

    AslFromCreator(AliasProvider aliasProvider, KnowledgeCacheService knowledgeCacheService) {
        this.aliasProvider = aliasProvider;
        this.knowledgeCacheService = knowledgeCacheService;
    }

    @FunctionalInterface
    interface ContainsToOwnerProvider {
        OwnerProviderTuple get(ContainsWrapper contains);
    }

    public ContainsToOwnerProvider addFromClause(AslRootQuery rootQuery, AqlQueryWrapper queryWrapper) {

        final Map<ContainsWrapper, OwnerProviderTuple> containsToStructureSubQuery = new HashMap<>();
        ContainsChain fromChain = queryWrapper.containsChain();
        addContainsChain(rootQuery, null, fromChain, false, containsToStructureSubQuery);

        // add contains condition to rootQuery
        buildContainsCondition(fromChain, false, containsToStructureSubQuery).ifPresent(rootQuery::addConditionAnd);

        return containsToStructureSubQuery::get;
    }

    /**
     * Determines the AslSourceRelation.
     * If it cannot be determined from desc, the parent is consulted.
     * This is the case when the structure rm type is not "distinguishing", e.g. for CLUSTER.
     *
     * @param desc
     * @param parent
     * @return
     */
    private static AslSourceRelation getSourceRelation(RmContainsWrapper desc, AslStructureQuery parent) {
        if (RmConstants.EHR.equals(desc.getRmType())) {
            return AslSourceRelation.EHR;
        }
        return Optional.of(desc)
                .map(RmContainsWrapper::getStructureRmType)
                .map(StructureRmType::getStructureRoot)
                .or(() -> AncestorStructureRmType.byTypeName(desc.getRmType())
                        .map(AncestorStructureRmType::getStructureRoot))
                .map(AslSourceRelation::get)
                .or(() -> Optional.ofNullable(parent).map(AslStructureQuery::getType))
                .orElse(null);
    }

    private void addContainsChain(
            AslEncapsulatingQuery encapsulatingQuery,
            AslStructureQuery lastParent,
            ContainsChain containsChain,
            boolean useLeftJoin,
            Map<ContainsWrapper, OwnerProviderTuple> containsToStructureSubQuery) {

        AslStructureQuery currentParent = lastParent;
        for (ContainsWrapper descriptor : containsChain.chain()) {
            currentParent = addContainsSubquery(
                    encapsulatingQuery, useLeftJoin, containsToStructureSubQuery, descriptor, currentParent);
        }

        if (containsChain.hasTrailingSetOperation()) {
            addContainsChainSetOperator(
                    encapsulatingQuery, containsChain, useLeftJoin, containsToStructureSubQuery, currentParent);
        }
    }

    private AslStructureQuery addContainsSubquery(
            AslEncapsulatingQuery encapsulatingQuery,
            boolean useLeftJoin,
            Map<ContainsWrapper, OwnerProviderTuple> containsToStructureSubQuery,
            ContainsWrapper descriptor,
            AslStructureQuery currentParent) {

        final RmContainsWrapper usedWrapper;
        final boolean isOriginalVersion;
        switch (descriptor) {
            case VersionContainsWrapper vcw -> {
                usedWrapper = vcw.child();
                isOriginalVersion = true;
            }
            case RmContainsWrapper rcw -> {
                usedWrapper = rcw;
                isOriginalVersion = false;
            }
        }

        AslSourceRelation parentType = Optional.ofNullable(currentParent)
                .map(AslStructureQuery::getType)
                .orElse(null);
        AslSourceRelation sourceRelation = getSourceRelation(usedWrapper, currentParent);

        boolean requiresVersionJoin;
        if (isOriginalVersion || parentType == AslSourceRelation.EHR) {
            requiresVersionJoin = true;
        }
        // In case we have FOLDER CONTAINS COMPOSITION c it could be that the c/uid/value is selected. In such cases
        // EncapsulatingQueryUtils.sqlSelectFieldForExtractedColumn uses the VO_ID and adds the COMP_VERSION.SYS_VERSION
        // field what is only available in the comp_version table.
        else if (parentType == AslSourceRelation.FOLDER && sourceRelation == AslSourceRelation.COMPOSITION) {
            requiresVersionJoin = true;
        } else if (currentParent != null || sourceRelation == AslSourceRelation.EHR) {
            requiresVersionJoin = false;
        } else {
            // Some paths for structure roots require access to the version table
            // (If we knew about the paths, the version join might sometimes be omitted)
            requiresVersionJoin = Optional.of(usedWrapper)
                    .map(RmContainsWrapper::getStructureRmType)
                    .filter(StructureRmType::isStructureRoot)
                    .isPresent();
        }

        final AslStructureQuery structureQuery = containsSubquery(usedWrapper, requiresVersionJoin, sourceRelation);
        structureQuery.setRepresentsOriginalVersionExpression(isOriginalVersion);

        addContainsSubqueryToContainer(encapsulatingQuery, structureQuery, currentParent, useLeftJoin);

        OwnerProviderTuple ownerProviderTuple = new OwnerProviderTuple(structureQuery, structureQuery);
        containsToStructureSubQuery.put(usedWrapper, ownerProviderTuple);
        if (isOriginalVersion) {
            containsToStructureSubQuery.put(descriptor, ownerProviderTuple);
        }
        return structureQuery;
    }

    private static void addContainsSubqueryToContainer(
            AslEncapsulatingQuery container,
            AslStructureQuery toAdd,
            AslStructureQuery joinParent,
            boolean asLeftJoin) {

        final AslJoin join;
        if (joinParent == null || container.getChildren().isEmpty()) {
            join = null;
        } else {
            JoinType joinType = asLeftJoin ? JoinType.LEFT_OUTER_JOIN : JoinType.JOIN;
            join = new AslJoin(joinParent, joinType, toAdd, aslJoinCondition(toAdd, joinParent));
        }
        container.addChild(toAdd, join);
    }

    private static AslAbstractJoinCondition aslJoinCondition(AslStructureQuery toAdd, AslStructureQuery joinParent) {

        AslSourceRelation parentType = joinParent.getType();
        AslSourceRelation targetType = toAdd.getType();
        if (parentType == AslSourceRelation.FOLDER && targetType == AslSourceRelation.COMPOSITION) {
            return new AslFolderItemJoinCondition(joinParent, joinParent, targetType, toAdd, toAdd);
        }
        return new AslDescendantCondition(parentType, joinParent, joinParent, targetType, toAdd, toAdd)
                .provideJoinCondition();
    }

    private void addContainsChainSetOperator(
            AslEncapsulatingQuery currentQuery,
            ContainsChain containsChain,
            boolean asLeftJoin,
            Map<ContainsWrapper, OwnerProviderTuple> containsToStructureSubQuery,
            AslStructureQuery currentParent) {
        ContainsSetOperationWrapper setOperator = containsChain.trailingSetOperation();
        for (ContainsChain operand : setOperator.operands()) {
            boolean requiresOrOperandSubQuery =
                    setOperator.operator() == ContainmentSetOperatorSymbol.OR && operand.size() > 1;

            if (requiresOrOperandSubQuery) {
                // OR operands with chaining inside need to be mapped to their own subquery.
                // Else the nested contains chain would not be isolated from the parent
                // and the outer left join would bleed into it.
                AslEncapsulatingQuery orSq =
                        buildOrOperandAsEncapsulatingQuery(containsToStructureSubQuery, currentParent, operand);
                AslStructureQuery child =
                        (AslStructureQuery) orSq.getChildren().getFirst().getLeft();
                currentQuery.addChild(
                        orSq,
                        new AslJoin(
                                currentParent,
                                JoinType.LEFT_OUTER_JOIN,
                                orSq,
                                new AslDescendantCondition(
                                                currentParent.getType(),
                                                currentParent,
                                                currentParent,
                                                child.getType(),
                                                orSq,
                                                child)
                                        .provideJoinCondition()));
            } else {
                // AND operands and simple (no chaining inside) OR operands can be joined directly
                addContainsChain(
                        currentQuery,
                        currentParent,
                        operand,
                        asLeftJoin || setOperator.operator() == ContainmentSetOperatorSymbol.OR,
                        containsToStructureSubQuery);
            }
        }
    }

    private AslEncapsulatingQuery buildOrOperandAsEncapsulatingQuery(
            Map<ContainsWrapper, OwnerProviderTuple> containsToStructureSubQuery,
            AslStructureQuery currentParent,
            ContainsChain operand) {
        AslEncapsulatingQuery orSq = new AslEncapsulatingQuery(aliasProvider.uniqueAlias("or_sq"));
        HashMap<ContainsWrapper, OwnerProviderTuple> subQueryMap = new HashMap<>();

        addContainsChain(orSq, currentParent, operand, false, subQueryMap);

        // add contains condition to orSq
        buildContainsCondition(operand, false, subQueryMap).ifPresent(orSq::addStructureCondition);

        // provider must be orSq
        subQueryMap.forEach((k, v) -> containsToStructureSubQuery.put(k, new OwnerProviderTuple(v.owner(), orSq)));

        return orSq;
    }

    private AslStructureQuery containsSubquery(
            RmContainsWrapper containsWrapper, boolean requiresVersionJoin, AslSourceRelation sourceRelation) {
        // e.g. "sCO_c_1"
        String rmType = containsWrapper.getRmType();
        final String sAlias = aliasProvider.uniqueAlias("s"
                + RmTypeAlias.optionalAlias(rmType).orElse(rmType)
                + Optional.of(containsWrapper)
                        .map(ContainsWrapper::alias)
                        .map(a -> "_" + a)
                        .orElse(""));

        final List<String> rmTypes;
        boolean isRoot;
        if (RmConstants.EHR.equals(rmType)) {
            rmTypes = List.of(RmConstants.EHR);
            isRoot = false;
        } else {
            // We only support structure types therefore we can ignore all non-structure descendants
            rmTypes = AncestorStructureRmType.byTypeName(rmType)
                    .map(AncestorStructureRmType::getDescendants)
                    .map(s -> s.stream().distinct().map(StructureRmType::name).toList())
                    .orElseGet(
                            () -> List.of(containsWrapper.getStructureRmType().name()));

            // Folder may be root, but is recursive
            isRoot = RmConstants.EHR_STATUS.equals(rmType) || RmConstants.COMPOSITION.equals(rmType);
        }
        final List<AslField> fields = fieldsForContainsSubquery(containsWrapper, requiresVersionJoin, sourceRelation);

        AslStructureQuery aslStructureQuery = new AslStructureQuery(
                sAlias, sourceRelation, fields, rmTypes, isRoot ? List.of() : rmTypes, null, requiresVersionJoin);
        AslUtils.predicates(
                        containsWrapper.getPredicate(),
                        c -> AslUtils.structurePredicateCondition(
                                c, aslStructureQuery, knowledgeCacheService::findUuidByTemplateId))
                .ifPresent(aslStructureQuery::addConditionAnd);

        if (isRoot) {
            aslStructureQuery.addConditionAnd(new AslFieldValueQueryCondition<>(
                    AslUtils.findFieldForOwner(
                            AslStructureColumn.NUM, aslStructureQuery.getSelect(), aslStructureQuery),
                    AslConditionOperator.EQ,
                    List.of(0)));
        }

        return aslStructureQuery;
    }

    @Nonnull
    private static List<AslField> fieldsForContainsSubquery(
            RmContainsWrapper nextDesc, boolean requiresVersionJoin, AslSourceRelation sourceRelation) {
        final List<AslField> fields = new ArrayList<>();
        if (RmConstants.EHR.equals(nextDesc.getRmType())) {
            fields.add(new AslColumnField(UUID.class, "id", null, false, AslExtractedColumn.EHR_ID));
            fields.add(new AslColumnField(OffsetDateTime.class, "creation_date", null, false, null));
        } else {
            Arrays.stream(AslStructureColumn.values())
                    .filter(c -> requiresVersionJoin
                            || c.isFromDataTable()
                            // Support for non-vo_id PKs
                            || sourceRelation.getPkeyFields().stream()
                                    .anyMatch(f -> f.getName().equals(c.getFieldName())))
                    // remove fields not supported by the relation
                    .filter(c -> Optional.of(c)
                            .map(f -> (requiresVersionJoin && c.isFromVersionTable())
                                    ? sourceRelation.getVersionTable()
                                    : sourceRelation.getDataTable())
                            .map(t -> t.field(c.getFieldName()))
                            .isPresent())
                    .map(AslStructureColumn::field)
                    .forEach(fields::add);

            // (Only) for Compositions version.root_concept mirrors the data.entity_concept of the COMPOSITION row
            if (requiresVersionJoin && RmConstants.COMPOSITION.equals(nextDesc.getRmType())) {
                fields.add(new AslColumnField(
                        String.class,
                        Tables.COMP_VERSION.ROOT_CONCEPT.getName(),
                        null,
                        true,
                        AslExtractedColumn.ROOT_CONCEPT));
            }

            // (Only) for FOLDER containing COMPOSITIONs we include the data items/id/value as complex extracted column
            Containment containment = nextDesc.containment().getContains();
            if (RmConstants.FOLDER.equals(nextDesc.getRmType())
                    && containment instanceof ContainmentClassExpression cs
                    && Objects.equals(cs.getType(), RmConstants.COMPOSITION)) {
                fields.add(new AslFolderItemIdValuesColumnField());
            }
        }
        return fields;
    }

    private static Optional<AslQueryCondition> buildContainsCondition(
            ContainsChain chainDescriptor,
            final boolean chainIsBelowOr,
            Map<ContainsWrapper, OwnerProviderTuple> containsToStructureSubQuery) {
        if (!chainIsBelowOr && !chainDescriptor.hasTrailingSetOperation()) {
            return Optional.empty();
        }

        List<AslQueryCondition> conditions = new ArrayList<>();
        if (chainIsBelowOr) {
            chainDescriptor.chain().stream()
                    .map(containsToStructureSubQuery::get)
                    .map(OwnerProviderTuple::provider)
                    // The first field in structure sub-queries should always be the id
                    .map(t -> new AslNotNullQueryCondition(t.getSelect().getFirst()))
                    .forEach(conditions::add);
        }

        if (chainDescriptor.hasTrailingSetOperation()) {
            containsConditionForSetOperator(chainDescriptor, chainIsBelowOr, containsToStructureSubQuery)
                    .forEach(conditions::add);
        }
        // merge as AND
        return Optional.of(conditions).map(List::stream).map(AslUtils::and);
    }

    private static Stream<AslQueryCondition> containsConditionForSetOperator(
            ContainsChain chainDescriptor,
            boolean chainIsBelowOr,
            Map<ContainsWrapper, OwnerProviderTuple> containsToStructureSubQuery) {
        ContainsSetOperationWrapper setOperator = chainDescriptor.trailingSetOperation();
        boolean isOrOperator = setOperator.operator() == ContainmentSetOperatorSymbol.OR;

        Stream<AslQueryCondition> operatorConditions = setOperator.operands().stream()
                .map(operand -> {
                    if (isOrOperator && operand.size() > 1) {
                        OwnerProviderTuple subQuery =
                                containsToStructureSubQuery.get(operand.chain().getFirst());
                        return new AslNotNullQueryCondition(AslUtils.findFieldForOwner(
                                AslStructureColumn.VO_ID, subQuery.provider().getSelect(), subQuery.owner()));
                    } else {
                        return buildContainsCondition(
                                        operand, chainIsBelowOr || isOrOperator, containsToStructureSubQuery)
                                .orElse(null);
                    }
                })
                .filter(Objects::nonNull);

        return isOrOperator
                ? AslUtils.reduceConditions(LogicalConditionOperator.OR, operatorConditions).stream()
                : operatorConditions;
    }
}
