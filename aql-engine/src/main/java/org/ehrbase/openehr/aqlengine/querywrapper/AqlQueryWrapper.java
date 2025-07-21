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
package org.ehrbase.openehr.aqlengine.querywrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathInfo;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsChain;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsSetOperationWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.RmContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.VersionContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.orderby.OrderByWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ComparisonOperatorConditionWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.LogicalOperatorConditionWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.condition.ComparisonOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.ExistsCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.LikeCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.LogicalOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.MatchesCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.NotCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.WhereCondition;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.Containment;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperator;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentVersionExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.orderby.OrderByExpression;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;
import org.ehrbase.openehr.sdk.aql.util.AqlUtil;

/**
 * A wrapper for the AqlQuery providing context and convenience methods.
 */
public final class AqlQueryWrapper {
    private final boolean distinct;
    private final List<SelectWrapper> selects;
    private final ContainsChain containsChain;
    private final ConditionWrapper where;
    private final List<OrderByWrapper> orderBy;
    private final Long limit;
    private final Long offset;
    private final Map<ContainsWrapper, PathInfo> pathInfos;

    /**
     * @param distinct
     * @param selects
     * @param containsChain
     * @param where
     * @param orderBy
     * @param limit
     * @param offset
     * @param pathInfos
     */
    public AqlQueryWrapper(
            boolean distinct,
            List<SelectWrapper> selects,
            ContainsChain containsChain,
            ConditionWrapper where,
            List<OrderByWrapper> orderBy,
            Long limit,
            Long offset,
            Map<ContainsWrapper, PathInfo> pathInfos) {
        this.distinct = distinct;
        this.selects = selects;
        this.containsChain = containsChain;
        this.where = where;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
        this.pathInfos = pathInfos;
    }

    public Stream<SelectWrapper> nonPrimitiveSelects() {
        return selects.stream().filter(sd -> sd.type() != SelectWrapper.SelectType.PRIMITIVE);
    }

    /**
     * Provides a wrapper for the AqlQuery providing context and convenience methods.
     *
     * @param aqlQuery
     * @param enableNodeSkipping
     * @return
     */
    public static AqlQueryWrapper create(AqlQuery aqlQuery, final boolean enableNodeSkipping) {
        Map<AbstractContainmentExpression, ContainsWrapper> containsDescs = new LinkedHashMap<>();

        ContainsChain fromClause;
        {
            AbstractContainmentExpression fromRoot = (AbstractContainmentExpression) aqlQuery.getFrom();
            AqlUtil.streamContainments(fromRoot)
                    .filter(ContainmentClassExpression.class::isInstance)
                    .map(ContainmentClassExpression.class::cast)
                    .forEach(c -> containsDescs.put(c, new RmContainsWrapper(c)));
            // Version descriptors require the descriptor of the child
            AqlUtil.streamContainments(fromRoot)
                    .filter(ContainmentVersionExpression.class::isInstance)
                    .map(ContainmentVersionExpression.class::cast)
                    .forEach(c -> containsDescs.put(c, new VersionContainsWrapper(c.getIdentifier(), (RmContainsWrapper)
                            containsDescs.get(c.getContains()))));

            fromClause = buildContainsChain(fromRoot, containsDescs);
        }

        List<SelectWrapper> selects = aqlQuery.getSelect().getStatement().stream()
                .map(s -> buildSelectDescriptor(containsDescs, s))
                .toList();

        ConditionWrapper where = Optional.of(aqlQuery)
                .map(AqlQuery::getWhere)
                .map(w -> buildWhereDescriptor(w, containsDescs, false))
                .orElse(null);

        List<OrderByWrapper> orderBy = CollectionUtils.emptyIfNull(aqlQuery.getOrderBy()).stream()
                .map(o -> buildOrderByDescriptor(o, containsDescs))
                .toList();

        Map<ContainsWrapper, PathInfo> pathInfos =
                PathInfo.createPathInfos(aqlQuery, containsDescs, enableNodeSkipping);

        return new AqlQueryWrapper(
                aqlQuery.getSelect().isDistinct(),
                selects,
                fromClause,
                where,
                orderBy,
                aqlQuery.getLimit(),
                aqlQuery.getOffset(),
                pathInfos);
    }

    private static OrderByWrapper buildOrderByDescriptor(
            OrderByExpression expression, Map<AbstractContainmentExpression, ContainsWrapper> containsDescs) {
        // TODO: expression.statement.rootPredicate once we support them
        return new OrderByWrapper(
                expression.getStatement(),
                expression.getSymbol(),
                containsDescs.get(expression.getStatement().getRoot()));
    }

    private static ContainsChain buildContainsChain(
            Containment root, Map<AbstractContainmentExpression, ContainsWrapper> containsDescs) {
        final List<ContainsWrapper> chain = new ArrayList<>();
        final ContainsSetOperationWrapper setOperator;

        Containment next = root;
        while (next instanceof AbstractContainmentExpression c) {

            chain.add(containsDescs.get(next));
            if (next instanceof ContainmentVersionExpression) {
                // Version descriptor represents itself and its child, so the child itself is not added
                next = ((AbstractContainmentExpression) c.getContains()).getContains();
            } else {
                next = c.getContains();
            }
        }

        if (next instanceof ContainmentSetOperator o) {
            setOperator = new ContainsSetOperationWrapper(
                    o.getSymbol(),
                    o.getValues().stream()
                            .map(c -> buildContainsChain(c, containsDescs))
                            .toList());
        } else {
            setOperator = null;
        }

        return new ContainsChain(chain, setOperator);
    }

    private static SelectWrapper buildSelectDescriptor(
            Map<AbstractContainmentExpression, ContainsWrapper> containsDescs, SelectExpression s) {
        Pair<SelectWrapper.SelectType, IdentifiedPath> typeAndPath =
                switch (s.getColumnExpression()) {
                    case IdentifiedPath i -> Pair.of(SelectWrapper.SelectType.PATH, i);
                    case AggregateFunction af -> Pair.of(
                            SelectWrapper.SelectType.AGGREGATE_FUNCTION, af.getIdentifiedPath());
                    case Primitive __ -> Pair.of(SelectWrapper.SelectType.PRIMITIVE, null);
                    default -> throw new IllegalArgumentException("Unknown ColumnExpression type in SELECT");
                };
        return new SelectWrapper(
                s,
                typeAndPath.getLeft(),
                Optional.of(typeAndPath)
                        .map(Pair::getRight)
                        .map(IdentifiedPath::getRoot)
                        .map(containsDescs::get)
                        .orElse(null));
    }

    private static ConditionWrapper buildWhereDescriptor(
            WhereCondition where, Map<AbstractContainmentExpression, ContainsWrapper> containsDescs, boolean negate) {
        return switch (where) {
            case ComparisonOperatorCondition c -> new ComparisonOperatorConditionWrapper(
                    new ComparisonOperatorConditionWrapper.IdentifiedPathWrapper(
                            containsDescs.get(((IdentifiedPath) c.getStatement()).getRoot()),
                            (IdentifiedPath) c.getStatement()),
                    ConditionWrapper.ComparisonConditionOperator.valueOf(
                            c.getSymbol().name(), negate),
                    (Primitive) c.getValue());
            case MatchesCondition c -> negate
                    ? new LogicalOperatorConditionWrapper(
                            ConditionWrapper.LogicalConditionOperator.AND,
                            c.getValues().stream()
                                    .map(Primitive.class::cast)
                                    .map(v -> (ConditionWrapper) new ComparisonOperatorConditionWrapper(
                                            new ComparisonOperatorConditionWrapper.IdentifiedPathWrapper(
                                                    containsDescs.get(
                                                            c.getStatement().getRoot()),
                                                    c.getStatement()),
                                            ConditionWrapper.ComparisonConditionOperator.NEQ,
                                            v))
                                    .toList())
                    : new ComparisonOperatorConditionWrapper(
                            new ComparisonOperatorConditionWrapper.IdentifiedPathWrapper(
                                    containsDescs.get(c.getStatement().getRoot()), c.getStatement()),
                            ConditionWrapper.ComparisonConditionOperator.MATCHES,
                            c.getValues().stream().map(Primitive.class::cast).toList());
            case LikeCondition c -> {
                ComparisonOperatorConditionWrapper condition = new ComparisonOperatorConditionWrapper(
                        new ComparisonOperatorConditionWrapper.IdentifiedPathWrapper(
                                containsDescs.get(c.getStatement().getRoot()), c.getStatement()),
                        ConditionWrapper.ComparisonConditionOperator.LIKE,
                        (Primitive) c.getValue());
                yield negate
                        ? new LogicalOperatorConditionWrapper(
                                ConditionWrapper.LogicalConditionOperator.NOT, List.of(condition))
                        : condition;
            }
            case ExistsCondition c -> {
                ComparisonOperatorConditionWrapper comparisonOperatorConditionDescriptor =
                        new ComparisonOperatorConditionWrapper(
                                new ComparisonOperatorConditionWrapper.IdentifiedPathWrapper(
                                        containsDescs.get(c.getValue().getRoot()), c.getValue()),
                                ConditionWrapper.ComparisonConditionOperator.EXISTS,
                                List.of());
                yield negate
                        ? new LogicalOperatorConditionWrapper(
                                ConditionWrapper.LogicalConditionOperator.NOT,
                                List.of(comparisonOperatorConditionDescriptor))
                        : comparisonOperatorConditionDescriptor;
            }
            case LogicalOperatorCondition c -> new LogicalOperatorConditionWrapper(
                    switch (c.getSymbol()) {
                        case OR -> negate
                                ? ConditionWrapper.LogicalConditionOperator.AND
                                : ConditionWrapper.LogicalConditionOperator.OR;
                        case AND -> negate
                                ? ConditionWrapper.LogicalConditionOperator.OR
                                : ConditionWrapper.LogicalConditionOperator.AND;
                    },
                    c.getValues().stream()
                            .map(w -> buildWhereDescriptor(w, containsDescs, negate))
                            .toList());
            case NotCondition c -> buildWhereDescriptor(c.getConditionDto(), containsDescs, !negate);
            case null -> throw new IllegalArgumentException(
                    "Encountered null reference instead of WhereCondition object");
            default -> throw new IllegalArgumentException(
                    "Unknown WhereCondition class: %s".formatted(where.getClass()));
        };
    }

    public boolean distinct() {
        return distinct;
    }

    public List<SelectWrapper> selects() {
        return selects;
    }

    public ContainsChain containsChain() {
        return containsChain;
    }

    public ConditionWrapper where() {
        return where;
    }

    public List<OrderByWrapper> orderBy() {
        return orderBy;
    }

    public Long limit() {
        return limit;
    }

    public Long offset() {
        return offset;
    }

    public Map<ContainsWrapper, PathInfo> pathInfos() {
        return pathInfos;
    }

    @Override
    public String toString() {
        return "AqlQueryWrapper[" + "distinct="
                + distinct + ", " + "selects="
                + selects + ", " + "containsChain="
                + containsChain + ", " + "where="
                + where + ", " + "orderBy="
                + orderBy + ", " + "limit="
                + limit + ", " + "offset="
                + offset + ", " + "pathInfos="
                + pathInfos + ']';
    }
}
