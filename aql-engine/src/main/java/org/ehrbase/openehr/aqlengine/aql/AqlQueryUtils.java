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
package org.ehrbase.openehr.aqlengine.aql;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.condition.ComparisonOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.ExistsCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.LikeCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.LogicalOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.MatchesCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.NotCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.WhereCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction;
import org.ehrbase.openehr.sdk.aql.dto.operand.ColumnExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.ComparisonLeftOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.Operand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.QueryParameter;
import org.ehrbase.openehr.sdk.aql.dto.operand.SingleRowFunction;
import org.ehrbase.openehr.sdk.aql.dto.orderby.OrderByExpression;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;

public final class AqlQueryUtils {
    private AqlQueryUtils() {}

    public static Stream<IdentifiedPath> allIdentifiedPaths(AqlQuery query) {

        return Stream.of(
                        query.getSelect().getStatement().stream().flatMap(AqlQueryUtils::allIdentifiedPaths),
                        streamWhereConditions(query.getWhere()).flatMap(AqlQueryUtils::allIdentifiedPaths),
                        Optional.of(query).map(AqlQuery::getOrderBy).stream()
                                .flatMap(Collection::stream)
                                .map(OrderByExpression::getStatement))
                .flatMap(s -> s);
    }

    public static Stream<IdentifiedPath> allIdentifiedPaths(WhereCondition w) {
        if (w instanceof ComparisonOperatorCondition c) {
            return Stream.concat(allIdentifiedPaths(c.getStatement()), allIdentifiedPaths(c.getValue()));
        } else if (w instanceof MatchesCondition c) {
            return Stream.of(c.getStatement());
        } else if (w instanceof LikeCondition c) {
            return Stream.of(c.getStatement());
        } else if (w instanceof ExistsCondition c) {
            // XXX Should this be included in the analysis?
            return Stream.of(c.getValue());
        } else {
            throw new IllegalArgumentException("Unsupported type of " + w);
        }
    }

    public static Stream<IdentifiedPath> allIdentifiedPaths(SelectExpression selectExpression) {
        ColumnExpression columnExpression = selectExpression.getColumnExpression();
        if (columnExpression instanceof Primitive) {
            return Stream.empty();
        } else if (columnExpression instanceof AggregateFunction f) {
            return Optional.of(f).map(AggregateFunction::getIdentifiedPath).stream();
        } else if (columnExpression instanceof IdentifiedPath ip) {
            return Stream.of(ip);
        } else if (columnExpression instanceof SingleRowFunction f) {
            return f.getOperandList().stream().flatMap(AqlQueryUtils::allIdentifiedPaths);
        } else {
            throw new IllegalArgumentException("Unsupported type of " + columnExpression);
        }
    }

    public static Stream<IdentifiedPath> allIdentifiedPaths(Operand operand) {
        if (operand instanceof Primitive) {
            return Stream.empty();
        } else if (operand instanceof QueryParameter) {
            return Stream.empty();
        } else if (operand instanceof IdentifiedPath ip) {
            return Stream.of(ip);
        } else if (operand instanceof SingleRowFunction f) {
            return f.getOperandList().stream().flatMap(AqlQueryUtils::allIdentifiedPaths);
        } else {
            throw new IllegalArgumentException("Unsupported type of " + operand);
        }
    }

    public static Stream<IdentifiedPath> allIdentifiedPaths(ComparisonLeftOperand operand) {
        if (operand instanceof IdentifiedPath ip) {
            return Stream.of(ip);
        } else if (operand instanceof SingleRowFunction f) {
            return f.getOperandList().stream().flatMap(AqlQueryUtils::allIdentifiedPaths);
        } else {
            throw new IllegalArgumentException("Unsupported type of " + operand);
        }
    }

    public static Stream<WhereCondition> streamWhereConditions(WhereCondition condition) {
        if (condition == null) {
            return Stream.empty();
        }
        return Stream.of(condition).flatMap(c -> {
            if (c instanceof ComparisonOperatorCondition
                    || c instanceof MatchesCondition
                    || c instanceof LikeCondition
                    || c instanceof ExistsCondition) {
                return Stream.of(c);
            } else if (c instanceof LogicalOperatorCondition logical) {
                return logical.getValues().stream().flatMap(AqlQueryUtils::streamWhereConditions);
            } else if (c instanceof NotCondition not) {
                return streamWhereConditions(not.getConditionDto());
            } else {
                throw new IllegalStateException("Unsupported condition type %s".formatted(c.getClass()));
            }
        });
    }
}
