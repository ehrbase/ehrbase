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

import static org.ehrbase.openehr.sdk.util.rmconstants.RmConstants.DV_DATE;
import static org.ehrbase.openehr.sdk.util.rmconstants.RmConstants.DV_DATE_TIME;
import static org.ehrbase.openehr.sdk.util.rmconstants.RmConstants.DV_DURATION;
import static org.ehrbase.openehr.sdk.util.rmconstants.RmConstants.DV_TIME;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDuration;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.openehr.aqlengine.ChangeTypeUtils;
import org.ehrbase.openehr.aqlengine.asl.AslUtils.AliasProvider;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslDvOrderedValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFalseQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotNullQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition.AslConditionOperator;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslTrueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslAggregatingField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslDvOrderedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper.SelectType;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ComparisonOperatorConditionWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper.ComparisonConditionOperator;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper.LogicalConditionOperator;
import org.ehrbase.openehr.aqlengine.querywrapper.where.LogicalOperatorConditionWrapper;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction.AggregateFunctionName;
import org.ehrbase.openehr.sdk.aql.dto.operand.DoublePrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.LongPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.TemporalPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.orderby.OrderByExpression.OrderByDirection;
import org.ehrbase.openehr.sdk.util.OpenEHRDateTimeSerializationUtils;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.jooq.SortOrder;
import org.springframework.stereotype.Component;

@Component
public class AqlSqlLayer {

    private static final Set<String> NUMERIC_DV_ORDERED_TYPES = Set.of(
            RmConstants.DV_ORDINAL,
            RmConstants.DV_SCALE,
            RmConstants.DV_PROPORTION,
            RmConstants.DV_COUNT,
            RmConstants.DV_QUANTITY);

    private final KnowledgeCacheService knowledgeCache;
    private final SystemService systemService;

    public AqlSqlLayer(KnowledgeCacheService knowledgeCache, SystemService systemService) {
        this.knowledgeCache = knowledgeCache;
        this.systemService = systemService;
    }

    public AslRootQuery buildAslRootQuery(AqlQueryWrapper query) {

        AliasProvider aliasProvider = new AliasProvider();
        AslRootQuery aslQuery = new AslRootQuery();

        // FROM
        AslFromCreator.ContainsToOwnerProvider containsToStructureSubquery =
                new AslFromCreator(aliasProvider, knowledgeCache).addFromClause(aslQuery, query);

        // Paths
        final AslPathCreator.PathToField pathToField = new AslPathCreator(
                        aliasProvider, knowledgeCache, systemService.getSystemId())
                .addPathQueries(query, containsToStructureSubquery, aslQuery);

        // SELECT
        if (query.nonPrimitiveSelects().findAny().isEmpty()) {
            addSyntheticSelect(query, containsToStructureSubquery, aslQuery);
        } else {
            boolean usesAggregateFunction = addSelect(query, pathToField, aslQuery);
            addOrderBy(query, pathToField, aslQuery, usesAggregateFunction);
        }

        // WHERE
        Optional.of(query)
                .map(AqlQueryWrapper::where)
                .flatMap(w -> buildWhereCondition(w, pathToField))
                .ifPresent(aslQuery::addConditionAnd);

        // LIMIT
        aslQuery.setLimit(query.limit());
        aslQuery.setOffset(query.offset());

        return aslQuery;
    }

    private static void addOrderBy(
            AqlQueryWrapper query,
            AslPathCreator.PathToField pathToField,
            AslRootQuery rootQuery,
            boolean usesAggregateFunction) {
        CollectionUtils.emptyIfNull(query.orderBy())
                .forEach(o -> rootQuery.addOrderBy(
                        pathToField.getField(o.identifiedPath()),
                        o.direction() == OrderByDirection.DESC ? SortOrder.DESC : SortOrder.ASC,
                        query.distinct() || usesAggregateFunction));
    }

    /**
     *
     * @param query
     * @param pathToField
     * @param rootQuery
     * @return if the select contains aggregate functions
     */
    private static boolean addSelect(
            AqlQueryWrapper query, AslPathCreator.PathToField pathToField, AslRootQuery rootQuery) {
        // SELECT
        query.nonPrimitiveSelects()
                .map(select -> switch (select.type()) {
                    case PATH -> pathToField.getField(select.getIdentifiedPath().orElseThrow());

                    case AGGREGATE_FUNCTION -> new AslAggregatingField(
                            select.getAggregateFunctionName(),
                            // identified path is null for COUNT(*)
                            pathToField.getField(select.getIdentifiedPath().orElse(null)),
                            select.isCountDistinct());
                    case PRIMITIVE, FUNCTION -> throw new IllegalArgumentException();
                })
                .forEach(rootQuery.getSelect()::add);

        // GROUP BY is determined by the aggregate functions in the select
        boolean usesAggregateFunction =
                query.nonPrimitiveSelects().anyMatch(s -> s.type() == SelectType.AGGREGATE_FUNCTION);
        if (usesAggregateFunction) {
            rootQuery
                    .getGroupByFields()
                    .addAll(query.nonPrimitiveSelects()
                            .filter(s -> s.type() != SelectType.AGGREGATE_FUNCTION)
                            .map(SelectWrapper::getIdentifiedPath)
                            .flatMap(Optional::stream)
                            .map(pathToField::getField)
                            .flatMap(aslField -> aslField.fieldsForAggregation(rootQuery))
                            .distinct()
                            .toList());

        } else if (query.distinct()) {
            // DISTINCT: group by all selects
            rootQuery
                    .getGroupByFields()
                    .addAll(rootQuery.getSelect().stream()
                            .flatMap(aslField -> aslField.fieldsForAggregation(rootQuery))
                            .distinct()
                            .toList());
        }
        return usesAggregateFunction;
    }

    /**
     * If a query only selects constants, the number of results is only counted.
     * Later, when creating the result set, this determines the number of identical rows
     * that are returned.
     *
     * @param query
     * @param containsToStructureSubQuery
     * @param rootQuery
     */
    private static void addSyntheticSelect(
            AqlQueryWrapper query,
            AslFromCreator.ContainsToOwnerProvider containsToStructureSubQuery,
            AslRootQuery rootQuery) {
        AslQuery ownerForSyntheticSelect = containsToStructureSubQuery
                // We can get the first since the first chain always must have at least one entry
                .get(query.containsChain().chain().getFirst())
                .owner();
        AslColumnField field = rootQuery.getAvailableFields().stream()
                .filter(AslColumnField.class::isInstance)
                .map(AslColumnField.class::cast)
                .filter(f -> f.getOwner() == ownerForSyntheticSelect)
                .filter(f -> StringUtils.equalsAny(f.getColumnName(), "id", AslStructureColumn.VO_ID.getFieldName()))
                .findFirst()
                .orElseThrow();
        rootQuery.getSelect().add(new AslAggregatingField(AggregateFunctionName.COUNT, field, false));
    }

    private Optional<AslQueryCondition> buildWhereCondition(
            ConditionWrapper condition, AslPathCreator.PathToField pathToField) {

        return switch (condition) {
            case LogicalOperatorConditionWrapper lcd -> logicalOperatorCondition(
                    lcd, c -> buildWhereCondition(c, pathToField));
            case ComparisonOperatorConditionWrapper comparison -> {
                AslField aslField =
                        pathToField.getField(comparison.leftComparisonOperand().path());
                if (aslField == null) {
                    throw new IllegalArgumentException("unknown field: %s"
                            .formatted(comparison
                                    .leftComparisonOperand()
                                    .path()
                                    .getPath()
                                    .render()));
                }

                if (aslField instanceof AslDvOrderedColumnField dvOrderedField) {
                    yield buildDvOrderedCondition(
                            dvOrderedField,
                            dvOrderedField.getDvOrderedTypes(),
                            comparison.operator(),
                            comparison.rightComparisonOperands());
                } else if (aslField instanceof AslRmPathField pathField
                        && !pathField.getDvOrderedTypes().isEmpty()) {
                    yield buildDvOrderedCondition(
                            pathField,
                            pathField.getDvOrderedTypes(),
                            comparison.operator(),
                            comparison.rightComparisonOperands());
                } else {
                    yield fieldValueQueryCondition(aslField, comparison);
                }
            }
        };
    }

    @Nonnull
    private static Optional<AslQueryCondition> logicalOperatorCondition(
            LogicalOperatorConditionWrapper condition,
            Function<ConditionWrapper, Optional<AslQueryCondition>> conditionBuilder) {

        Stream<AslQueryCondition> operands =
                condition.logicalOperands().stream().map(conditionBuilder).flatMap(Optional::stream);

        if (condition.operator() == LogicalConditionOperator.NOT) {
            return Optional.of(LogicalConditionOperator.NOT.build(operands.toList()));
        } else {
            return AslUtils.reduceConditions(condition.operator(), operands);
        }
    }

    @Nonnull
    private Optional<AslQueryCondition> fieldValueQueryCondition(
            AslField aslField, ComparisonOperatorConditionWrapper comparison) {
        ComparisonConditionOperator operator = comparison.operator();
        return Optional.of(
                switch (operator) {
                    case EXISTS -> aslField.getExtractedColumn() != null
                            ? new AslTrueQueryCondition()
                            : new AslNotNullQueryCondition(aslField);
                    case LIKE, MATCHES, EQ, GT_EQ, GT, LT_EQ, LT, NEQ -> {
                        List<?> values = whereConditionValues(aslField, comparison, operator);
                        if (values.isEmpty()) {
                            yield switch (operator.getAslOperator()) {
                                case AslConditionOperator.IN,
                                        AslConditionOperator.EQ,
                                        AslConditionOperator.LIKE -> new AslFalseQueryCondition();
                                case AslConditionOperator.NEQ -> new AslTrueQueryCondition();
                                default -> throw new IllegalArgumentException(
                                        "Unexpected operator %s".formatted(operator.getAslOperator()));
                            };
                        } else {
                            yield new AslFieldValueQueryCondition<>(aslField, operator.getAslOperator(), values);
                        }
                    }
                });
    }

    private List<?> whereConditionValues(
            AslField aslField, ComparisonOperatorConditionWrapper comparison, ComparisonConditionOperator operator) {
        return switch (aslField.getExtractedColumn()) {
            case TEMPLATE_ID -> AslUtils.templateIdConditionValues(
                    comparison.rightComparisonOperands(), operator, knowledgeCache::findUuidByTemplateId);
            case ARCHETYPE_NODE_ID -> AslUtils.archetypeNodeIdConditionValues(
                    comparison.rightComparisonOperands(), operator);
            case ROOT_CONCEPT -> AslUtils.archetypeNodeIdConditionValues(comparison.rightComparisonOperands(), operator)
                    .stream()
                    // archetype must be for COMPOSITION
                    .filter(tc -> StructureRmType.COMPOSITION.getAlias().equals(tc.aliasedRmType()))
                    .map(AslRmTypeAndConcept::concept)
                    .toList();
            case OV_TIME_COMMITTED_DV, EHR_TIME_CREATED_DV -> AslUtils.streamStringPrimitives(comparison)
                    .map(AslUtils::toOffsetDateTime)
                    .filter(Objects::nonNull)
                    .toList();
            case AD_CHANGE_TYPE_CODE_STRING -> AslUtils.streamStringPrimitives(comparison)
                    .map(StringPrimitive::getValue)
                    .map(ChangeTypeUtils::getJooqChangeTypeByCode)
                    .filter(Objects::nonNull)
                    .toList();
            case AD_CHANGE_TYPE_PREFERRED_TERM, AD_CHANGE_TYPE_VALUE -> AslUtils.streamStringPrimitives(comparison)
                    .map(StringPrimitive::getValue)
                    .map(v -> "unknown".equals(v)
                            ? ContributionChangeType.Unknown
                            : ContributionChangeType.lookupLiteral(v))
                    .filter(Objects::nonNull)
                    .toList();
            case null -> AslUtils.conditionValue(comparison.rightComparisonOperands(), operator, aslField.getType());
            default -> AslUtils.conditionValue(comparison.rightComparisonOperands(), operator, aslField.getType());
        };
    }

    private static Optional<AslQueryCondition> buildDvOrderedCondition(
            AslField field, Set<String> dvOrderedTypes, ComparisonConditionOperator operator, List<Primitive> values) {
        if (operator == ComparisonConditionOperator.EXISTS || operator == ComparisonConditionOperator.LIKE) {
            throw new IllegalArgumentException("LIKE/EXISTS on DV_ORDERED is not supported");
        }
        List<Pair<Set<String>, Set<Object>>> typeToValues =
                determinePossibleDvOrderedTypesAndValues(dvOrderedTypes, operator, values);
        if (typeToValues.isEmpty()) {
            return Optional.of(new AslFalseQueryCondition());
        }
        return AslUtils.reduceConditions(
                LogicalConditionOperator.OR,
                typeToValues.stream()
                        .map(e -> new AslDvOrderedValueQueryCondition<>(
                                e.getKey(), field, operator.getAslOperator(), List.copyOf(e.getValue()))));
    }

    /**
     *
     * @param allowedTypes
     * @param values
     * @return &lt;Set&lt;DvOrdered type&gt;, Set&lt;magnitude value&gt;&gt;
     */
    private static List<Pair<Set<String>, Set<Object>>> determinePossibleDvOrderedTypesAndValues(
            Set<String> allowedTypes, ComparisonConditionOperator operator, Collection<Primitive> values) {
        // non-numeric DvOrdered cannot be handled together
        HashMap<String, Set<Object>> nonNumericDvOrderedTypeToValues = new HashMap<>();
        boolean hasNumericDvOrdered = CollectionUtils.containsAny(allowedTypes, NUMERIC_DV_ORDERED_TYPES);
        Set<Object> numericValues = new HashSet<>();
        boolean isEqualsOp =
                operator == ComparisonConditionOperator.EQ || operator == ComparisonConditionOperator.MATCHES;
        for (Primitive value : values) {
            if (value instanceof TemporalPrimitive p) {
                handleTemporalPrimitiveForDvOrdered(
                        allowedTypes, p.getTemporal(), isEqualsOp, nonNumericDvOrderedTypeToValues);
            } else if (value instanceof StringPrimitive p) {
                handleStringPrimitiveForDvOrdered(allowedTypes, p, isEqualsOp, nonNumericDvOrderedTypeToValues);
            } else if (value instanceof DoublePrimitive || value instanceof LongPrimitive) {
                if (hasNumericDvOrdered) numericValues.add(value.getValue());
            }
        }

        List<Pair<Set<String>, Set<Object>>> result = new ArrayList<>();
        if (!numericValues.isEmpty()) {
            Set<String> numericDvOrderedTypes = SetUtils.intersection(allowedTypes, NUMERIC_DV_ORDERED_TYPES);
            result.add(Pair.of(numericDvOrderedTypes, numericValues));
        }
        nonNumericDvOrderedTypeToValues.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> Pair.of(Set.of(e.getKey()), e.getValue()))
                .forEach(result::add);
        return result;
    }

    private static void handleStringPrimitiveForDvOrdered(
            Set<String> allowedTypes, StringPrimitive p, boolean isEqualsOp, HashMap<String, Set<Object>> result) {
        /*
        DATE_TIME/TIME strings with fractional seconds, where the precision is not 10^-3,
        or DURATION strings will not be parsed as TemporalPrimitive by the AQL parser.
        To avoid confusion we also support those by checking for the possibility and manually parsing them.
        */
        String val = p.getValue();
        if (CollectionUtils.containsAny(allowedTypes, DV_DATE, DV_DATE_TIME, DV_TIME)) {
            AslUtils.parseDateTimeOrTimeWithHigherPrecision(val)
                    .ifPresent(t -> handleTemporalPrimitiveForDvOrdered(allowedTypes, t, isEqualsOp, result));
        }
        if (allowedTypes.contains(DV_DURATION)) {
            Optional.of(val)
                    .map(v -> {
                        try {
                            return new DvDuration(val);
                        } catch (IllegalArgumentException e) {
                            // not a duration value -> skip it
                            return null;
                        }
                    })
                    .map(DvDuration::getMagnitude)
                    .ifPresent(m -> addToMultiValuedMap(result, DV_DURATION, m));
        }
    }

    private static void handleTemporalPrimitiveForDvOrdered(
            Set<String> allowedTypes, TemporalAccessor p, boolean isEqualsOp, HashMap<String, Set<Object>> result) {
        boolean hasDate = p.isSupported(ChronoField.YEAR);
        boolean hasTime = p.isSupported(ChronoField.HOUR_OF_DAY);
        if (hasDate) {
            if ((!hasTime || !isEqualsOp) && allowedTypes.contains(DV_DATE)) {
                addToMultiValuedMap(
                        result, DV_DATE, OpenEHRDateTimeSerializationUtils.toMagnitude(new DvDate(LocalDate.from(p))));
            }
            if (allowedTypes.contains(DV_DATE_TIME)) {
                addToMultiValuedMap(
                        result, DV_DATE_TIME, OpenEHRDateTimeSerializationUtils.toMagnitude(new DvDateTime(p)));
            }
        } else if (hasTime && allowedTypes.contains(DV_TIME)) {
            addToMultiValuedMap(result, DV_TIME, OpenEHRDateTimeSerializationUtils.toMagnitude(new DvTime(p)));
        }
    }

    private static <K, V> void addToMultiValuedMap(Map<K, Set<V>> map, K key, V value) {
        map.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(value);
    }
}
