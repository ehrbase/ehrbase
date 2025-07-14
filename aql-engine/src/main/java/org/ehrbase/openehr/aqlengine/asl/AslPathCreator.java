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

import static org.ehrbase.jooq.pg.Tables.AUDIT_DETAILS;
import static org.ehrbase.openehr.aqlengine.asl.AslUtils.streamConditionDescriptors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.aqlengine.asl.AslUtils.AliasProvider;
import org.ehrbase.openehr.aqlengine.asl.DataNodeInfo.ExtractedColumnDataNodeInfo;
import org.ehrbase.openehr.aqlengine.asl.DataNodeInfo.JsonRmDataNodeInfo;
import org.ehrbase.openehr.aqlengine.asl.DataNodeInfo.StructureRmDataNodeInfo;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldFieldQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotNullQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslProvidesJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition.AslConditionOperator;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslTrueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslConstantField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField.FieldSource;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoin;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslFilteringQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslPathDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRmObjectDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery.AslSourceRelation;
import org.ehrbase.openehr.aqlengine.pathanalysis.ANode.NodeCategory;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathCohesionAnalysis.PathCohesionTreeNode;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathInfo;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathInfo.JoinMode;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper.SelectType;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ComparisonOperatorConditionWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper.LogicalConditionOperator;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPathUtil;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.util.AqlUtil;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.jooq.JSONB;
import org.jooq.JoinType;
import org.springframework.util.function.SingletonSupplier;

final class AslPathCreator {

    private final AliasProvider aliasProvider;
    private final KnowledgeCacheService knowledgeCacheService;
    private final String systemId;

    @FunctionalInterface
    interface PathToField {
        AslField getField(IdentifiedPath path);
    }

    AslPathCreator(AliasProvider aliasProvider, KnowledgeCacheService knowledgeCacheService, String systemId) {
        this.aliasProvider = aliasProvider;
        this.knowledgeCacheService = knowledgeCacheService;
        this.systemId = systemId;
    }

    @Nonnull
    public PathToField addPathQueries(
            AqlQueryWrapper query,
            AslFromCreator.ContainsToOwnerProvider containsToStructureSubQuery,
            AslRootQuery rootQuery) {
        Map<IdentifiedPath, AslField> pathToField = new LinkedHashMap<>();

        addEhrFields(query, containsToStructureSubQuery, pathToField);

        List<DataNodeInfo> dataNodeInfos = new ArrayList<>();

        query.pathInfos().forEach((contains, pathInfo) -> {
            if (RmConstants.EHR.equals(contains.getRmType())) {
                throw new IllegalArgumentException(
                        "Only paths within [EHR_STATUS,COMPOSITION,FOLDER,CLUSTER] are supported");
            }

            OwnerProviderTuple parent = containsToStructureSubQuery.get(contains);
            AslSourceRelation sourceRelation = ((AslStructureQuery) parent.owner()).getType();

            joinPathStructureNode(
                            rootQuery,
                            parent,
                            pathInfo.isArchetypeNode(pathInfo.getCohesionTreeRoot()),
                            null,
                            sourceRelation,
                            pathInfo.getCohesionTreeRoot(),
                            pathInfo,
                            parent.provider(),
                            -1)
                    .forEach(dataNodeInfos::add);
        });

        addQueriesForDataNode(dataNodeInfos.stream(), rootQuery, null, pathToField);

        return pathToField::get;
    }

    private void addEhrFields(
            AqlQueryWrapper query,
            AslFromCreator.ContainsToOwnerProvider containsToStructureSubQuery,
            Map<IdentifiedPath, AslField> pathToField) {
        Stream.of(
                        // select
                        query.nonPrimitiveSelects()
                                // We want to skip COUNT(*) since it does not have a path
                                .filter(sd -> sd.type() != SelectType.AGGREGATE_FUNCTION
                                        || sd.getIdentifiedPath().isPresent())
                                .map(s ->
                                        Pair.of(s.root(), s.getIdentifiedPath().orElse(null))),
                        // where
                        streamConditionDescriptors(query.where())
                                .map(ComparisonOperatorConditionWrapper::leftComparisonOperand)
                                .map(s -> Pair.of(s.root(), s.path())),
                        // order by
                        query.orderBy().stream().map(s -> Pair.of(s.root(), s.identifiedPath())))
                .flatMap(s -> s)
                .filter(p -> RmConstants.EHR.equals(p.getLeft().getRmType()))
                .distinct()
                .forEach(p -> {
                    ContainsWrapper contains = p.getLeft();
                    AslExtractedColumn ec = AslExtractedColumn.find(
                                    contains, p.getRight().getPath())
                            .orElseThrow();
                    AslQuery ehrSubquery =
                            containsToStructureSubQuery.get(contains).owner();
                    AslField field;
                    if (ec == AslExtractedColumn.EHR_SYSTEM_ID_DV || ec == AslExtractedColumn.EHR_SYSTEM_ID) {
                        field = new AslConstantField<>(
                                String.class, systemId, new FieldSource(ehrSubquery, ehrSubquery, ehrSubquery), ec);
                    } else {
                        field = findExtractedColumnField(ec, new FieldSource(ehrSubquery, ehrSubquery, ehrSubquery));
                    }
                    pathToField.put(p.getRight(), field);
                });
    }

    private void addQueriesForDataNode(
            Stream<DataNodeInfo> dataNodeInfos,
            AslRootQuery rootQuery,
            AslPathDataQuery parentPathDataQuery,
            Map<IdentifiedPath, AslField> pathToField) {
        dataNodeInfos.forEach(dni -> {
            switch (dni) {
                case ExtractedColumnDataNodeInfo ecDni -> addExtractedColumns(rootQuery, ecDni, pathToField);
                case JsonRmDataNodeInfo jrdDni -> addPathDataQuery(jrdDni, rootQuery, parentPathDataQuery, pathToField);
                case StructureRmDataNodeInfo srdDni -> addRmObjectData(srdDni, rootQuery, pathToField);
            }
            dni.node().getPathsEndingAtNode().forEach(ip -> addFilterQueryIfRequired(dni, ip, rootQuery, pathToField));
        });
    }

    private void addPathDataQuery(
            JsonRmDataNodeInfo dni,
            AslRootQuery rootQuery,
            AslPathDataQuery parentPathDataQuery,
            Map<IdentifiedPath, AslField> pathToField) {
        boolean isPathDataRoot = parentPathDataQuery == null;
        final AslQuery base = isPathDataRoot ? (AslStructureQuery) dni.parent().owner() : parentPathDataQuery;
        final AslQuery provider = isPathDataRoot ? dni.providerSubQuery() : parentPathDataQuery;

        final AslPathDataQuery dataQuery;
        final AslField pathField;
        String alias = aliasProvider.uniqueAlias("pd");
        if (dni.multipleValued()) {
            if (isPathDataRoot) {
                // Extract the array first to apply structure based filters before unnesting -> avoids unwanted row
                // multiplication
                // In the future this may also apply to isPathDataRoot == false to support advanced filtering
                AslPathDataQuery arrayQuery = new AslPathDataQuery(
                        alias + "_array", base, provider, dni.pathInJson(), false, dni.dvOrderedTypes(), JSONB.class);
                rootQuery.addChild(arrayQuery, new AslJoin(provider, JoinType.LEFT_OUTER_JOIN, arrayQuery));

                dataQuery = new AslPathDataQuery(
                        alias, arrayQuery, arrayQuery, List.of(), true, dni.dvOrderedTypes(), dni.type());
                rootQuery.addChild(dataQuery, new AslJoin(arrayQuery, JoinType.LEFT_OUTER_JOIN, dataQuery));

            } else {
                dataQuery = new AslPathDataQuery(
                        alias, base, provider, dni.pathInJson(), true, dni.dvOrderedTypes(), dni.type());
                rootQuery.addChild(dataQuery, new AslJoin(provider, JoinType.LEFT_OUTER_JOIN, dataQuery));
            }
            pathField = dataQuery.getSelect().getFirst();
            addQueriesForDataNode(dni.dependentPathDataNodes(), rootQuery, dataQuery, pathToField);
        } else if (dni.dependentPathDataNodes().findAny().isPresent()) {
            throw new IllegalStateException("Only multiple-valued json-path-nodes can have dependent nodes");
        } else {
            pathField = new AslRmPathField(
                            AslUtils.findFieldForOwner("data", provider.getSelect(), base),
                            dni.pathInJson(),
                            dni.dvOrderedTypes(),
                            dni.type())
                    .withProvider(rootQuery);
        }
        dni.node().getPathsEndingAtNode().forEach(path -> pathToField.put(path, pathField));
    }

    private void addFilterQueryIfRequired(
            DataNodeInfo dni,
            IdentifiedPath identifiedPath,
            AslRootQuery rootQuery,
            Map<IdentifiedPath, AslField> pathToField) {
        List<AslJoinCondition> filterConditions = Stream.concat(
                        rootQuery.getChildren().stream()
                                .filter(jp -> jp.getLeft() == dni.providerSubQuery())
                                .map(Pair::getRight)
                                .filter(Objects::nonNull)
                                .map(AslJoin::getLeft),
                        Stream.of(dni.providerSubQuery()))
                .map(AslQuery::joinConditionsForFiltering)
                .map(m -> m.getOrDefault(identifiedPath, Collections.emptyList()))
                .flatMap(List::stream)
                .filter(jc -> !(jc.getCondition() instanceof AslTrueQueryCondition))
                .map(jc -> jc.withLeftProvider(rootQuery))
                .map(AslJoinCondition.class::cast)
                .toList();
        if (!filterConditions.isEmpty()) {
            AslField sourceField = pathToField.get(identifiedPath);

            if (sourceField instanceof AslSubqueryField sf) {
                AslSubqueryField filtered = sf.withFilterConditions(filterConditions);
                pathToField.replace(identifiedPath, filtered);

            } else {
                AslFilteringQuery filteringQuery = new AslFilteringQuery(
                        aliasProvider.uniqueAlias(sourceField.getOwner().getAlias() + "_f"), sourceField);
                rootQuery.addChild(
                        filteringQuery,
                        new AslJoin(
                                sourceField.getInternalProvider(),
                                JoinType.LEFT_OUTER_JOIN,
                                filteringQuery,
                                filterConditions));
                pathToField.replace(identifiedPath, filteringQuery.getSelect().getFirst());
            }
        }
    }

    private void addRmObjectData(
            StructureRmDataNodeInfo dni, AslRootQuery rootQuery, Map<IdentifiedPath, AslField> pathToField) {

        AslStructureQuery base = (AslStructureQuery) dni.parent().owner();
        AslQuery provider = dni.providerSubQuery();
        AslRmObjectDataQuery dataQuery = new AslRmObjectDataQuery(aliasProvider.uniqueAlias("pd"), base, provider);

        AslSubqueryField field = AslSubqueryField.createAslSubqueryField(JSONB.class, dataQuery);

        dni.node().getPathsEndingAtNode().forEach(path -> pathToField.put(path, field));
    }

    private void addExtractedColumns(
            AslRootQuery root, ExtractedColumnDataNodeInfo dni, Map<IdentifiedPath, AslField> pathToField) {
        final FieldSource fieldSource = new FieldSource(dni.parent().owner(), dni.providerSubQuery(), root);
        AslField field = createExtractedColumnField(dni.extractedColumn(), fieldSource);
        dni.node().getPathsEndingAtNode().forEach(path -> pathToField.put(path, field));
    }

    private AslField createExtractedColumnField(AslExtractedColumn ec, FieldSource fieldSource) {
        return switch (ec) {
            case NAME_VALUE,
                    TEMPLATE_ID,
                    EHR_ID,
                    ROOT_CONCEPT,
                    OV_CONTRIBUTION_ID,
                    OV_TIME_COMMITTED,
                    OV_TIME_COMMITTED_DV,
                    AD_CHANGE_TYPE_PREFERRED_TERM,
                    AD_CHANGE_TYPE_CODE_STRING,
                    AD_CHANGE_TYPE_VALUE,
                    AD_CHANGE_TYPE_DV,
                    AD_DESCRIPTION_VALUE,
                    AD_DESCRIPTION_DV,
                    EHR_TIME_CREATED,
                    EHR_TIME_CREATED_DV -> findExtractedColumnField(ec, fieldSource);
            case AD_CHANGE_TYPE_TERMINOLOGY_ID_VALUE -> new AslConstantField<>(
                    String.class, "openehr", fieldSource, ec);
            case AD_SYSTEM_ID, EHR_SYSTEM_ID, EHR_SYSTEM_ID_DV -> new AslConstantField<>(
                    String.class, systemId, fieldSource, ec);
            case VO_ID, ARCHETYPE_NODE_ID -> new AslComplexExtractedColumnField(ec, fieldSource);
        };
    }

    @Nonnull
    private static AslColumnField findExtractedColumnField(AslExtractedColumn ec, FieldSource fieldSource) {
        AslColumnField field = AslUtils.findFieldForOwner(
                        ec.getColumns().getFirst(),
                        fieldSource.internalProvider().getSelect(),
                        fieldSource.owner())
                .withProvider(fieldSource.provider());
        if (field.getExtractedColumn() == null) {
            /*
            Some extracted columns refer to fields representing multiple extracted columns.
            The field is copied, so the field represents exactly one extracted column.
            */
            field = new AslColumnField(
                    field.getType(),
                    field.getColumnName(),
                    new FieldSource(field.getOwner(), field.getInternalProvider(), field.getProvider()),
                    field.isVersionTableField(),
                    ec);
        }
        return field;
    }

    private Stream<DataNodeInfo> joinPathStructureNode(
            AslEncapsulatingQuery query,
            OwnerProviderTuple parent,
            boolean isArchetypeParent,
            JoinMode parentJoinMode,
            AslSourceRelation sourceRelation,
            PathCohesionTreeNode currentNode,
            PathInfo pathInfo,
            AslQuery rootProviderQuery,
            final int structureLevel) {

        final OwnerProviderTuple subQuery;
        final AslEncapsulatingQuery currentQuery;
        final JoinMode joinMode = pathInfo.joinMode(currentNode);
        final boolean skipNode = pathInfo.isNodeSkippable(currentNode);
        if (joinMode == JoinMode.ROOT || skipNode) {
            subQuery = parent;
            currentQuery = query;
        } else {
            AslStructureQuery sq = pathStructureSubQuery(
                    currentNode.getAttribute().getAttribute(),
                    currentNode.getAttribute().getPredicateOrOperands(),
                    sourceRelation,
                    pathInfo.getTargetTypes(currentNode));
            subQuery = new OwnerProviderTuple(sq, sq);

            if (parentJoinMode == JoinMode.INTERNAL_SINGLE_CHILD) {
                currentQuery = addInternalPathNode(query, parent, sq, currentNode);
            } else {
                currentQuery = addEncapsulatingQueryWithPathNode(query, parent, parentJoinMode, sq, currentNode);
                if (parentJoinMode == JoinMode.ROOT) {
                    rootProviderQuery = currentQuery;
                }
            }
        }

        if (!skipNode && subQuery.owner() instanceof AslStructureQuery sq) {
            addFiltersToPathNodeSubquery(currentNode, structureLevel, sq);
        }

        final AslQuery finalRootProviderSubQuery = rootProviderQuery;
        Stream<DataNodeInfo> dataNodeInfoStream = currentNode.getChildren().stream()
                .flatMap(child -> {
                    if (subQuery.owner() instanceof AslStructureQuery sq
                            && sq.isRepresentsOriginalVersionExpression()
                            && pathInfo.getTargetTypes(child).stream().anyMatch(RmConstants.AUDIT_DETAILS::equals)) {
                        // VERSION.commit_audit
                        return joinAuditDetailsPaths(currentQuery, subQuery, child, finalRootProviderSubQuery);
                    }

                    NodeCategory nodeCategory = pathInfo.getNodeCategory(child);
                    return switch (nodeCategory) {
                        case STRUCTURE -> joinPathStructureNode(
                                currentQuery,
                                subQuery,
                                skipNode && isArchetypeParent || !skipNode && pathInfo.isArchetypeNode(currentNode),
                                joinMode,
                                sourceRelation,
                                child,
                                pathInfo,
                                finalRootProviderSubQuery,
                                structureLevel + 1);
                        case STRUCTURE_INTERMEDIATE, FOUNDATION_EXTENDED -> throw new IllegalArgumentException();
                        case RM_TYPE -> joinRmTypeNode(
                                child, currentQuery, subQuery, finalRootProviderSubQuery, pathInfo, 1);
                        case FOUNDATION -> joinFoundationNode(
                                child, currentQuery, subQuery, finalRootProviderSubQuery, pathInfo, 1);
                    };
                });

        if ((joinMode == JoinMode.ROOT || joinMode == JoinMode.DATA)
                // this node only returns an RM object, if there is actually a path ending here
                && !currentNode.getPathsEndingAtNode().isEmpty()) {
            return Stream.of(
                            dataNodeInfoStream,
                            Stream.of(new StructureRmDataNodeInfo(
                                    currentNode, subQuery, currentQuery, rootProviderQuery)))
                    .flatMap(s -> s);
        } else {
            return dataNodeInfoStream;
        }
    }

    @Nonnull
    private AslEncapsulatingQuery addEncapsulatingQueryWithPathNode(
            AslEncapsulatingQuery query,
            OwnerProviderTuple parent,
            JoinMode parentJoinMode,
            AslStructureQuery sq,
            PathCohesionTreeNode currentNode) {
        final AslEncapsulatingQuery currentQuery = new AslEncapsulatingQuery(aliasProvider.uniqueAlias("p_eq"));
        currentQuery.addChild(sq, null);

        AslQuery parentProvider = parentJoinMode == JoinMode.ROOT ? parent.provider() : parent.owner();
        AslJoinCondition[] joinConditions = Stream.concat(
                        AslUtils.pathChildConditions(
                                        parentProvider, (AslStructureQuery) parent.owner(), currentQuery, sq)
                                .map(AslProvidesJoinCondition::provideJoinCondition),
                        parentFiltersAsJoinCondition(parent, currentNode).stream())
                .toArray(AslJoinCondition[]::new);
        query.addChild(
                currentQuery, new AslJoin(parent.provider(), JoinType.LEFT_OUTER_JOIN, currentQuery, joinConditions));

        if (parentJoinMode == JoinMode.INTERNAL_FORK) {
            query.addConditionOr(new AslNotNullQueryCondition(
                    AslUtils.findFieldForOwner(AslStructureColumn.VO_ID, currentQuery.getSelect(), sq)));
        }
        return currentQuery;
    }

    @Nonnull
    private static AslEncapsulatingQuery addInternalPathNode(
            AslEncapsulatingQuery query,
            OwnerProviderTuple parent,
            AslStructureQuery nodeSubquery,
            PathCohesionTreeNode currentNode) {
        List<AslJoinCondition> childNodeJoinConditions = AslUtils.<AslJoinCondition>concatStreams(
                        parentFiltersAsJoinCondition(parent, currentNode).stream(),
                        AslUtils.pathChildConditions(
                                        parent.provider(),
                                        (AslStructureQuery) parent.owner(),
                                        nodeSubquery,
                                        nodeSubquery)
                                .map(AslProvidesJoinCondition::provideJoinCondition))
                .toList();
        query.addChild(
                nodeSubquery, new AslJoin(parent.provider(), JoinType.JOIN, nodeSubquery, childNodeJoinConditions));
        return query;
    }

    private void addFiltersToPathNodeSubquery(
            PathCohesionTreeNode currentNode, int structureLevel, AslStructureQuery sq) {
        List<AndOperatorPredicate> condition1 = currentNode.getAttribute().getPredicateOrOperands();
        long attributePredicateCount = AqlUtil.streamPredicates(condition1).count();
        List<Pair<IdentifiedPath, List<AndOperatorPredicate>>> allPathPredicates = currentNode.getPaths().stream()
                .map(ip -> Pair.of(
                        ip,
                        structureLevel < 0
                                ? ListUtils.emptyIfNull(ip.getRootPredicate())
                                : ip.getPath()
                                        .getPathNodes()
                                        .get(structureLevel)
                                        .getPredicateOrOperands()))
                .toList();

        if (allPathPredicates.stream()
                .map(Pair::getRight)
                .map(AqlUtil::streamPredicates)
                .map(Stream::count)
                .anyMatch(c -> attributePredicateCount != c)) {
            allPathPredicates.forEach(p -> sq.addJoinConditionForFiltering(
                    p.getKey(),
                    AslUtils.predicates(
                                    p.getRight(),
                                    cp -> AslUtils.structurePredicateCondition(
                                            cp, sq, knowledgeCacheService::findUuidByTemplateId))
                            .orElse(new AslTrueQueryCondition())));
        }
    }

    private static Optional<AslPathFilterJoinCondition> parentFiltersAsJoinCondition(
            OwnerProviderTuple parent, PathCohesionTreeNode currentNode) {
        Map<IdentifiedPath, List<AslPathFilterJoinCondition>> filterConditions =
                parent.owner().joinConditionsForFiltering();
        if (filterConditions.isEmpty()) {
            return Optional.empty();
        }

        return AslUtils.reduceConditions(
                        LogicalConditionOperator.OR,
                        filterConditions.entrySet().stream()
                                .filter(e -> currentNode.getPaths().contains(e.getKey()))
                                .map(Entry::getValue)
                                .map(Collection::stream)
                                .map(jc -> jc.map(AslPathFilterJoinCondition::getCondition))
                                .map(AslUtils::and)
                                .filter(Objects::nonNull))
                .filter(condition -> !(condition instanceof AslTrueQueryCondition))
                .map(condition -> new AslPathFilterJoinCondition(parent.owner(), condition));
    }

    private Stream<DataNodeInfo> joinAuditDetailsPaths(
            AslEncapsulatingQuery currentQuery,
            OwnerProviderTuple parent,
            PathCohesionTreeNode currentNode,
            AslQuery rootProviderSubQuery) {
        Supplier<OwnerProviderTuple> auditDetailsParent =
                SingletonSupplier.of(() -> addAuditDetailsSubQuery(currentQuery, parent));

        Map<IdentifiedPath, PathCohesionTreeNode> pathToNode = streamCohesionTreeNodes(currentNode)
                .flatMap(n -> n.getPathsEndingAtNode().stream().map(p -> Pair.of(p, n)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        return currentNode.getPaths().stream()
                .map(ip -> Pair.of(
                        ip,
                        AslExtractedColumn.find(RmConstants.ORIGINAL_VERSION, ip.getPath())
                                // VERSION.commit_audit
                                .or(() -> AslExtractedColumn.find(RmConstants.AUDIT_DETAILS, ip.getPath(), 1))
                                .orElseThrow()))
                .map(p -> {
                    boolean isAuditDetailsColumn =
                            p.getRight().getAllowedRmTypes().contains(RmConstants.AUDIT_DETAILS);
                    return new ExtractedColumnDataNodeInfo(
                            pathToNode.get(p.getLeft()),
                            isAuditDetailsColumn ? auditDetailsParent.get() : parent,
                            isAuditDetailsColumn ? auditDetailsParent.get().owner() : rootProviderSubQuery,
                            p.getRight());
                });
    }

    private OwnerProviderTuple addAuditDetailsSubQuery(AslEncapsulatingQuery currentQuery, OwnerProviderTuple parent) {
        List<AslField> fields = Stream.of(AUDIT_DETAILS.ID, AUDIT_DETAILS.DESCRIPTION, AUDIT_DETAILS.CHANGE_TYPE)
                .map(f -> (AslField) new AslColumnField(f.getType(), f.getName(), null, false, null))
                .toList();
        AslStructureQuery auditDetailsQuery = new AslStructureQuery(
                aliasProvider.uniqueAlias("p_ca"),
                AslSourceRelation.AUDIT_DETAILS,
                fields,
                Set.of(RmConstants.AUDIT_DETAILS),
                Set.of(RmConstants.AUDIT_DETAILS),
                null,
                false,
                false,
                false);

        currentQuery.addChild(
                auditDetailsQuery,
                new AslJoin(
                        parent.provider(),
                        JoinType.JOIN,
                        auditDetailsQuery,
                        new AslFieldFieldQueryCondition(
                                        AslUtils.findFieldForOwner(
                                                AslStructureColumn.AUDIT_ID,
                                                parent.owner().getSelect(),
                                                parent.owner()),
                                        AslConditionOperator.EQ,
                                        AslUtils.findFieldForOwner(
                                                AUDIT_DETAILS.ID.getName(),
                                                auditDetailsQuery.getSelect(),
                                                auditDetailsQuery))
                                .provideJoinCondition()));
        return new OwnerProviderTuple(auditDetailsQuery, auditDetailsQuery);
    }

    private static Stream<PathCohesionTreeNode> streamCohesionTreeNodes(PathCohesionTreeNode node) {
        return Stream.of(Stream.of(node), node.getChildren().stream().flatMap(AslPathCreator::streamCohesionTreeNodes))
                .flatMap(Function.identity());
    }

    private static Stream<DataNodeInfo> streamJsonRmDataNodes(
            PathCohesionTreeNode currentNode,
            OwnerProviderTuple subQuery,
            AslEncapsulatingQuery query,
            AslQuery rootProviderSubQuery,
            PathInfo pathInfo,
            Stream<DataNodeInfo> dependentNodes,
            int levelInJson) {

        boolean multipleValued = pathInfo.isMultipleValued(currentNode);
        boolean pathsEndingAtNode = !currentNode.getPathsEndingAtNode().isEmpty();

        if (!pathsEndingAtNode && !multipleValued) {
            return Stream.empty();
        }

        List<PathNode> pathToNode = pathInfo.getPathToNode(currentNode);
        Class<?> fieldType = Set.of("STRING").equals(pathInfo.getTargetTypes(currentNode)) ? String.class : JSONB.class;

        return Stream.of(new JsonRmDataNodeInfo(
                currentNode,
                subQuery,
                query,
                rootProviderSubQuery,
                pathToNode.subList(pathToNode.size() - levelInJson, pathToNode.size()),
                pathInfo.isMultipleValued(currentNode),
                dependentNodes,
                pathInfo.getDvOrderedTypes(currentNode),
                fieldType));
    }

    private static Stream<DataNodeInfo> joinRmTypeNode(
            PathCohesionTreeNode currentNode,
            AslEncapsulatingQuery query,
            OwnerProviderTuple parentStructureQuery,
            AslQuery rootProviderQuery,
            PathInfo pathInfo,
            int levelInJson) {

        boolean multipleValued = pathInfo.isMultipleValued(currentNode);
        int nextLevelInJson = multipleValued ? 1 : (levelInJson + 1);
        OwnerProviderTuple parent = multipleValued ? null : parentStructureQuery;
        Stream<DataNodeInfo> childNodes = currentNode.getChildren().stream().flatMap(child -> {
            NodeCategory nodeCategory = pathInfo.getNodeCategory(child);
            return switch (nodeCategory) {
                case STRUCTURE, STRUCTURE_INTERMEDIATE -> throw new IllegalArgumentException();
                case RM_TYPE, FOUNDATION_EXTENDED -> joinRmTypeNode(
                        child, query, parent, rootProviderQuery, pathInfo, nextLevelInJson);
                case FOUNDATION -> joinFoundationNode(
                        child, query, parent, rootProviderQuery, pathInfo, nextLevelInJson);
            };
        });

        return Stream.of(
                        streamJsonRmDataNodes(
                                currentNode,
                                parentStructureQuery,
                                query,
                                rootProviderQuery,
                                pathInfo,
                                multipleValued ? childNodes : Stream.empty(),
                                levelInJson),
                        multipleValued ? Stream.<DataNodeInfo>empty() : childNodes)
                .flatMap(s -> s);
    }

    private static Stream<DataNodeInfo> joinFoundationNode(
            PathCohesionTreeNode currentNode,
            AslEncapsulatingQuery query,
            OwnerProviderTuple parentStructureQuery,
            AslQuery rootProviderQuery,
            PathInfo pathInfo,
            int levelInJson) {
        AslQuery parent = Optional.ofNullable(parentStructureQuery)
                .map(OwnerProviderTuple::owner)
                .orElse(null);
        Optional<DataNodeInfo> extractedColumnInfo = (parent instanceof AslStructureQuery sq)
                ? Stream.of(AslExtractedColumn.values())
                        .filter(ec -> ec.getAllowedRmTypes().stream()
                                .anyMatch(t -> sq.getRmTypes().contains(t)
                                        || (sq.isRepresentsOriginalVersionExpression()
                                                && RmConstants.ORIGINAL_VERSION.equals(t))))
                        .filter(ec -> levelInJson == ec.getPath().getPathNodes().size())
                        .filter(ec -> currentNode.getPaths().stream()
                                .allMatch(p -> p.getPath().endsWith(ec.getPath())))
                        .findFirst()
                        .map(ec -> new ExtractedColumnDataNodeInfo(
                                currentNode, parentStructureQuery, rootProviderQuery, ec))
                : Optional.empty();

        if (extractedColumnInfo.isPresent()) {
            return extractedColumnInfo.stream();
        } else {
            return streamJsonRmDataNodes(
                    currentNode, parentStructureQuery, query, rootProviderQuery, pathInfo, Stream.empty(), levelInJson);
        }
    }

    private AslStructureQuery pathStructureSubQuery(
            String attribute,
            List<AndOperatorPredicate> attributePredicates,
            AslSourceRelation sourceRelation,
            Collection<String> rmTypes) {

        final List<AslField> fields = Arrays.stream(AslStructureColumn.values())
                // remove fields not supported by the relation
                .filter(c -> sourceRelation.getDataTable().field(c.getFieldName()) != null)
                .map(AslStructureColumn::field)
                .collect(Collectors.toList());
        fields.add(new AslColumnField(String.class, AslStructureQuery.ENTITY_ATTRIBUTE, false));

        final String sqAlias = aliasProvider.uniqueAlias("p_" + attribute + "_");
        AslStructureQuery aslStructureQuery = new AslStructureQuery(
                sqAlias, sourceRelation, fields, rmTypes, List.of(), attribute, false, false, false);

        AslUtils.predicates(attributePredicates, cp -> pathStructurePredicateCondition(cp, aslStructureQuery))
                .ifPresent(aslStructureQuery::addConditionAnd);

        return aslStructureQuery;
    }

    @Nonnull
    private static AslFieldValueQueryCondition<?> pathStructurePredicateCondition(
            ComparisonOperatorPredicate cp, AslStructureQuery aslStructureQuery) {
        String value = ((StringPrimitive) cp.getValue()).getValue();
        if (AqlObjectPathUtil.ARCHETYPE_NODE_ID.equals(cp.getPath())) {
            return new AslFieldValueQueryCondition<>(
                    AslComplexExtractedColumnField.archetypeNodeIdField(FieldSource.withOwner(aslStructureQuery)),
                    AslConditionOperator.EQ,
                    List.of(AslRmTypeAndConcept.fromArchetypeNodeId(value)));
        } else if (AqlObjectPathUtil.NAME_VALUE.equals(cp.getPath())) {
            return new AslFieldValueQueryCondition<>(
                    AslUtils.findFieldForOwner(
                            AslStructureColumn.ENTITY_NAME, aslStructureQuery.getSelect(), aslStructureQuery),
                    AslConditionOperator.EQ,
                    List.of(value));
        } else {
            throw new IllegalArgumentException("Unexpected attribute predicate path: %s".formatted(cp.getPath()));
        }
    }
}
