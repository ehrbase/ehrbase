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
import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_DATA;
import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_VERSION;
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_DATA;
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_VERSION;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.jooq.pg.tables.EhrFolderData;
import org.ehrbase.jooq.pg.util.AdditionalSQLFunctions;
import org.ehrbase.openehr.aqlengine.AqlConfigurationProperties;
import org.ehrbase.openehr.aqlengine.asl.AslUtils;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslAggregatingField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslConstantField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslFolderItemIdVirtualField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoin;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslFilteringQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslPathDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRmObjectDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery.AslSourceRelation;
import org.ehrbase.openehr.aqlengine.sql.postprocessor.AqlSqlQueryPostProcessor;
import org.ehrbase.openehr.dbformat.DbToRmFormat;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.JSONObjectAggNullStep;
import org.jooq.Operator;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectField;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.SelectHavingStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.SelectQuery;
import org.jooq.SelectSelectStep;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableLike;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Builds an SQL query from an ASL query
 */
@Component
public class AqlSqlQueryBuilder {

    private final AqlConfigurationProperties aqlConfigurationProperties;
    private final DSLContext context;
    private final TemplateService templateService;
    private final Optional<AqlSqlQueryPostProcessor> queryPostProcessor;

    public AqlSqlQueryBuilder(
            AqlConfigurationProperties aqlConfigurationProperties,
            DSLContext context,
            TemplateService templateService,
            Optional<AqlSqlQueryPostProcessor> queryPostProcessor) {
        this.aqlConfigurationProperties = aqlConfigurationProperties;
        this.context = context;
        this.templateService = templateService;
        this.queryPostProcessor = queryPostProcessor;
    }

    public static String subqueryAlias(AslQuery aslQuery) {
        return aslQuery.getAlias() + "sq";
    }

    public static String versionSubqueryAlias(AslQuery aslQuery) {
        return aslQuery.getAlias() + "_version_sq";
    }

    /**
     * Resolves the data and version jooq tables (sql tables or sub-queries) the <code>AslQuery</code> is based on
     */
    static class AslQueryTables {

        private final Map<AslQuery, Table<?>> dataTables = new HashMap<>();
        private final Map<AslQuery, Table<?>> versionTables = new HashMap<>();

        private AslQueryTables() {}

        Table<?> getDataTable(AslQuery q) {
            return dataTables.get(q);
        }

        Table<?> getVersionTable(AslQuery q) {
            return versionTables.get(q);
        }

        public void put(AslQuery q, Table<?> dataTable, Table<?> versionTable) {
            dataTables.put(q, dataTable);
            versionTables.put(q, versionTable);
        }

        public void remove(AslStructureQuery aq) {
            dataTables.remove(aq);
            versionTables.remove(aq);
        }
    }

    public SelectQuery<Record> buildSqlQuery(AslRootQuery aslRootQuery) {

        AslQueryTables aslQueryToTable = new AslQueryTables();
        SelectJoinStep<Record> encapsulatingQuery =
                buildEncapsulatingQuery(aslRootQuery, context::select, aslQueryToTable);

        SelectQuery<Record> query = encapsulatingQuery.getQuery();

        // LIMIT
        if (aslRootQuery.getLimit() != null) {
            query.addLimit(aslRootQuery.getOffset() == null ? 0L : aslRootQuery.getOffset(), aslRootQuery.getLimit());
        }

        queryPostProcessor.ifPresent(p -> p.afterBuildSqlQuery(aslRootQuery, query));

        return query;
    }

    public Result<Record> explain(boolean analyze, SelectQuery<Record> selectQuery) {
        if (analyze) {
            return context.fetch("EXPLAIN (SUMMARY, COSTS, VERBOSE, FORMAT JSON, ANALYZE, TIMING) {0}", selectQuery);
        } else {
            return context.fetch("EXPLAIN (SUMMARY, COSTS, VERBOSE, FORMAT JSON) {0}", selectQuery);
        }
    }

    @Nonnull
    private SelectJoinStep<Record> buildEncapsulatingQuery(
            AslEncapsulatingQuery aq, Supplier<SelectSelectStep<Record>> creator, AslQueryTables aslQueryToTable) {
        Iterator<Pair<AslQuery, AslJoin>> childIt = aq.getChildren().iterator();

        // from

        AslQuery aslRoot = childIt.next().getLeft();
        Table<?> root = buildQuery(aslRoot, null, aslQueryToTable);
        aslQueryToTable.put(aslRoot, root, root);
        SelectJoinStep<Record> from = creator.get().from(root);

        while (childIt.hasNext()) {
            Pair<AslQuery, AslJoin> nextChild = childIt.next();
            AslQuery childQuery = nextChild.getLeft();
            AslJoin join = nextChild.getRight();
            AslQuery target = join.getLeft();
            Table<?> toJoin = buildQuery(childQuery, target, aslQueryToTable);

            if (aqlConfigurationProperties.pgLljWorkaround()) {
                EncapsulatingQueryUtils.applyPgLljWorkaround(childQuery, join, toJoin);
            }

            aslQueryToTable.put(childQuery, toJoin, toJoin);
            from.join(toJoin, join.getJoinType()).on(ConditionUtils.buildJoinCondition(join, aslQueryToTable));
        }

        SelectQuery<Record> query = from.getQuery();
        // select
        for (AslField field : aq.getSelect()) {
            SelectField<?> sqlField = EncapsulatingQueryUtils.selectField(field, aslQueryToTable);
            query.addSelect(sqlField);
        }
        // where
        query.addConditions(
                Operator.AND,
                Stream.concat(
                                Optional.of(aq).map(AslEncapsulatingQuery::getCondition).stream(),
                                aq.getStructureConditions().stream())
                        .map(c -> ConditionUtils.buildCondition(c, aslQueryToTable, true))
                        .toList());

        if (aq instanceof AslRootQuery rq) {
            rq.getGroupByFields().stream()
                    .flatMap(gb -> EncapsulatingQueryUtils.groupByFields(gb, aslQueryToTable))
                    .forEach(query::addGroupBy);

            // if the magnitude is needed for ORDER BY, it is added to the GROUP BY
            rq.getGroupByDvOrderedMagnitudeFields().stream()
                    .map(f -> {
                        Table<?> dataTable = aslQueryToTable.getDataTable(f.getInternalProvider());
                        Field<JSONB> dvOrderedField = FieldUtils.buidDvOrderedField(true, f, dataTable);
                        return AdditionalSQLFunctions.jsonb_dv_ordered_magnitude(dvOrderedField);
                    })
                    .forEach(query::addGroupBy);

            rq.getOrderByFields().stream()
                    .flatMap(ob -> EncapsulatingQueryUtils.orderFields(ob, aslQueryToTable, templateService))
                    .forEach(query::addOrderBy);
        }
        return from;
    }

    private Table<?> buildQuery(AslQuery aslQuery, AslQuery target, AslQueryTables aslQueryToTable) {
        return switch (aslQuery) {
            case AslStructureQuery aq -> buildStructureQuery(aq, aslQueryToTable)
                    .asTable(aq.getAlias());
            case AslEncapsulatingQuery aq -> buildEncapsulatingQuery(aq, DSL::select, aslQueryToTable)
                    .asTable(aq.getAlias());
            case AslRmObjectDataQuery aq -> DSL.lateral(
                    buildDataSubquery(aq, aslQueryToTable).asTable(aq.getAlias()));
            case AslFilteringQuery aq -> DSL.lateral(buildFilteringQuery(aq, aslQueryToTable.getDataTable(target))
                    .asTable(aq.getAlias()));
            case AslPathDataQuery aq -> DSL.lateral(
                    buildPathDataQuery(aq, target, aslQueryToTable).asTable(aq.getAlias()));
        };
    }

    /**
     * Has to be wrapped in DSL::lateral.
     * Applies "jsonb_array_elements" function, if last node is multiple valued
     * <p>
     * Structure based:
     * <p>
     * select "sSE_s_0"."data"->'N' as "pd_0_data"
     * <p>
     * Path data based:
     * <p>
     * select "cData"."data"->'N' as "pd_0_data"
     *
     * @param aslData
     * @param target
     * @return
     */
    private static TableLike<Record> buildPathDataQuery(
            AslPathDataQuery aslData, AslQuery target, AslQueryTables aslQueryToTable) {
        Table<?> targetTable = aslQueryToTable.getDataTable(target);
        Function<String, Field<JSONB>> dataFieldProvider =
                colName -> FieldUtils.aliasedField(targetTable, aslData, colName, JSONB.class);

        return DSL.select(aslData.getSelect().stream()
                .map(AslColumnField.class::cast)
                .map(f -> pathDataField(aslData, f, dataFieldProvider))
                .toList());
    }

    @Nonnull
    private static Field pathDataField(
            AslPathDataQuery aslData, AslColumnField f, Function<String, Field<JSONB>> dataFieldProvider) {
        Field<JSONB> dataField = dataFieldProvider.apply(f.getColumnName());
        Field<JSONB> jsonbField =
                FieldUtils.buildJsonbPathField(aslData.getPathNodes(f), aslData.isMultipleValued(), dataField);
        Field<?> field;
        if (f.getType() == String.class) {
            field = DSL.jsonbGetElementAsText(jsonbField, 0);
        } else {
            field = jsonbField;
        }
        return field.as(f.getName(true));
    }

    private static SelectSelectStep<?> buildFilteringQuery(AslFilteringQuery aq, Table<?> target) {
        final String fieldName = ((AslColumnField) aq.getSelect().getFirst()).getAliasedName();
        Stream<Field> fields =
                switch (aq.getSourceField()) {
                    case AslColumnField src -> Stream.of(
                            FieldUtils.field(target, src, true).as(fieldName));
                    case AslComplexExtractedColumnField src -> src.getExtractedColumn().getColumns().stream()
                            .map(fn -> FieldUtils.field(target, src, fn, true).as(src.aliasedName(fn)));
                    case AslConstantField<?> cf -> Stream.of(DSL.inline(cf.getValue(), cf.getType()));
                    case AslAggregatingField __ -> throw new IllegalArgumentException(
                            "Filtering queries cannot be based on AslAggregatingField");
                    case AslSubqueryField __ -> throw new IllegalArgumentException(
                            "Filtering queries cannot be based on AslSubqueryField");
                    case AslFolderItemIdVirtualField __ -> throw new IllegalArgumentException(
                            "Filtering queries cannot be based on AslFolderItemIdValuesColumnField");
                    case AslRmPathField arpf -> {
                        Field<JSONB> srcField = FieldUtils.field(target, arpf.getSrcField(), JSONB.class, true);
                        Field<JSONB> ret = FieldUtils.buildJsonbPathField(arpf.getPathInJson(), false, srcField);
                        if (arpf.getType() == String.class) {
                            yield Stream.of(DSL.jsonbGetElementAsText(ret, DSL.inline(0))
                                    .as(fieldName));
                        } else {
                            yield Stream.of(ret.as(fieldName));
                        }
                    }
                };
        return DSL.select(fields.toArray(Field[]::new));
    }

    @Nonnull
    private static SelectConditionStep<Record> buildStructureQuery(
            AslStructureQuery aq, AslQueryTables aslQueryToTable) {

        // the data table is needed if any of its fields are selected or constrained
        boolean requiresDataTable = !aq.isRoot()
                || !aq.isRequiresVersionTableJoin()
                || AslUtils.concatStreams(
                                aq.getSelect().stream(),
                                Optional.of(aq).map(AslStructureQuery::getCondition).stream()
                                        .flatMap(AslUtils::streamConditionFields),
                                aq.getStructureConditions().stream().flatMap(AslUtils::streamConditionFields))
                        .flatMap(AslUtils::streamFieldNames)
                        // check if the field is not from the version table (there may be a more efficient way)
                        .anyMatch(f ->
                                aq.getType().getVersionTable().fieldStream().noneMatch(tf -> f.equals(tf.getName())));

        Table<?> dataTable = requiresDataTable ? aq.getType().getDataTable().as(subqueryAlias(aq)) : null;
        Table<?> primaryTable = aq.isRequiresVersionTableJoin()
                ? aq.getType().getVersionTable().as(versionSubqueryAlias(aq))
                : dataTable;

        SelectJoinStep<Record> step = structureQueryBase(aq, primaryTable, dataTable, aq.isRequiresVersionTableJoin());

        aslQueryToTable.put(aq, dataTable, primaryTable);

        // add regular and structure conditions
        SelectConditionStep<Record> where = step.where(Stream.concat(
                        Optional.of(aq).map(AslStructureQuery::getCondition).stream(),
                        aq.getStructureConditions().stream())
                .map(c -> ConditionUtils.buildCondition(c, aslQueryToTable, false))
                .toArray(Condition[]::new));

        if (aq.isRoot() && requiresDataTable) {
            where = where.and(dataTable
                    .field(AslStructureColumn.NUM.getFieldName(), Integer.class)
                    .eq(DSL.inline(0)));
        }

        // data and primary are local to this sub-query and can be removed
        aslQueryToTable.remove(aq);
        return where;
    }

    @Nonnull
    private static SelectJoinStep<Record> structureQueryBase(
            AslStructureQuery aq, Table<?> primaryTable, Table<?> dataTable, boolean hasVersionTable) {

        Map<Class<? extends AslField>, List<AslField>> aslFields =
                aq.getSelect().stream().collect(Collectors.groupingBy(AslField::getClass));

        Stream<Field<?>> columnFields = consumeFieldsOfType(
                aslFields,
                AslColumnField.class,
                cf -> ((aq.isRequiresVersionTableJoin() && cf.isVersionTableField()) ? primaryTable : dataTable)
                        .field(cf.getColumnName())
                        .as(cf.getAliasedName()));

        Stream<AslFolderItemIdVirtualField> folderFields =
                consumeFieldsOfType(aslFields, AslFolderItemIdVirtualField.class, Function.identity());

        if (!aslFields.isEmpty()) {
            throw new IllegalStateException("StructureQueryBase could not handle AslFields of type %s"
                    .formatted(aslFields.values().stream()
                            .flatMap(Collection::stream)
                            .map(Object::getClass)
                            .map(Class::getSimpleName)
                            .toList()));
        }

        final SelectJoinStep<Record> step;
        if (hasVersionTable) {
            step = structureQueryBaseVersionToDataTable(aq, primaryTable, dataTable, columnFields, folderFields);
        } else {
            step = structureQueryBaseUsingDataTable(aq, primaryTable, columnFields, folderFields);
        }
        return step;
    }

    /**
     * <code>
     * select
     *   "base".*, "fi_uuids"
     * from "ehr"."ehr_folder_data" as "base"
     * join "ehr"."ehr_folder_data" as "descendant"
     * on (
     *      "descendant"."ehr_id" = "base"."ehr_id"
     *      and "descendant"."ehr_folders_idx" = "base"."ehr_folders_idx"
     *      and "descendant"."num" between "base"."num" and "base"."num_cap"
     * )
     * join UNNEST("descendant"."item_uuids") as "fi_uuids"
     * on (1=1)
     * </code>
     */
    private static Pair<Table<?>, List<SelectFieldOrAsterisk>> buildFolderItemIdNestedSelect(
            Table<?> dataTable, AslFolderItemIdVirtualField column, boolean subAlias) {

        String fieldName = column.getFieldName();

        EhrFolderData baseFolderTable = EHR_FOLDER_DATA.as("base");
        EhrFolderData descendantFolderTable = EHR_FOLDER_DATA.as("descendant");

        // --------------------------------------------------------------
        Table<?> itemsUUIDArrayTable = DSL.unnest(descendantFolderTable.field(EHR_FOLDER_DATA.ITEM_UUIDS))
                .as("fi_uuids");

        Field<UUID> itemUUIDs =
                DSL.field(DSL.name("fi_uuids").quotedName().toString(), UUID.class, itemsUUIDArrayTable);
        // @format:off
        // we need all fields at this point + the item id array
        SelectOnConditionStep<Record> selectOnConditionStep = DSL.select(baseFolderTable.asterisk(), itemUUIDs)
                .from(baseFolderTable)
                // -- 1st join on folder data where the folders are subfolder of the root one
                .join(descendantFolderTable)
                    .on(descendantFolderTable.EHR_ID.eq(baseFolderTable.EHR_ID))
                    .and(descendantFolderTable.EHR_FOLDERS_IDX.eq(baseFolderTable.EHR_FOLDERS_IDX))
                    .and(descendantFolderTable.NUM.between(baseFolderTable.NUM, baseFolderTable.NUM_CAP))
                // -- take the item_uuids column, unnest them and then join with the each column
                .join(itemsUUIDArrayTable)
                .on();

        // in case we join using the folder_data table we need to pick a dedicated alias for the items to prevent clashes
        Table<?> joinTable = subAlias
                ? selectOnConditionStep.asTable(dataTable.getName() + "_items")
                :  selectOnConditionStep.asTable(dataTable);

        // @format:on
        List<SelectFieldOrAsterisk> selectFields =
                List.of(FieldUtils.virtualAliasedField(joinTable, itemUUIDs, column, fieldName));
        return Pair.of(joinTable, selectFields);
    }

    @Nonnull
    private static SelectJoinStep<Record> structureQueryBaseVersionToDataTable(
            AslStructureQuery aq,
            Table<?> primaryTable,
            Table<?> dataTable,
            Stream<Field<?>> columnFields,
            Stream<AslFolderItemIdVirtualField> folderFields) {
        return switch (aq.getType()) {
            case EHR_STATUS -> structureQueryBaseUsingVersionAndDataTable(
                    columnFields, primaryTable, dataTable, EHR_STATUS_VERSION.EHR_ID, EHR_STATUS_DATA.EHR_ID);
            case COMPOSITION -> structureQueryBaseUsingVersionAndDataTable(
                    columnFields, primaryTable, dataTable, COMP_VERSION.VO_ID, COMP_DATA.VO_ID);
            case FOLDER -> folderStructureQueryBaseUsingVersionAndData(
                    primaryTable, dataTable, folderFields, columnFields);
            default -> throw new IllegalArgumentException("%s has no version table".formatted(aq.getType()));
        };
    }

    private static <T> SelectJoinStep<Record> structureQueryBaseUsingVersionAndDataTable(
            Stream<Field<?>> columnFields,
            Table<?> primaryTable,
            Table<?> dataTable,
            Field<T> versionField,
            Field<T> dataField) {
        SelectJoinStep<Record> from =
                DSL.select(columnFields.toArray(SelectFieldOrAsterisk[]::new)).from(primaryTable);
        return dataTable == null
                ? from
                : from.join(dataTable).on(primaryTable.field(versionField).eq(dataTable.field(dataField)));
    }

    private static SelectJoinStep<Record> folderStructureQueryBaseUsingVersionAndData(
            final Table<?> primaryTable,
            final Table<?> dataTable,
            final Stream<AslFolderItemIdVirtualField> folderFields,
            Stream<Field<?>> columnFields) {
        Optional<AslFolderItemIdVirtualField> folderItemAslField = folderFields.findFirst();

        final Condition onCondition = primaryTable
                .field(EHR_FOLDER_VERSION.EHR_ID)
                .eq(dataTable.field(EHR_FOLDER_DATA.EHR_ID))
                .and(primaryTable
                        .field(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX)
                        .eq(dataTable.field(EHR_FOLDER_DATA.EHR_FOLDERS_IDX)));

        if (folderItemAslField.isEmpty()) {
            return DSL.select(columnFields.toArray(SelectFieldOrAsterisk[]::new))
                    .from(primaryTable)
                    .join(dataTable)
                    .on(onCondition);
        } else {
            AslFolderItemIdVirtualField itemIdVirtualField = folderItemAslField.get();
            Pair<Table<?>, List<SelectFieldOrAsterisk>> tableToSelect =
                    buildFolderItemIdNestedSelect(dataTable, itemIdVirtualField, false);

            // we need all fields at this point + the item id array
            Table<?> joinTable = tableToSelect.getLeft();
            List<SelectFieldOrAsterisk> selectFields = tableToSelect.getRight();

            return DSL.select(Stream.concat(columnFields, selectFields.stream()).toArray(SelectFieldOrAsterisk[]::new))
                    .from(primaryTable)
                    .join(joinTable)
                    .on(onCondition);
        }
    }

    @Nonnull
    private static SelectJoinStep<Record> structureQueryBaseUsingDataTable(
            AslStructureQuery aq,
            Table<?> primaryTable,
            Stream<Field<?>> columnFields,
            Stream<AslFolderItemIdVirtualField> folderFields) {

        if (aq.getType() == AslSourceRelation.FOLDER) {

            Optional<AslFolderItemIdVirtualField> columnField = folderFields.findFirst();
            if (columnField.isPresent()) {
                AslFolderItemIdVirtualField column = columnField.get();
                Pair<Table<?>, List<SelectFieldOrAsterisk>> tableToSelect =
                        buildFolderItemIdNestedSelect(primaryTable, column, true);

                // we need all fields at this point + the item id array
                Table<?> joinTable = tableToSelect.getLeft();
                List<SelectFieldOrAsterisk> selectFields = tableToSelect.getRight();

                // We join by num to reduce the parent child scanning to a single folder
                // on "sF_f2_0_data_sq"."num" = "sF_f2_0sq"."num"
                // and "sF_f2_0_data_sq"."ehr_id" = "sF_f2_0sq"."ehr_id"
                // and "sF_f2_0_data_sq"."ehr_folders_idx" = "sF_f2_0sq"."ehr_folders_idx"
                Condition onCondition = primaryTable
                        .field(EHR_FOLDER_DATA.NUM)
                        .eq(joinTable.field(EHR_FOLDER_DATA.NUM))
                        .and(primaryTable.field(EHR_FOLDER_DATA.EHR_ID).eq(joinTable.field(EHR_FOLDER_DATA.EHR_ID)))
                        .and(primaryTable
                                .field(EHR_FOLDER_DATA.EHR_FOLDERS_IDX)
                                .eq(joinTable.field(EHR_FOLDER_DATA.EHR_FOLDERS_IDX)));

                return DSL.select(Stream.concat(columnFields, selectFields.stream())
                                .toArray(SelectFieldOrAsterisk[]::new))
                        .from(primaryTable)
                        .join(joinTable)
                        .on(onCondition);
            }
        }

        return DSL.select(columnFields.toArray(SelectFieldOrAsterisk[]::new)).from(primaryTable);
    }

    private static <T extends AslField, R> Stream<R> consumeFieldsOfType(
            Map<Class<? extends AslField>, List<AslField>> aslFields,
            Class<T> type,
            Function<? super T, ? extends R> mapper) {
        return Optional.ofNullable(aslFields.remove(type)).orElseGet(List::of).stream()
                .filter(type::isInstance)
                .map(type::cast)
                .map(mapper);
    }

    private static <T> SelectJoinStep<Record> structureQueryBaseVersion(
            Stream<Field<?>> columnFields, Table<?> primaryTable, Table<?> dataTable, TableField<?, T> tableField) {
        return DSL.select(columnFields.toArray(SelectFieldOrAsterisk[]::new))
                .from(primaryTable)
                .join(dataTable)
                .on(Objects.requireNonNull(primaryTable.field(tableField)).eq(dataTable.field(tableField)));
    }

    /**
     * select
     * jsonb_object_agg(
     *   d2."entity_idx",
     *  d2."data"
     * ) as "data"
     * from "ehr"."comp_data" d2
     * where
     * c2."ehr_id" = d2."ehr_id"
     * and c2."VO_ID" = d2."VO_ID"
     * and c2."num" <= d2."num"
     * and c2."num_cap" >= d2."num"
     * group by d2."VO_ID"
     */
    static SelectHavingStep<Record1<JSONB>> buildDataSubquery(
            AslRmObjectDataQuery aslData, AslQueryTables aslQueryToTable, Condition... additionalConditions) {
        AslQuery target = aslData.getBaseProvider();
        Table<?> targetTable = aslQueryToTable.getDataTable(target);
        AslSourceRelation type = AslUtils.getTargetType(aslData.getBase());
        final boolean isRoot = aslData.getBase() instanceof AslStructureQuery sq && sq.isRoot();

        Table<?> data = type.getDataTable().as(subqueryAlias(aslData));
        String dataFieldName = ((AslColumnField) aslData.getSelect().getFirst()).getName(true);
        Field<JSONB> jsonbField = dataAggregation(data, type).as(DSL.name(dataFieldName));

        SelectJoinStep<Record1<JSONB>> from = DSL.select(jsonbField).from(data);

        // primary key condition
        List<Field> pKeyFields = type.getPkeyFields().stream()
                .map((TableField<?, ?> field) -> {
                    Field f = data.field(field);
                    // add EQ to WHERE
                    from.where(
                            FieldUtils.aliasedField(targetTable, aslData, field).eq(f));
                    return f;
                })
                .toList();

        final Condition[] conditions;
        if (isRoot) {
            //  no hierarch constraints needed
            conditions = additionalConditions;
        } else {
            conditions = ArrayUtils.addFirst(
                    additionalConditions,
                    Objects.requireNonNull(data.field(COMP_DATA.NUM))
                            .between(
                                    FieldUtils.aliasedField(targetTable, aslData, COMP_DATA.NUM),
                                    FieldUtils.aliasedField(targetTable, aslData, COMP_DATA.NUM_CAP)));
        }

        return from.where(conditions).groupBy(pKeyFields);
    }

    /**
     * The aggregated jsonb can be processed by DbToRmFormat::reconstructFromDbFormat
     *
     * @return
     */
    private static JSONObjectAggNullStep<JSONB> dataAggregation(Table<?> dataTable, AslSourceRelation type) {

        Field<String> keyField = dataTable.field(COMP_DATA.ENTITY_IDX);
        Field<JSONB> dataField = dataTable.field(COMP_DATA.DATA);

        Field<JSONB> valueField;
        if (type == AslSourceRelation.FOLDER) {
            Field<UUID[]> uuidsField = dataTable.field(EhrFolderData.EHR_FOLDER_DATA.ITEM_UUIDS);
            valueField = DSL.case_()
                    .when(DSL.cardinality(uuidsField).eq(DSL.inline(0)), dataField)
                    .else_(AdditionalSQLFunctions.jsonb_set(
                            dataField,
                            AdditionalSQLFunctions.array_to_jsonb(uuidsField),
                            DbToRmFormat.FOLDER_ITEMS_UUID_ARRAY_ALIAS));
        } else {
            valueField = dataField;
        }
        return DSL.jsonbObjectAgg(keyField, valueField);
    }
}
