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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslAndQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFalseQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotNullQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslOrQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslProvidesJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition.AslConditionOperator;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslTrueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField.FieldSource;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery.AslSourceRelation;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ComparisonOperatorConditionWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper.ComparisonConditionOperator;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper.LogicalConditionOperator;
import org.ehrbase.openehr.aqlengine.querywrapper.where.LogicalOperatorConditionWrapper;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.TemporalPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.util.OpenEHRDateTimeParseUtils;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.jooq.JSONB;

public final class AslUtils {

    static final class AliasProvider {
        private final Map<String, Integer> aliasCounters = new HashMap<>();

        public String uniqueAlias(String alias) {
            return alias + "_" + aliasCounters.compute(alias, (k, v) -> v == null ? 0 : v + 1);
        }
    }

    private AslUtils() {}

    public static Stream<AslField> streamConditionFields(AslQueryCondition condition) {
        return switch (condition) {
            case AslAndQueryCondition c -> c.getOperands().stream().flatMap(AslUtils::streamConditionFields);
            case AslOrQueryCondition c -> c.getOperands().stream().flatMap(AslUtils::streamConditionFields);
            case AslNotQueryCondition c -> streamConditionFields(c.getCondition());
            case AslNotNullQueryCondition c -> Stream.of(c.getField());
            case AslFieldValueQueryCondition<?> c -> Stream.of(c.getField());
            case AslFalseQueryCondition __ -> Stream.empty();
            case AslTrueQueryCondition __ -> Stream.empty();
            case AslProvidesJoinCondition __ -> throw new IllegalArgumentException();
        };
    }

    public static Stream<ComparisonOperatorConditionWrapper> streamConditionDescriptors(ConditionWrapper condition) {
        if (condition == null) {
            return Stream.empty();
        } else if (condition instanceof ComparisonOperatorConditionWrapper cd) {
            return Stream.of(cd);
        } else if (condition instanceof LogicalOperatorConditionWrapper ld) {
            return ld.logicalOperands().stream().flatMap(AslUtils::streamConditionDescriptors);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + condition);
        }
    }

    public static String translateAqlLikePatternToSql(String aqlLike) {
        StringBuilder sb = new StringBuilder(aqlLike.length());

        for (int pos = 0, l = aqlLike.length(); pos < l; pos++) {
            char c = aqlLike.charAt(pos);
            switch (c) {
                    // sql reserved
                case '%', '_' -> sb.append('\\').append(c);
                    // escape char
                case '\\' -> {
                    pos++;
                    if (pos >= l) {
                        throw new IllegalArgumentException("Invalid LIKE pattern: %s".formatted(aqlLike));
                    }

                    char next = aqlLike.charAt(pos);
                    switch (next) {
                        case '*', '?' -> sb.append(next);
                        case '\\' -> sb.append("\\\\");
                        default -> throw new IllegalArgumentException("Invalid LIKE pattern: %s".formatted(aqlLike));
                    }
                }
                    // replace by sql
                case '?' -> sb.append('_');
                case '*' -> sb.append('%');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    public static OffsetDateTime toOffsetDateTime(StringPrimitive sp) {
        final TemporalAccessor temporal;
        if (sp instanceof TemporalPrimitive tp) {
            temporal = tp.getTemporal();
        } else {
            temporal = parseDateTimeOrTimeWithHigherPrecision(sp.getValue()).orElse(null);
        }
        if (temporal == null) {
            return null;
        } else if (temporal instanceof OffsetDateTime odt) {
            return odt;
        } else if (!temporal.isSupported(ChronoField.YEAR)) {
            return null;
        }

        boolean hasTime = temporal.isSupported(ChronoField.HOUR_OF_DAY);
        boolean hasOffset = hasTime && temporal.isSupported(ChronoField.OFFSET_SECONDS);

        if (hasOffset) {
            return OffsetDateTime.from(temporal);
        } else if (hasTime) {
            return LocalDateTime.from(temporal).atOffset(ZoneOffset.UTC);
        } else {
            return LocalDate.from(temporal).atStartOfDay().atOffset(ZoneOffset.UTC);
        }
    }

    public static Optional<TemporalAccessor> parseDateTimeOrTimeWithHigherPrecision(String val) {
        int dotIdx = val.indexOf('.');
        int tIdx = val.indexOf('T');
        int length = val.length();
        try {
            if (dotIdx == 19 && tIdx == 10 && length > 20 || dotIdx == 15 && tIdx == 8 && length > 16) {
                // extended or compact DATE_TIME format
                return Optional.of(OpenEHRDateTimeParseUtils.parseDateTime(val));
            } else if (tIdx == -1 && (dotIdx == 8 && length > 9 || dotIdx == 10 && length > 11)) {
                // extended or compact TIME format
                return Optional.of(OpenEHRDateTimeParseUtils.parseTime(val));
            }
        } catch (IllegalArgumentException e) {
            if (!(e.getCause() instanceof DateTimeException)) {
                throw e;
            }
        }

        return Optional.empty();
    }

    public static AslColumnField findFieldForOwner(
            AslStructureColumn structureField, List<AslField> fields, AslQuery owner) {
        return findFieldForOwner(structureField.getFieldName(), fields, owner);
    }

    // TODO convert to AslQuery member
    public static AslColumnField findFieldForOwner(String fieldName, List<AslField> fields, AslQuery owner) {
        return fields.stream()
                .filter(f -> f.getOwner() == owner)
                .filter(AslColumnField.class::isInstance)
                .map(AslColumnField.class::cast)
                .filter(f -> fieldName.equals(f.getColumnName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Field '%s' does not exist for owner '%s'".formatted(fieldName, owner.getAlias())));
    }

    static AslQueryCondition structurePredicateCondition(
            ComparisonOperatorPredicate predicate,
            AslStructureQuery query,
            Function<String, Optional<UUID>> templateUuidLookupFunc) {

        Set<String> candidateTypes = new HashSet<>(query.getRmTypes());
        if (candidateTypes.isEmpty() && query.getType() == AslSourceRelation.EHR) {
            candidateTypes.add(RmConstants.EHR);
        }
        AslExtractedColumn extractedColumn = AslExtractedColumn.find(
                        candidateTypes.iterator().next(), predicate.getPath())
                .filter(ec -> ec.getAllowedRmTypes().containsAll(candidateTypes))
                .orElseThrow();
        ComparisonConditionOperator operator =
                ComparisonConditionOperator.valueOf(predicate.getOperator().name());
        final AslConditionOperator aslOperator = operator.getAslOperator();
        FieldSource ownerSource = FieldSource.withOwner(query);
        List<Primitive> value = List.of(((Primitive) predicate.getValue()));
        AslFieldValueQueryCondition<?> condition =
                switch (extractedColumn) {
                    case NAME_VALUE -> new AslFieldValueQueryCondition<>(
                            findFieldForOwner(AslStructureColumn.ENTITY_NAME, query.getSelect(), query),
                            aslOperator,
                            conditionValue(value, operator, String.class));
                    case VO_ID -> new AslFieldValueQueryCondition<>(
                            AslComplexExtractedColumnField.voIdField(ownerSource),
                            aslOperator,
                            conditionValue(value, operator, String.class));
                    case EHR_ID -> new AslFieldValueQueryCondition<>(
                            findFieldForOwner("id", query.getSelect(), query),
                            aslOperator,
                            conditionValue(value, operator, String.class));
                    case ARCHETYPE_NODE_ID -> new AslFieldValueQueryCondition<>(
                            AslComplexExtractedColumnField.archetypeNodeIdField(ownerSource),
                            aslOperator,
                            archetypeNodeIdConditionValues(value, operator));
                    case ROOT_CONCEPT -> new AslFieldValueQueryCondition<>(
                            findFieldForOwner("root_concept", query.getSelect(), query),
                            aslOperator,
                            archetypeNodeIdConditionValues(value, operator).stream()
                                    // archetype must be for COMPOSITION
                                    .filter(tc -> StructureRmType.COMPOSITION
                                            .getAlias()
                                            .equals(tc.aliasedRmType()))
                                    .map(AslRmTypeAndConcept::concept)
                                    .toList());
                    case TEMPLATE_ID -> {
                        // Template id is handled separately since the extracted column stores the internal uuid
                        List<UUID> templateUuids = templateIdConditionValues(value, operator, templateUuidLookupFunc);
                        yield new AslFieldValueQueryCondition<>(
                                findFieldForOwner(AslStructureColumn.TEMPLATE_ID, query.getSelect(), query),
                                aslOperator,
                                templateUuids);
                    }
                    case OV_CONTRIBUTION_ID,
                            OV_TIME_COMMITTED,
                            OV_TIME_COMMITTED_DV,
                            AD_SYSTEM_ID,
                            AD_CHANGE_TYPE_TERMINOLOGY_ID_VALUE,
                            AD_CHANGE_TYPE_PREFERRED_TERM,
                            AD_CHANGE_TYPE_CODE_STRING,
                            AD_CHANGE_TYPE_VALUE,
                            AD_CHANGE_TYPE_DV,
                            AD_DESCRIPTION_VALUE,
                            AD_DESCRIPTION_DV,
                            EHR_TIME_CREATED,
                            EHR_TIME_CREATED_DV,
                            EHR_SYSTEM_ID,
                            EHR_SYSTEM_ID_DV -> throw new IllegalArgumentException(
                            "Unexpected structure predicate on %s".formatted(extractedColumn));
                };
        if (condition.getValues().isEmpty()) {
            return switch (condition.getOperator()) {
                case IN, EQ, LIKE -> new AslFalseQueryCondition();
                case NEQ -> new AslTrueQueryCondition();
                default -> throw new IllegalArgumentException(
                        "Unexpected operator %s".formatted(condition.getOperator()));
            };
        }

        return condition;
    }

    @Nonnull
    static List<AslRmTypeAndConcept> archetypeNodeIdConditionValues(
            List<Primitive> comparison, ComparisonConditionOperator operator) {
        return conditionValue(comparison, operator, String.class).stream()
                .map(String.class::cast)
                .map(AslRmTypeAndConcept::fromArchetypeNodeId)
                .toList();
    }

    @Nonnull
    static List<UUID> templateIdConditionValues(
            List<Primitive> operands,
            ComparisonConditionOperator operator,
            Function<String, Optional<UUID>> templateUuidLookupFunc) {
        if (EnumSet.of(
                        ComparisonConditionOperator.LIKE,
                        ComparisonConditionOperator.GT_EQ,
                        ComparisonConditionOperator.GT,
                        ComparisonConditionOperator.LT_EQ,
                        ComparisonConditionOperator.LT)
                .contains(operator)) {
            // These operators will require special implementation for template_id
            throw new IllegalArgumentException("unexpected operator for template_id: %s".formatted(operator));
        }
        return conditionValue(operands, operator, String.class).stream()
                .filter(Objects::nonNull)
                .map(String.class::cast)
                .map(templateUuidLookupFunc)
                .flatMap(Optional::stream)
                .toList();
    }

    static Stream<StringPrimitive> streamStringPrimitives(ComparisonOperatorConditionWrapper c) {
        return c.rightComparisonOperands().stream()
                .filter(StringPrimitive.class::isInstance)
                .map(StringPrimitive.class::cast);
    }

    static Optional<AslQueryCondition> reduceConditions(
            LogicalConditionOperator setOp, Stream<AslQueryCondition> conditions) {

        List<AslQueryCondition> unfiltered = conditions.toList();

        if (unfiltered.isEmpty()) {
            return Optional.empty();
        }

        List<AslQueryCondition> filtered =
                unfiltered.stream().filter(setOp::filterNotNoop).toList();

        if (filtered.isEmpty()) {
            // if all conditions are noop conditions, return one of them
            return Optional.of(unfiltered.getFirst());
        }

        if (filtered.size() == 1) {
            return Optional.of(filtered.getFirst());
        }

        return filtered.stream()
                .filter(setOp::filterShortCircuit)
                .findFirst()
                .or(() -> Optional.of(setOp.build(filtered)));
    }

    static Optional<AslQueryCondition> predicates(
            List<AndOperatorPredicate> orPredicates,
            Function<ComparisonOperatorPredicate, AslQueryCondition> comparisonOperatorHandler) {
        return reduceConditions(
                LogicalConditionOperator.OR,
                CollectionUtils.emptyIfNull(orPredicates).stream()
                        .map(p -> reduceConditions(
                                LogicalConditionOperator.AND,
                                p.getOperands().stream().map(comparisonOperatorHandler)))
                        .flatMap(Optional::stream));
    }

    static List<?> conditionValue(List<Primitive> values, ComparisonConditionOperator operator, Class<?> type) {
        boolean isJsonbField = JSONB.class.isAssignableFrom(type);
        return switch (operator) {
            case EXISTS -> Collections.emptyList();
            case MATCHES, EQ, NEQ -> values.stream()
                    .map(Primitive::getValue)
                    .filter(p -> isJsonbField
                            || type.isInstance(p)
                            || UUID.class.isAssignableFrom(type) && p instanceof String)
                    .toList();
            case LT, GT_EQ, GT, LT_EQ -> values.stream()
                    .map(Primitive::getValue)
                    .toList();
            case LIKE -> values.stream()
                    .map(Primitive::getValue)
                    .map(String.class::cast)
                    .map(AslUtils::translateAqlLikePatternToSql)
                    .filter(p -> isJsonbField || type.isInstance(p) || UUID.class.isAssignableFrom(type))
                    .toList();
        };
    }

    static AslQueryCondition and(Stream<AslQueryCondition> conditionStream) {
        List<AslQueryCondition> conditions = conditionStream.toList();
        return switch (conditions.size()) {
            case 0 -> null;
            case 1 -> conditions.getFirst();
            default -> new AslAndQueryCondition(conditions);
        };
    }
}
