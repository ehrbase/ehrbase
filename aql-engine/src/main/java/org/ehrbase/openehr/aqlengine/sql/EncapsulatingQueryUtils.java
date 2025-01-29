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
import static org.ehrbase.openehr.aqlengine.ChangeTypeUtils.JOOQ_CHANGE_TYPE_TO_CODE;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.util.AdditionalSQLFunctions;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslAggregatingField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslConstantField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslDvOrderedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslFolderItemIdVirtualField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslOrderByField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoin;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRmObjectDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.sql.AqlSqlQueryBuilder.AslQueryTables;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction.AggregateFunctionName;
import org.jooq.CaseWhenStep;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.JoinType;
import org.jooq.Param;
import org.jooq.SelectField;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EncapsulatingQueryUtils {
    private static final Logger LOG = LoggerFactory.getLogger(EncapsulatingQueryUtils.class);

    private EncapsulatingQueryUtils() {}

    private static SelectField<?> sqlAggregatingField(
            AslAggregatingField af, Table<?> src, AslQueryTables aslQueryToTable) {
        if ((src == null || af.getBaseField() == null) && af.getFunction() != AggregateFunctionName.COUNT) {
            throw new IllegalArgumentException("only count does not require a source table");
        }

        boolean isExtractedColumn = Optional.of(af)
                .map(AslAggregatingField::getBaseField)
                .map(AslField::getExtractedColumn)
                // treat VERSION.commit_audit.time_committed and EHR.time_created as primitive and not DV_ORDERED
                .filter(ec -> !EnumSet.of(
                                AslExtractedColumn.OV_TIME_COMMITTED,
                                AslExtractedColumn.OV_TIME_COMMITTED_DV,
                                AslExtractedColumn.EHR_TIME_CREATED_DV,
                                AslExtractedColumn.EHR_TIME_CREATED)
                        .contains(ec))
                .isPresent();
        if (isExtractedColumn && af.getFunction() != AggregateFunctionName.COUNT) {
            throw new IllegalArgumentException(
                    "Aggregate function %s is not allowed for extracted columns".formatted(af.getFunction()));
        }

        Function<Field<?>, SelectField<?>> aggregateFunction = toAggregatedFieldFunction(af);
        Field<?> field = fieldToAggregate(src, af, aslQueryToTable);

        return aggregateFunction.apply(field);
    }

    @Nullable
    private static Field<?> fieldToAggregate(Table<?> src, AslAggregatingField af, AslQueryTables aslQueryToTable) {
        return switch (af.getBaseField()) {
            case null -> null;
            case AslColumnField f -> FieldUtils.field(Objects.requireNonNull(src), f, true);
            case AslComplexExtractedColumnField ecf -> {
                Objects.requireNonNull(src);
                yield switch (ecf.getExtractedColumn()) {
                    case VO_ID -> FieldUtils.field(src, ecf, COMP_DATA.VO_ID.getName(), true);
                    case ARCHETYPE_NODE_ID -> DSL.field(DSL.row(
                            FieldUtils.field(src, ecf, COMP_DATA.RM_ENTITY.getName(), true),
                            FieldUtils.field(src, ecf, COMP_DATA.ENTITY_CONCEPT.getName(), true)));
                    case NAME_VALUE,
                            EHR_ID,
                            TEMPLATE_ID,
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
                            "%s is not a complex extracted column".formatted(ecf.getExtractedColumn()));
                };
            }
            case AslConstantField cf -> DSL.inline(cf.getValue(), cf.getType());
            case AslSubqueryField sqfd -> subqueryField(sqfd, aslQueryToTable);
            case AslAggregatingField __ -> throw new IllegalArgumentException(
                    "Cannot aggregate on AslAggregatingField");
            case AslFolderItemIdVirtualField __ -> throw new IllegalArgumentException(
                    "Cannot aggregate on AslFolderItemIdValuesColumnField");
            case AslRmPathField arpf -> FieldUtils.buildRmPathField(arpf, src);
        };
    }

    @Nonnull
    private static Function<Field<?>, SelectField<?>> toAggregatedFieldFunction(AslAggregatingField af) {
        return switch (af.getFunction()) {
            case COUNT -> f -> AdditionalSQLFunctions.count(af.isDistinct(), f);
            case MIN -> f -> af.getBaseField() instanceof AslDvOrderedColumnField
                            || (af.getBaseField() instanceof AslRmPathField pf
                                    && !pf.getDvOrderedTypes().isEmpty())
                    ? AdditionalSQLFunctions.min_dv_ordered(f)
                    : DSL.min(f);
            case MAX -> f -> af.getBaseField() instanceof AslDvOrderedColumnField
                            || (af.getBaseField() instanceof AslRmPathField pf
                                    && !pf.getDvOrderedTypes().isEmpty())
                    ? AdditionalSQLFunctions.max_dv_ordered(f)
                    : DSL.max(f);
            case SUM -> f -> DSL.aggregate("sum", SQLDataType.NUMERIC, f);
            case AVG -> f -> DSL.aggregate("avg", SQLDataType.NUMERIC, f);
        };
    }

    static SelectField<?> sqlSelectFieldForExtractedColumn(AslComplexExtractedColumnField ecf, Table<?> src) {
        return switch (ecf.getExtractedColumn()) {
            case VO_ID -> DSL.row(
                    FieldUtils.field(src, ecf, COMP_DATA.VO_ID.getName(), true),
                    FieldUtils.field(src, ecf, COMP_VERSION.SYS_VERSION.getName(), true));
            case ARCHETYPE_NODE_ID -> DSL.row(
                    FieldUtils.field(src, ecf, COMP_DATA.ENTITY_CONCEPT.getName(), true),
                    FieldUtils.field(src, ecf, COMP_DATA.RM_ENTITY.getName(), true));
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

    private static Field<?> subqueryField(AslSubqueryField sqf, AslQueryTables aslQueryToTable) {
        AslQuery baseQuery = sqf.getBaseQuery();
        if (!(baseQuery instanceof AslRmObjectDataQuery aq)) {
            throw new IllegalArgumentException("Subquery field not supported for type: " + baseQuery.getClass());
        }
        return AqlSqlQueryBuilder.buildDataSubquery(
                        aq,
                        aslQueryToTable,
                        sqf.getFilterConditions().stream()
                                .map(c -> ConditionUtils.buildCondition(c, aslQueryToTable, true))
                                .toArray(Condition[]::new))
                .asField();
    }

    /**
     * substring(entity_concept, 1, 1) = '.',
     * case when substring(entity_concept, 1, 1) = '.' then rm_entity else null end,
     *        entity_concept
     * @param conceptField
     * @param typeField
     * @return
     */
    private static Stream<Field<?>> archetypeNodeIdOrderFields(Field conceptField, Field typeField) {
        Condition isArchetype = conceptField.like(DSL.inline(".%"));

        // order by type name, not alias
        Map<Param<String>, Param<Integer>> rmTypeOrderMap = new LinkedHashMap<>();
        {
            Iterator<StructureRmType> it = Arrays.stream(StructureRmType.values())
                    .sorted(Comparator.comparing(Enum::name))
                    .iterator();
            int pos = 0;
            while (it.hasNext()) {
                rmTypeOrderMap.put(DSL.inline(it.next().getAlias()), DSL.inline(pos++));
            }
        }

        CaseWhenStep typeOrderField = DSL.case_(typeField).mapValues(rmTypeOrderMap);

        // at… / id… before openEHR…
        return Stream.of(
                // at… / id… before openEHR…
                isArchetype,
                // for archetypes order by RM type
                DSL.case_().when(isArchetype, typeOrderField),
                conceptField);
    }

    private static Field templateIdOrderField(Field templateUidField, TemplateService templateService) {
        // order lexicographically by template id
        Map<UUID, String> templates = templateService.findAllTemplateIds();

        if (templates.isEmpty()) {
            LOG.warn("No template ids found: Fallback to ordering by internal UUID");
            return templateUidField;
        }

        Map<Param<UUID>, Param<Integer>> templateIdOrderMap = new LinkedHashMap<>();
        Iterator<UUID> it = templates.entrySet().stream()
                .sorted(Comparator.comparing(Entry::getValue, Collator.getInstance(Locale.ENGLISH)))
                .map(Entry::getKey)
                .iterator();
        int pos = 0;
        while (it.hasNext()) {
            templateIdOrderMap.put(DSL.inline(it.next()), DSL.inline(pos++));
        }

        return DSL.case_(templateUidField).mapValues(templateIdOrderMap).else_(DSL.inline((Object) null));
    }

    /**
     * Postgresql contains a bug where filters in lateral left joins inside a left join are not respected.
     * This situation can be avoided by applying an identity function to each select expression.
     * <p>
     * See <a href="https://www.postgresql.org/message-id/18284-47505a20c23647f8@postgresql.org">Postgresql BUG #18284</a>.
     *
     * @param childQuery
     * @param join
     * @param relation
     */
    public static void applyPgLljWorkaround(AslQuery childQuery, AslJoin join, Table<?> relation) {
        boolean workaroundNeeded = join.getJoinType() != null
                && join.getJoinType() != JoinType.JOIN
                && !(childQuery instanceof AslStructureQuery);
        if (workaroundNeeded) {
            // wrap each field with COALESCE() as identity function
            Field<?>[] fields = relation.fieldsRow().fields();
            for (int i = 0; i < fields.length; i++) {
                Field<?> field = fields[i];
                // DSL::function because DSL::coalesce would be liquidated
                fields[i] = DSL.function("COALESCE", field.getDataType(), field).as(field.getName());
            }
        }
    }

    public static SelectField<?> selectField(AslField field, AslQueryTables aslQueryToTable) {
        Table<?> src = Optional.of(field)
                .map(AslField::getInternalProvider)
                .map(aslQueryToTable::getDataTable)
                .orElse(null);
        return switch (field) {
            case AslColumnField f -> FieldUtils.field(Objects.requireNonNull(src), f, true)
                    .as(f.getName(true));
            case AslComplexExtractedColumnField ecf -> sqlSelectFieldForExtractedColumn(
                    ecf, Objects.requireNonNull(src));
            case AslAggregatingField af -> sqlAggregatingField(af, src, aslQueryToTable);
            case AslConstantField<?> cf -> DSL.inline(cf.getValue(), cf.getType());
            case AslSubqueryField sqf -> subqueryField(sqf, aslQueryToTable);
            case AslFolderItemIdVirtualField fidv -> throw new IllegalArgumentException(
                    "%s is not support as select field".formatted(fidv.getExtractedColumn()));
            case AslRmPathField arpf -> FieldUtils.buildRmPathField(arpf, src);
        };
    }

    @Nonnull
    public static Stream<Field<?>> groupByFields(AslField gb, AslQueryTables aslQueryToTable) {
        Table<?> src = aslQueryToTable.getDataTable(gb.getInternalProvider());
        return switch (gb) {
            case AslColumnField f -> Stream.of(FieldUtils.field(src, f, true));
            case AslComplexExtractedColumnField ecf -> {
                switch (ecf.getExtractedColumn()) {
                    case VO_ID -> {
                        Field<?> voIdField = FieldUtils.field(src, ecf, COMP_DATA.VO_ID.getName(), true);
                        Field<?> versionField = FieldUtils.field(src, ecf, COMP_VERSION.SYS_VERSION.getName(), true);
                        yield Stream.of(voIdField, versionField);
                    }
                    case ARCHETYPE_NODE_ID -> {
                        Field<?> conceptField = FieldUtils.field(src, ecf, COMP_DATA.ENTITY_CONCEPT.getName(), true);
                        Field<?> typeField = FieldUtils.field(src, ecf, COMP_DATA.RM_ENTITY.getName(), true);
                        yield Stream.of(typeField, conceptField);
                    }
                    default -> throw new IllegalArgumentException(
                            "%s is not a complex extracted column".formatted(ecf.getExtractedColumn()));
                }
            }
            case AslSubqueryField sqf -> Stream.of(subqueryField(sqf, aslQueryToTable));
            case AslConstantField<?> __ -> Stream.empty();
            case AslAggregatingField __ -> throw new IllegalArgumentException(
                    "Cannot aggregate by AslAggregatingField");
            case AslFolderItemIdVirtualField __ -> throw new IllegalArgumentException(
                    "Cannot aggregate by AslFolderItemIdValuesColumnField");
            case AslRmPathField arpf -> Stream.of(FieldUtils.buildRmPathField(arpf, src));
        };
    }

    private static Stream<Field<?>> complexExtractedColumnOrderByFields(
            AslComplexExtractedColumnField ecf, Table<?> src) {
        return switch (ecf.getExtractedColumn()) {
            case VO_ID -> Stream.of(FieldUtils.field(src, ecf, COMP_DATA.VO_ID.getName(), true));
            case ARCHETYPE_NODE_ID -> {
                Field<?> conceptField = FieldUtils.field(src, ecf, COMP_DATA.ENTITY_CONCEPT.getName(), true);
                Field<?> typeField = FieldUtils.field(src, ecf, COMP_DATA.RM_ENTITY.getName(), true);
                yield archetypeNodeIdOrderFields(conceptField, typeField);
            }
            default -> throw new IllegalArgumentException(
                    "Order by %s is not supported".formatted(ecf.getExtractedColumn()));
        };
    }

    @Nonnull
    public static Stream<SortField<?>> orderFields(
            AslOrderByField ob, AslQueryTables aslQueryToTable, TemplateService templateService) {
        AslField aslField = ob.field();
        Table<?> src = aslQueryToTable.getDataTable(aslField.getInternalProvider());
        return (switch (aslField) {
                    case AslDvOrderedColumnField f -> Stream.of(AdditionalSQLFunctions.jsonb_dv_ordered_magnitude(
                            (Field<JSONB>) FieldUtils.field(src, f, true)));
                    case AslColumnField f -> columnOrderField(f, src, templateService);
                    case AslComplexExtractedColumnField ecf -> complexExtractedColumnOrderByFields(ecf, src);
                    case AslConstantField __ -> Stream.<Field<?>>empty();
                    case AslSubqueryField sqf -> Stream.of(subqueryField(sqf, aslQueryToTable));
                    case AslAggregatingField __ -> throw new IllegalArgumentException(
                            "ORDER BY AslAggregatingField is not allowed");
                    case AslFolderItemIdVirtualField __ -> throw new IllegalArgumentException(
                            "ORDER BY AslFolderItemIdValuesColumnField is not allowed");
                    case AslRmPathField arpf -> {
                        var f = FieldUtils.buildRmPathField(arpf, src);
                        if (arpf.getType() == String.class
                                || arpf.getDvOrderedTypes().isEmpty()) {
                            yield Stream.of(f);
                        } else {
                            yield Stream.of(AdditionalSQLFunctions.jsonb_dv_ordered_magnitude((Field<JSONB>) f));
                        }
                    }
                })
                .map(f -> f.sort(ob.direction()));
    }

    @Nonnull
    private static Stream<Field<?>> columnOrderField(AslColumnField f, Table<?> src, TemplateService templateService) {
        Field<?> field = FieldUtils.field(src, f, true);

        field = switch (f.getExtractedColumn()) {
                // ensure order by name, not internal ID
            case TEMPLATE_ID -> templateIdOrderField(field, templateService);
            case AD_CHANGE_TYPE_VALUE, AD_CHANGE_TYPE_PREFERRED_TERM -> DSL.lower(field.cast(String.class));
            case AD_CHANGE_TYPE_CODE_STRING -> DSL.case_((Field<ContributionChangeType>) field)
                    .mapValues(JOOQ_CHANGE_TYPE_TO_CODE);
            case null -> field;
            default -> field;};
        return Stream.of(field);
    }
}
