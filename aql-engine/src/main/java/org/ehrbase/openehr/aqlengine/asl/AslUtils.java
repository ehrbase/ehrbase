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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.jooq.pg.Tables;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslAndQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFalseQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldFieldQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotNullQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslOrQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslProvidesJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition.AslConditionOperator;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslTrueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslAggregatingField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslConstantField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField.FieldSource;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslFolderItemIdVirtualField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery.AslSourceRelation;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathCohesionAnalysis.PathCohesionTreeNode;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathInfo;
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
import org.jooq.TableField;

public final class AslUtils {

    private static final String EHR_TABLE_ID_FIELD =
            Tables.EHR_.ID.getUnqualifiedName().first();
    private static final String COMP_DATA_TABLE_ROOT_CONCEPT_FIELD =
            Tables.COMP_VERSION.ROOT_CONCEPT.getUnqualifiedName().first();

    public static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedHashMap(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(
                keyMapper,
                valueMapper,
                (u, v) -> {
                    throw new IllegalStateException(
                            String.format("Duplicate key: attempted merging values %s and %s", u, v));
                },
                LinkedHashMap::new);
    }

    @SafeVarargs
    public static <T> Stream<T> concatStreams(Stream<? extends T>... streams) {
        return Arrays.stream(streams).flatMap(s -> s);
    }

    public static Stream<String> streamFieldNames(final AslField field) {
        return switch (field) {
            case AslColumnField cf -> Stream.of(cf.getColumnName());
            case AslRmPathField pf -> Stream.of(pf.getSrcField().getColumnName());
            case AslConstantField<?> __ -> Stream.empty();
            case AslAggregatingField af -> streamFieldNames(af.getBaseField());
            case AslComplexExtractedColumnField ecf -> ecf.getExtractedColumn().getColumns().stream();
            case AslSubqueryField sqf -> {
                AslQuery baseQuery = ((AslDataQuery) sqf.getBaseQuery()).getBase();
                Stream<String> pkeyFieldNames =
                        getTargetType(baseQuery).getPkeyFields().stream().map(TableField::getName);
                Stream<String> filterConditionFieldNames = sqf.getFilterConditions().stream()
                        .flatMap(AslUtils::streamConditionFields)
                        .flatMap(AslUtils::streamFieldNames);
                if (baseQuery instanceof AslStructureQuery sq && sq.isRoot()) {
                    yield concatStreams(pkeyFieldNames, filterConditionFieldNames);
                }
                yield concatStreams(
                        pkeyFieldNames,
                        filterConditionFieldNames,
                        Stream.of(AslStructureColumn.NUM.getFieldName()),
                        Stream.of(AslStructureColumn.NUM_CAP.getFieldName()));
            }
            case AslFolderItemIdVirtualField f -> Stream.of(f.getFieldName());
            case null -> Stream.empty();
        };
    }

    static final class AliasProvider {
        private final Map<String, Integer> aliasCounters = new HashMap<>();

        public String uniqueAlias(String alias) {
            return alias + "_" + aliasCounters.compute(alias, (k, v) -> v == null ? 0 : v + 1);
        }
    }

    private static final EnumSet<AslSourceRelation> SUPPORTED_DESCENDANT_PARENT_RELATIONS = EnumSet.of(
            AslSourceRelation.COMPOSITION,
            AslSourceRelation.EHR_STATUS,
            AslSourceRelation.FOLDER,
            AslSourceRelation.EHR);
    private static final EnumSet<AslSourceRelation> SUPPORTED_DESCENDANT_CONDITIONS = EnumSet.of(
            AslSourceRelation.COMPOSITION,
            AslSourceRelation.EHR_STATUS,
            AslSourceRelation.FOLDER // FOLDER CONTAINS FOLDER
            );

    private AslUtils() {}

    public static AslSourceRelation getTargetType(AslQuery target) {
        if (target instanceof AslStructureQuery sq) {
            return sq.getType();
        } else {
            throw new IllegalArgumentException("target is no StructureQuery: %s".formatted(target));
        }
    }

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
            case null -> Stream.empty();
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
                            findFieldForOwner(EHR_TABLE_ID_FIELD, query.getSelect(), query),
                            aslOperator,
                            conditionValue(value, operator, String.class));
                    case ARCHETYPE_NODE_ID -> new AslFieldValueQueryCondition<>(
                            AslComplexExtractedColumnField.archetypeNodeIdField(ownerSource),
                            aslOperator,
                            archetypeNodeIdConditionValues(value, operator));
                    case ROOT_CONCEPT -> new AslFieldValueQueryCondition<>(
                            findFieldForOwner(COMP_DATA_TABLE_ROOT_CONCEPT_FIELD, query.getSelect(), query),
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

    public static Stream<AslFieldFieldQueryCondition> descendantJoinConditionProviders(
            AslQuery left, AslStructureQuery leftOwner, AslQuery right, AslStructureQuery rightOwner) {

        AslSourceRelation parentRelation = leftOwner.getType();
        if (!SUPPORTED_DESCENDANT_PARENT_RELATIONS.contains(parentRelation)) {
            throw new IllegalArgumentException("unexpected parent relation type %s".formatted(parentRelation));
        }
        AslSourceRelation descendantRelation = rightOwner.getType();
        if (!SUPPORTED_DESCENDANT_CONDITIONS.contains(descendantRelation)) {
            throw new IllegalArgumentException("unexpected descendant relation type %s".formatted(descendantRelation));
        }

        return switch (parentRelation) {
            case EHR -> Stream.of(new AslFieldFieldQueryCondition(
                    findFieldForOwner(EHR_TABLE_ID_FIELD, left.getSelect(), leftOwner),
                    AslConditionOperator.EQ,
                    findFieldForOwner(AslStructureColumn.EHR_ID, right.getSelect(), rightOwner)));
            case EHR_STATUS -> Stream.concat(
                    Stream.of(joinColumnEqualCondition(AslStructureColumn.EHR_ID, left, leftOwner, right, rightOwner)),
                    joinNumCapBetweenConditions(left, leftOwner, right, rightOwner));
                // l.vo_id == r.vo_id and l.num < r.num <= l.num_cap
            case COMPOSITION -> Stream.concat(
                    Stream.of(joinColumnEqualCondition(AslStructureColumn.VO_ID, left, leftOwner, right, rightOwner)),
                    joinNumCapBetweenConditions(left, leftOwner, right, rightOwner));
                // l.ehr_id == r.ehr_id and l.folder_idx == r.folder_idx and l.num < r.num <= l.num_cap
            case FOLDER -> concatStreams(
                    Stream.of(joinColumnEqualCondition(AslStructureColumn.EHR_ID, left, leftOwner, right, rightOwner)),
                    Stream.of(joinColumnEqualCondition(
                            AslStructureColumn.EHR_FOLDER_IDX, left, leftOwner, right, rightOwner)),
                    joinNumCapBetweenConditions(left, leftOwner, right, rightOwner));
            case AUDIT_DETAILS -> throw new IllegalArgumentException(
                    "Descendant condition not applicable to AUDIT_DETAILS");
        };
    }

    public static Stream<AslFieldFieldQueryCondition> pathChildConditions(
            AslQuery left, AslStructureQuery leftOwner, AslQuery right, AslStructureQuery rightOwner) {
        return concatStreams(
                joinSameRootObjectConditions(left, leftOwner, right, rightOwner),
                Stream.of(joinNumEqualParentNumCondition(left, leftOwner, right, rightOwner)));
    }

    public static Stream<AslFieldFieldQueryCondition> archetypeAnchorConditions(
            PathCohesionTreeNode leftNode,
            AslQuery left,
            AslStructureQuery leftOwner,
            AslQuery right,
            AslStructureQuery rightOwner) {
        return concatStreams(
                joinSameRootObjectConditions(left, leftOwner, right, rightOwner),
                Stream.of(joinCItemNumCondition(leftNode, left, leftOwner, right, rightOwner)));
    }

    public static Stream<AslFieldFieldQueryCondition> nodeIdAnchorConditions(
            PathCohesionTreeNode leftNode,
            AslQuery left,
            AslStructureQuery leftOwner,
            AslQuery right,
            AslStructureQuery rightOwner) {
        return concatStreams(
                joinSameRootObjectConditions(left, leftOwner, right, rightOwner),
                Stream.of(joinCItemNumCondition(leftNode, left, leftOwner, right, rightOwner)),
                joinNumCapBetweenConditions(left, leftOwner, right, rightOwner));
    }

    private static Stream<AslFieldFieldQueryCondition> joinSameRootObjectConditions(
            AslQuery left, AslStructureQuery leftOwner, AslQuery right, AslStructureQuery rightOwner) {
        AslSourceRelation parentRelation = leftOwner.getType();
        if (!EnumSet.of(AslSourceRelation.COMPOSITION, AslSourceRelation.EHR_STATUS, AslSourceRelation.FOLDER)
                .contains(parentRelation)) {
            throw new IllegalArgumentException("unexpected parent relation type %s".formatted(parentRelation));
        }
        AslSourceRelation childRelation = rightOwner.getType();
        if (!EnumSet.of(AslSourceRelation.COMPOSITION, AslSourceRelation.EHR_STATUS, AslSourceRelation.FOLDER)
                .contains(childRelation)) {
            throw new IllegalArgumentException("unexpected descendant relation type %s".formatted(childRelation));
        }
        return switch (parentRelation) {
            case EHR_STATUS -> Stream.of(
                    joinColumnEqualCondition(AslStructureColumn.EHR_ID, left, leftOwner, right, rightOwner));
                // l.vo_id == r.vo_id and l.num == r.parent_num
            case COMPOSITION -> Stream.of(
                    joinColumnEqualCondition(AslStructureColumn.VO_ID, left, leftOwner, right, rightOwner));
                // l.ehr_id == r.ehr_id and l.folder_idx = r.folder_idx and l.num == r.parent_num
            case FOLDER -> Stream.of(
                    joinColumnEqualCondition(AslStructureColumn.EHR_ID, left, leftOwner, right, rightOwner),
                    joinColumnEqualCondition(AslStructureColumn.EHR_FOLDER_IDX, left, leftOwner, right, rightOwner));
            case AUDIT_DETAILS -> throw new IllegalArgumentException(
                    "Path child condition not applicable to AUDIT_DETAILS");
            case EHR -> throw new IllegalArgumentException("Path child condition not applicable to EHR");
        };
    }

    private static AslFieldFieldQueryCondition joinColumnEqualCondition(
            AslStructureColumn column,
            AslQuery left,
            AslStructureQuery leftOwner,
            AslQuery right,
            AslStructureQuery rightOwner) {
        return joinColumnEqualToColumnCondition(column, left, leftOwner, column, right, rightOwner);
    }

    private static AslFieldFieldQueryCondition joinColumnEqualToColumnCondition(
            AslStructureColumn leftColumn,
            AslQuery left,
            AslStructureQuery leftOwner,
            AslStructureColumn rightColumn,
            AslQuery right,
            AslStructureQuery rightOwner) {
        return new AslFieldFieldQueryCondition(
                findFieldForOwner(leftColumn, left.getSelect(), leftOwner),
                AslConditionOperator.EQ,
                findFieldForOwner(rightColumn, right.getSelect(), rightOwner));
    }

    private static Stream<AslFieldFieldQueryCondition> joinNumCapBetweenConditions(
            AslQuery left, AslStructureQuery leftOwner, AslQuery right, AslStructureQuery rightOwner) {

        if (leftOwner.isRoot()) {
            // descendants of a root (EHR_STATUS, COMPOSITION) are always constrained by RM type, so no condition on num
            // is necessary
            return Stream.empty();
        } else {
            return Stream.of(
                    new AslFieldFieldQueryCondition(
                            findFieldForOwner(AslStructureColumn.NUM, left.getSelect(), leftOwner),
                            AslConditionOperator.LT,
                            findFieldForOwner(AslStructureColumn.NUM, right.getSelect(), rightOwner)),
                    new AslFieldFieldQueryCondition(
                            findFieldForOwner(AslStructureColumn.NUM_CAP, left.getSelect(), leftOwner),
                            AslConditionOperator.GT_EQ,
                            findFieldForOwner(AslStructureColumn.NUM, right.getSelect(), rightOwner)));
        }
    }

    private static AslFieldFieldQueryCondition joinNumEqualParentNumCondition(
            AslQuery left, AslStructureQuery leftOwner, AslQuery right, AslStructureQuery rightOwner) {
        if (leftOwner.isRoot()) {
            return new AslFieldFieldQueryCondition(
                    findFieldForOwner(AslStructureColumn.PARENT_NUM, right.getSelect(), rightOwner),
                    AslConditionOperator.EQ,
                    new AslConstantField<>(Integer.class, 0, FieldSource.NONE, null));
        } else {
            return joinColumnEqualToColumnCondition(
                    AslStructureColumn.NUM, left, leftOwner, AslStructureColumn.PARENT_NUM, right, rightOwner);
        }
    }

    private static AslFieldFieldQueryCondition joinCItemNumCondition(
            final PathCohesionTreeNode leftNode,
            AslQuery left,
            AslStructureQuery leftOwner,
            AslQuery right,
            AslStructureQuery rightOwner) {
        if (leftOwner.isRoot()) {
            return new AslFieldFieldQueryCondition(
                    findFieldForOwner(AslStructureColumn.C_ITEM_NUM, right.getSelect(), rightOwner),
                    AslConditionOperator.EQ,
                    new AslConstantField<>(Integer.class, 0, FieldSource.NONE, null));
        } else if (PathInfo.isArchetypeNode(leftNode)) {
            return new AslFieldFieldQueryCondition(
                    findFieldForOwner(AslStructureColumn.NUM, left.getSelect(), leftOwner),
                    AslConditionOperator.EQ,
                    findFieldForOwner(AslStructureColumn.C_ITEM_NUM, right.getSelect(), rightOwner));
        } else {
            return joinColumnEqualCondition(AslStructureColumn.C_ITEM_NUM, left, leftOwner, right, rightOwner);
        }
    }
}
