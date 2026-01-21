/*
 * Copyright (c) 2025 vitasystems GmbH.
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.openehr.aqlengine.aql.model.ListPredicateOperand;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.condition.ComparisonOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.ComparisonOperatorSymbol;
import org.ehrbase.openehr.sdk.aql.dto.condition.LogicalOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.LogicalOperatorCondition.ConditionLogicalOperatorSymbol;
import org.ehrbase.openehr.sdk.aql.dto.condition.MatchesCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.WhereCondition;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.PathPredicateOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate.PredicateComparisonOperator;
import org.springframework.stereotype.Component;

@Component
public class AqlConditionAsPredicatePostProcessor implements AqlQueryParsingPostProcessor {
    private static final Set<AslExtractedColumn> SUPPORTED_EXCTRACTED_COLUMNS = EnumSet.of(
            AslExtractedColumn.EHR_ID,
            AslExtractedColumn.VO_ID,
            AslExtractedColumn.ARCHETYPE_NODE_ID,
            AslExtractedColumn.ROOT_CONCEPT,
            AslExtractedColumn.TEMPLATE_ID);

    @Override
    public void afterParseAql(final AqlQuery aqlQuery, final AqlQueryRequest request, final AqlQueryContext ctx) {
        aqlQuery.setWhere(moveConditions(aqlQuery.getWhere()));
    }

    private WhereCondition moveConditions(WhereCondition where) {
        return switch (where) {
            case ComparisonOperatorCondition c
            when c.getSymbol() == ComparisonOperatorSymbol.EQ
                    && c.getValue() instanceof PathPredicateOperand<?> val
                    && c.getStatement() instanceof IdentifiedPath ip
                    && ip.getRoot() instanceof ContainmentClassExpression cce
                    && isSupported(ip, cce) -> {
                addPredicate(ip, cce, val);
                yield null;
            }
            case LogicalOperatorCondition c
            when c.getSymbol() == ConditionLogicalOperatorSymbol.AND && CollectionUtils.isNotEmpty(c.getValues()) -> {
                List<WhereCondition> values = c.getValues();
                for (int i = 0; i < values.size(); i++) {
                    WhereCondition condition = values.get(i);
                    if (moveConditions(condition) == null) {
                        values.remove(i--);
                    }
                }
                yield c;
            }
            case MatchesCondition c
            when c.getStatement() instanceof IdentifiedPath ip
                    && ip.getRoot() instanceof ContainmentClassExpression cce
                    && isSupported(ip, cce) -> {
                PathPredicateOperand<?> val = new ListPredicateOperand<>(c.getValues().stream()
                        .filter(Primitive.class::isInstance)
                        .map(Primitive.class::cast)
                        .toList());
                addPredicate(ip, cce, val);
                yield null;
            }
            case null, default -> where;
        };
    }

    private boolean isSupported(final IdentifiedPath ip, final ContainmentClassExpression cce) {
        return AslExtractedColumn.find(cce.getType(), ip.getPath())
                .filter(SUPPORTED_EXCTRACTED_COLUMNS::contains)
                .isPresent();
    }

    private void addPredicate(
            final IdentifiedPath ip, final ContainmentClassExpression cce, final PathPredicateOperand<?> val) {
        ComparisonOperatorPredicate predicate =
                new ComparisonOperatorPredicate(ip.getPath(), PredicateComparisonOperator.EQ, val);
        if (CollectionUtils.isEmpty(cce.getPredicates())) {
            List<AndOperatorPredicate> predicates = new ArrayList<>();
            AndOperatorPredicate andOperatorPredicate = new AndOperatorPredicate(new ArrayList<>());
            andOperatorPredicate.getOperands().add(predicate);
            predicates.add(andOperatorPredicate);
            cce.setPredicates(predicates);
        } else {
            cce.getPredicates()
                    .forEach(andOperatorPredicate ->
                            andOperatorPredicate.getOperands().add(predicate));
        }
    }

    @Override
    public int getOrder() {
        return PKEY_CONDITION_AS_PREDICATE_PRECEDENCE;
    }
}
