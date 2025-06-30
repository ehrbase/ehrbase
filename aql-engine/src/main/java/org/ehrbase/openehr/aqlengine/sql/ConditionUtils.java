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
package org.ehrbase.openehr.aqlengine.sql;

import static org.ehrbase.jooq.pg.Tables.COMP_DATA;
import static org.ehrbase.jooq.pg.Tables.COMP_VERSION;
import static org.ehrbase.openehr.dbformat.DbToRmFormat.TYPE_ATTRIBUTE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.jooq.pg.util.AdditionalSQLFunctions;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslAndQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslDvOrderedValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFalseQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotNullQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslOrQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslPathChildCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslProvidesJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition.AslConditionOperator;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslTrueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslAggregatingField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslConstantField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslFolderItemIdVirtualField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslDelegatingJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslFolderItemJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoin;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery.AslSourceRelation;
import org.ehrbase.openehr.aqlengine.sql.AqlSqlQueryBuilder.AslQueryTables;
import org.ehrbase.openehr.dbformat.RmAttributeAlias;
import org.ehrbase.openehr.dbformat.RmTypeAlias;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Table;
import org.jooq.impl.DSL;

final class ConditionUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ConditionUtils() {}

    public static Condition buildJoinCondition(AslJoin aslJoin, AslQueryTables aslQueryToTable) {
        Table<?> sqlLeft = aslQueryToTable.getDataTable(aslJoin.getLeft());
        Table<?> sqlRight = aslQueryToTable.getDataTable(aslJoin.getRight());

        List<Condition> conditions = new ArrayList<>();
        for (AslJoinCondition jc : aslJoin.getOn()) {
            switch (jc) {
                case AslDelegatingJoinCondition desc -> addDelegatingJoinConditions(
                        desc, conditions, sqlLeft, sqlRight);
                case AslPathFilterJoinCondition filterCondition -> conditions.add(
                        buildCondition(filterCondition.getCondition(), aslQueryToTable, true));
                case AslFolderItemJoinCondition c -> conditions.add(
                        joinFolderItemIdEqualVoIdCondition(c, sqlLeft, sqlRight));
            }
        }

        return conditions.stream().reduce(DSL.noCondition(), DSL::and);
    }

    private static void addDelegatingJoinConditions(
            AslDelegatingJoinCondition joinCondition, List<Condition> conditions, Table<?> sqlLeft, Table<?> sqlRight) {
        (switch (joinCondition.getDelegate()) {
                    case AslPathChildCondition c -> pathChildConditions(c, sqlLeft, sqlRight, true);
                    case AslFieldJoinCondition c -> fieldJoinCondition(c, sqlLeft, sqlRight, true);
                })
                .forEach(conditions::add);
    }

    private static Stream<Condition> pathChildConditions(
            final AslPathChildCondition dc,
            final Table<?> sqlLeft,
            final Table<?> sqlRight,
            final boolean isJoinCondition) {
        AslSourceRelation parentRelation = dc.getParentRelation();
        if (!EnumSet.of(AslSourceRelation.COMPOSITION, AslSourceRelation.EHR_STATUS, AslSourceRelation.FOLDER)
                .contains(parentRelation)) {
            throw new IllegalArgumentException("unexpected parent relation type %s".formatted(parentRelation));
        }
        if (!EnumSet.of(AslSourceRelation.COMPOSITION, AslSourceRelation.EHR_STATUS, AslSourceRelation.FOLDER)
                .contains(dc.getChildRelation())) {
            throw new IllegalArgumentException(
                    "unexpected descendant relation type %s".formatted(dc.getChildRelation()));
        }

        return switch (parentRelation) {
            case EHR_STATUS -> Stream.of(
                    joinColumnEqualCondition(AslStructureColumn.EHR_ID, dc, sqlLeft, sqlRight, isJoinCondition),
                    joinNumEqualParentNumCondition(dc, sqlLeft, sqlRight, isJoinCondition));
                // l.vo_id == r.vo_id and l.num == r.parent_num
            case COMPOSITION -> Stream.of(
                    joinColumnEqualCondition(AslStructureColumn.VO_ID, dc, sqlLeft, sqlRight, isJoinCondition),
                    joinNumEqualParentNumCondition(dc, sqlLeft, sqlRight, isJoinCondition));
                // l.ehr_id == r.ehr_id and l.folder_idx = r.folder_idx and l.num == r.parent_num
            case FOLDER -> Stream.of(
                    joinColumnEqualCondition(AslStructureColumn.EHR_ID, dc, sqlLeft, sqlRight, isJoinCondition),
                    joinColumnEqualCondition(AslStructureColumn.EHR_FOLDER_IDX, dc, sqlLeft, sqlRight, isJoinCondition),
                    joinNumEqualParentNumCondition(dc, sqlLeft, sqlRight, isJoinCondition));
            case AUDIT_DETAILS -> throw new IllegalArgumentException(
                    "Path child condition not applicable to AUDIT_DETAILS");
            case EHR -> throw new IllegalArgumentException("Path child condition not applicable to EHR");
        };
    }

    private static Stream<Condition> fieldJoinCondition(
            AslFieldJoinCondition ic, Table<?> sqlLeft, Table<?> sqlRight, boolean isJoinCondition) {

        Field lf = FieldUtils.field(sqlLeft, ic.getLeftField(), true);
        Field rf = FieldUtils.field(sqlRight, ic.getRightField(), isJoinCondition);
        return Stream.of(
                switch (ic.getOperator()) {
                    case LIKE -> lf.like(rf);
                    case IN -> lf.in(rf);
                    case EQ -> lf.eq(rf);
                    case NEQ -> lf.ne(rf);
                    case GT_EQ -> lf.ge(rf);
                    case GT -> lf.gt(rf);
                    case LT_EQ -> lf.le(rf);
                    case LT -> lf.lt(rf);
                    case IS_NULL -> lf.isNull();
                    case IS_NOT_NULL -> lf.isNotNull();
                });
    }

    public static Condition buildCondition(AslQueryCondition c, AslQueryTables tables, boolean useAliases) {
        return switch (c) {
            case null -> DSL.noCondition();
            case AslAndQueryCondition and -> DSL.and(and.getOperands().stream()
                    .map(o -> buildCondition(o, tables, useAliases))
                    .toList());
            case AslOrQueryCondition or -> DSL.or(or.getOperands().stream()
                    .map(o -> buildCondition(o, tables, useAliases))
                    .toList());
            case AslNotQueryCondition not -> DSL.not(buildCondition(not.getCondition(), tables, useAliases));
            case AslFalseQueryCondition __ -> DSL.falseCondition();
            case AslTrueQueryCondition __ -> DSL.trueCondition();
            case AslNotNullQueryCondition nn -> notNullCondition(tables, useAliases, nn);
            case AslFieldValueQueryCondition fv -> buildFieldValueCondition(tables, useAliases, fv);
            case AslFieldJoinCondition ic -> DSL.and(fieldJoinCondition(
                            ic,
                            tables.getDataTable(ic.getLeftProvider()),
                            tables.getDataTable(ic.getRightProvider()),
                            false)
                    .toList());
            case AslPathChildCondition dc -> DSL.and(pathChildConditions(
                            dc,
                            tables.getDataTable(dc.getLeftProvider()),
                            dc.getParentRelation() == AslSourceRelation.EHR
                                    ? tables.getVersionTable(dc.getRightProvider())
                                    : tables.getDataTable(dc.getRightProvider()),
                            false)
                    .toList());
        };
    }

    @Nonnull
    private static Condition notNullCondition(AslQueryTables tables, boolean useAliases, AslNotNullQueryCondition nn) {
        AslField field = nn.getField();
        if (field.getExtractedColumn() != null) {
            return DSL.trueCondition();

        } else if (field instanceof AslColumnField f) {
            return (f.isVersionTableField()
                            ? tables.getVersionTable(field.getProvider())
                            : tables.getDataTable(field.getProvider()))
                    .field(f.getName(useAliases))
                    .isNotNull();

        } else {
            throw new IllegalArgumentException(
                    "Unsupported field type: %s".formatted(field.getClass().getSimpleName()));
        }
    }

    private static Condition buildFieldValueCondition(
            AslQueryTables tables, boolean useAliases, AslFieldValueQueryCondition fv) {
        AslField field = fv.getField();

        AslQuery internalProvider = field.getInternalProvider();
        Table<?> srcTable = tables.getDataTable(internalProvider);
        if (fv instanceof AslDvOrderedValueQueryCondition<?> dvc) {
            final Field<JSONB> sqlDvOrderedField = FieldUtils.buidDvOrderedField(useAliases, field, srcTable);
            Field<BigDecimal> sqlMagnitudeField = AdditionalSQLFunctions.jsonb_dv_ordered_magnitude(sqlDvOrderedField);
            Field<String> sqlTypeField = DSL.jsonbGetAttributeAsText(
                    sqlDvOrderedField, DSL.inline(RmAttributeAlias.getAlias(TYPE_ATTRIBUTE)));
            List<String> types =
                    dvc.getTypesToCompare().stream().map(RmTypeAlias::getAlias).toList();
            return applyOperator(AslConditionOperator.IN, sqlTypeField, types)
                    .and(applyOperator(dvc.getOperator(), sqlMagnitudeField, dvc.getValues()));
        }

        return switch (field) {
            case AslComplexExtractedColumnField ecf -> complexExtractedColumnCondition(
                    useAliases, fv, ecf, srcTable, tables.getVersionTable(internalProvider));
            case AslColumnField f -> applyOperator(
                    fv.getOperator(),
                    FieldUtils.field(
                            f.isVersionTableField() ? tables.getVersionTable(internalProvider) : srcTable,
                            f,
                            useAliases),
                    fv.getValues());
                // XXX conditions on constant fields could be evaluated here instead of by the DB
            case AslConstantField f -> applyOperator(
                    fv.getOperator(), DSL.inline(f.getValue(), f.getType()), fv.getValues());
            case AslAggregatingField __ -> throw new IllegalArgumentException(
                    "AslAggregatingField cannot be used in WHERE");
            case AslSubqueryField __ -> throw new IllegalArgumentException("AslSubqueryField cannot be used in WHERE");
            case AslFolderItemIdVirtualField __ -> throw new IllegalArgumentException(
                    "AslFolderItemIdValuesColumnField cannot be used in WHERE");
            case AslRmPathField arpf -> {
                Field<JSONB> srcField =
                        FieldUtils.field(Objects.requireNonNull(srcTable), arpf.getSrcField(), JSONB.class, useAliases);
                Field<JSONB> f = FieldUtils.buildJsonbPathField(arpf.getPathInJson(), false, srcField);

                yield applyOperator(
                        fv.getOperator(),
                        arpf.getType() == String.class ? DSL.jsonbGetElementAsText(f, DSL.inline(0)) : f,
                        fv.getValues());
            }
        };
    }

    @Nonnull
    static Condition complexExtractedColumnCondition(
            boolean useAliases,
            AslFieldValueQueryCondition<?> fv,
            AslComplexExtractedColumnField ecf,
            Table<?> dataTable,
            Table<?> versionTable) {
        return switch (ecf.getExtractedColumn()) {
            case VO_ID -> {
                yield switch (fv.getOperator()) {
                    case IS_NULL, IS_NOT_NULL -> voIdCondition(versionTable, useAliases, null, fv.getOperator(), ecf);
                    case IN, EQ -> voIdInCondition(versionTable, useAliases, (List<String>) fv.getValues(), true, ecf);
                    case NEQ -> voIdInCondition(versionTable, useAliases, (List<String>) fv.getValues(), false, ecf);
                    case LIKE, GT_EQ, GT, LT_EQ, LT -> voIdCondition(
                            versionTable, useAliases, (String) fv.getValues().getFirst(), fv.getOperator(), ecf);
                };
            }
            case ARCHETYPE_NODE_ID -> {
                AslConditionOperator op =
                        fv.getOperator() == AslConditionOperator.IN ? AslConditionOperator.EQ : fv.getOperator();
                yield fv.getValues().stream()
                        .map(AslRmTypeAndConcept.class::cast)
                        .map(p -> archetypeNodeIdCondition(dataTable, useAliases, ecf, p, op))
                        .reduce(DSL.noCondition(), DSL::or);
            }
            case TEMPLATE_ID,
                    NAME_VALUE,
                    EHR_ID,
                    ROOT_CONCEPT,
                    OV_CONTRIBUTION_ID,
                    OV_TIME_COMMITTED_DV,
                    OV_TIME_COMMITTED,
                    AD_SYSTEM_ID,
                    AD_DESCRIPTION_DV,
                    AD_DESCRIPTION_VALUE,
                    AD_CHANGE_TYPE_DV,
                    AD_CHANGE_TYPE_VALUE,
                    AD_CHANGE_TYPE_CODE_STRING,
                    AD_CHANGE_TYPE_PREFERRED_TERM,
                    AD_CHANGE_TYPE_TERMINOLOGY_ID_VALUE,
                    EHR_TIME_CREATED_DV,
                    EHR_TIME_CREATED,
                    EHR_SYSTEM_ID,
                    EHR_SYSTEM_ID_DV -> throw new IllegalArgumentException(
                    "Extracted column %s is not complex".formatted(ecf.getExtractedColumn()));
        };
    }

    private static Condition archetypeNodeIdCondition(
            Table<?> src,
            boolean aliasedNames,
            AslComplexExtractedColumnField ecf,
            AslRmTypeAndConcept rmTypeAndConcept,
            AslConditionOperator op) {
        return Stream.of(
                        Pair.of(COMP_DATA.RM_ENTITY, rmTypeAndConcept.aliasedRmType()),
                        Pair.of(COMP_DATA.ENTITY_CONCEPT, rmTypeAndConcept.concept()))
                .filter(p -> p.getValue() != null)
                .map(p1 -> applyOperator(
                        op, FieldUtils.field(src, ecf, p1.getKey().getName(), aliasedNames), List.of(p1.getValue())))
                .reduce(DSL.noCondition(), op == AslConditionOperator.NEQ ? DSL::or : DSL::and);
    }

    @Nonnull
    private static Condition voIdInCondition(
            Table<?> versionTable,
            boolean aliasedNames,
            List<String> ids,
            boolean notNegated,
            AslComplexExtractedColumnField field) {
        if (ids.isEmpty()) {
            return notNegated ? DSL.falseCondition() : DSL.trueCondition();
        }
        if (ids.size() == 1) {
            return voIdCondition(
                    versionTable,
                    aliasedNames,
                    ids.getFirst(),
                    notNegated ? AslConditionOperator.EQ : AslConditionOperator.NEQ,
                    field);
        }

        List<Field<?>> uidList = null;
        List<Field<?>> uidVersionList = null;

        for (String id : ids) {
            // id is expected to be valid, see FeatureCheckUtils::ensureOperandSupported
            Pair<String, Integer> voId = parseVoId(id);
            Field<?> voIdValueField = createVoIdValue(voId);

            if (voId.getRight() == null) {
                if (uidList == null) {
                    uidList = new ArrayList<>(ids.size());
                }
                uidList.add(voIdValueField);
            } else {
                if (uidVersionList == null) {
                    uidVersionList = new ArrayList<>(ids.size());
                }
                uidVersionList.add(voIdValueField);
            }
        }

        Field<?> uuidField = FieldUtils.field(versionTable, field, COMP_VERSION.VO_ID.getName(), aliasedNames);

        Condition uidCond;
        if (uidList == null) {
            uidCond = null;
        } else {
            uidCond = uuidField.in(uidList);
        }
        Condition uidVersionCond;
        if (uidVersionList == null) {
            uidVersionCond = null;
        } else {
            Field<?> versionField =
                    FieldUtils.field(versionTable, field, COMP_VERSION.SYS_VERSION.getName(), aliasedNames);
            uidVersionCond = DSL.field(DSL.row(uuidField, versionField)).in(uidVersionList);
        }

        Condition cond = DSL.or(uidCond, uidVersionCond);
        if (notNegated) {
            return cond;
        } else {
            return cond.not();
        }
    }

    @Nonnull
    private static Condition voIdCondition(
            Table<?> versionTable,
            boolean aliasedNames,
            String id,
            AslConditionOperator op,
            AslComplexExtractedColumnField field) {

        Field<?> uuidField = FieldUtils.field(versionTable, field, COMP_VERSION.VO_ID.getName(), aliasedNames);
        if (op == AslConditionOperator.IS_NULL) {
            return uuidField.isNull();
        } else if (op == AslConditionOperator.IS_NOT_NULL) {
            return uuidField.isNotNull();
        }

        // id is expected to be valid, see FeatureCheckUtils::ensureOperandSupported
        Pair<String, Integer> voId = parseVoId(id);

        Field left;
        if (voId.getRight() == null) {
            left = uuidField;
        } else {
            Field<?> versionField =
                    FieldUtils.field(versionTable, field, COMP_VERSION.SYS_VERSION.getName(), aliasedNames);
            left = DSL.field(DSL.row(uuidField, versionField));
        }
        Field<?> right = createVoIdValue(voId);

        return switch (op) {
            case IN, EQ -> left.eq(right);
            case NEQ -> left.ne(right);
            case LT -> left.lt(right);
            case GT -> left.gt(right);
            case GT_EQ -> left.ge(right);
            case LT_EQ -> left.le(right);
            case LIKE, IS_NULL, IS_NOT_NULL -> throw new IllegalArgumentException();
        };
    }

    private static Pair<String, Integer> parseVoId(String id) {
        int uidEndPos = id.indexOf("::");
        if (uidEndPos < 0) {
            return Pair.of(id, null);
        }
        int versionPos = id.indexOf("::", uidEndPos + 2);
        return Pair.of(id.substring(0, uidEndPos), Integer.parseInt(id.substring(versionPos + 2)));
    }

    private static Field<?> createVoIdValue(Pair<String, Integer> voId) {
        Field<UUID> uuidField = DSL.inline(voId.getLeft()).cast(UUID.class);
        if (voId.getRight() == null) {
            return uuidField;
        } else {
            Field<Integer> versionField = DSL.inline(voId.getRight());
            return DSL.field(DSL.row(uuidField, versionField));
        }
    }

    private static Condition applyOperator(AslConditionOperator operator, Field field, Collection<?> values) {
        Class<?> sqlFieldType = field.getType();
        boolean isJsonbField = JSONB.class.isAssignableFrom(sqlFieldType);
        boolean isUuidField = !isJsonbField && UUID.class.isAssignableFrom(sqlFieldType);
        if (operator == AslConditionOperator.LIKE) {
            String likePattern = (String) values.iterator().next();
            if (isJsonbField) {
                likePattern = escapeAsJsonString(likePattern);
            }
            return field.cast(String.class).like(likePattern);
        } else if (operator == AslConditionOperator.IS_NULL) {
            return field.isNull();
        } else if (operator == AslConditionOperator.IS_NOT_NULL) {
            return field.isNotNull();
        }

        boolean orderOperator = EnumSet.of(
                        AslConditionOperator.GT_EQ,
                        AslConditionOperator.GT,
                        AslConditionOperator.LT_EQ,
                        AslConditionOperator.LT)
                .contains(operator);

        List filteredValues = values.stream()
                .map(v -> {
                    final Object value;
                    if (isUuidField && v instanceof String s) {
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(s);
                        } catch (IllegalArgumentException e) {
                            // value stays null
                            uuid = null;
                        }
                        value = uuid;
                    } else if (isJsonbField
                            || orderOperator
                            || sqlFieldType.isInstance(v)
                            || (Number.class.isAssignableFrom(sqlFieldType) && v instanceof Number)) {
                        value = v;
                    } else {
                        value = null;
                    }
                    return value;
                })
                .filter(Objects::nonNull)
                .toList();
        return switch (filteredValues.size()) {
            case 0 -> switch (operator) {
                case IN, EQ -> DSL.falseCondition();
                case NEQ -> DSL.trueCondition();
                case GT_EQ, GT, LT_EQ, LT -> throw new IllegalArgumentException(
                        "%s-Condition needs one value, not 0".formatted(operator));
                default -> throw new IllegalStateException("Unexpected value: " + operator);
            };
            case 1 -> {
                Object val = filteredValues.getFirst();
                boolean valueAndFieldTypeCompatible = sqlFieldType.isInstance(val)
                        || (Number.class.isAssignableFrom(sqlFieldType) && val instanceof Number);
                Field wrappedValue = isJsonbField || (orderOperator && !valueAndFieldTypeCompatible)
                        ? AdditionalSQLFunctions.to_jsonb(val)
                        : DSL.inline(val);
                Field wrappedField = !isJsonbField && orderOperator && !valueAndFieldTypeCompatible
                        ? AdditionalSQLFunctions.to_jsonb(field)
                        : field;
                yield switch (operator) {
                    case IN, EQ -> field.eq(wrappedValue);
                    case NEQ -> field.ne(wrappedValue);
                    case GT_EQ -> wrappedField.ge(wrappedValue);
                    case GT -> wrappedField.gt(wrappedValue);
                    case LT_EQ -> wrappedField.le(wrappedValue);
                    case LT -> wrappedField.lt(wrappedValue);
                    default -> throw new IllegalStateException("Unexpected value: " + operator);
                };
            }
            default -> switch (operator) {
                case IN -> field.in(filteredValues.stream()
                        .map(v -> isJsonbField ? AdditionalSQLFunctions.to_jsonb(v) : DSL.inline(v))
                        .toList());
                case EQ, NEQ, GT_EQ, GT, LT_EQ, LT -> throw new IllegalArgumentException(
                        "%s-Condition needs one value, not %d".formatted(operator, filteredValues.size()));
                default -> throw new IllegalStateException("Unexpected value: " + operator);
            };
        };
    }

    /**
     * Provides a join conditions using the given column name
     * <code>[sqlLeft].column = [sqlRight].column</code>
     * Example:
     * <code>"p_data__0"."p_data__0_vo_id" = "p_events__0"."p_events__0_vo_id"</code>
     */
    private static Condition joinColumnEqualCondition(
            AslStructureColumn column,
            AslProvidesJoinCondition dc,
            Table<?> sqlLeft,
            Table<?> sqlRight,
            boolean aliased) {
        final String cName = column.getFieldName();
        return FieldUtils.field(sqlLeft, dc.getLeftProvider(), dc.getLeftOwner(), cName, UUID.class, true)
                .eq(FieldUtils.field(sqlRight, dc.getRightProvider(), dc.getRightOwner(), cName, UUID.class, aliased));
    }

    /**
     * Provides a parent child join conditions using the left <code>num</code> to right <code>parent_num</code>
     * <code>[sqlLeft].num = [sqlRight].parent_name</code>
     * Example:
     * <code>"p_data__0"."p_data__0_vo_id" = "p_events__0"."p_events__0_vo_id"</code>
     */
    private static Condition joinNumEqualParentNumCondition(
            AslProvidesJoinCondition dc, Table<?> sqlLeft, Table<?> sqlRight, boolean aliased) {

        final String num = AslStructureColumn.NUM.getFieldName();
        final String parentNum = AslStructureColumn.PARENT_NUM.getFieldName();

        return FieldUtils.field(sqlLeft, dc.getLeftProvider(), dc.getLeftOwner(), num, Integer.class, true)
                .eq(FieldUtils.field(
                        sqlRight, dc.getRightProvider(), dc.getRightOwner(), parentNum, Integer.class, aliased));
    }

    /**
     * Provides the FOLDER contains COMPOSITION join condition using
     * <code>[sqlRight]_vo_id = [sqlLeft]_item_id_value</code>
     * Example:
     * <code>on "sCO_c_0_vo_id" = "sF_0"."sF_0_item_id_value"</code>
     *
     * @param dc {@link AslFolderItemJoinCondition}
     * @param sqlLeft structure query on <code>folder_data</code>
     * @param sqlRight structure query on <code>comp_data</code>
     * @return joinByItemId matching the composition void against the folder item id
     */
    private static Condition joinFolderItemIdEqualVoIdCondition(
            AslFolderItemJoinCondition dc, Table<?> sqlLeft, Table<?> sqlRight) {

        AslQuery leftOwner = dc.getLeftOwner();

        AslQuery rightProvider = dc.rightProvider();
        AslQuery rightOwner = dc.getRightOwner();

        AslFolderItemIdVirtualField column = leftOwner.getSelect().stream()
                .filter(AslFolderItemIdVirtualField.class::isInstance)
                .map(AslFolderItemIdVirtualField.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "AslFolderItemJoinCondition requires an AslFolderItemIdValuesColumnField"));

        // comp.vo_id == folder.data /items/id/value
        // on "sCO_c_0_vo_id" = "sF_0_data_item_id_value"
        return FieldUtils.field(
                        sqlRight, rightProvider, rightOwner, AslStructureColumn.VO_ID.getFieldName(), UUID.class, true)
                .eq(FieldUtils.field(sqlLeft, column, column.getFieldName(), UUID.class, true));
    }

    static String escapeAsJsonString(String string) {
        if (string == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(string);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }
}
