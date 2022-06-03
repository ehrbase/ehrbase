/*
* Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
* Stefan Spiska (Vitasystems GmbH).

* This file is part of Project EHRbase

* Copyright (c) 2015 Christian Chevalley
* This file is part of Project Ethercis
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ehrbase.aql.sql;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.aql.compiler.TopAttributes;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.LateralJoinDefinition;
import org.ehrbase.aql.definition.Variables;
import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.binding.LimitBinding;
import org.ehrbase.aql.sql.binding.OrderByField;
import org.ehrbase.aql.sql.binding.SelectBinder;
import org.ehrbase.aql.sql.binding.SuperQuery;
import org.ehrbase.aql.sql.binding.VariableDefinitions;
import org.ehrbase.aql.sql.binding.WhereMultiFields;
import org.ehrbase.aql.sql.postprocessing.RawJsonTransform;
import org.ehrbase.aql.sql.queryimpl.DurationFormatter;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.MultiFields;
import org.ehrbase.aql.sql.queryimpl.MultiFieldsMap;
import org.ehrbase.aql.sql.queryimpl.MultiFieldsMultiMap;
import org.ehrbase.aql.sql.queryimpl.TemplateMetaData;
import org.ehrbase.aql.sql.queryimpl.UnknownVariableException;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.service.IntrospectService;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JoinType;
import org.jooq.Operator;
import org.jooq.Param;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.SelectQuery;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

/**
 * Perform an assembled SQL query depending on its strategy
 * <p>
 * The strategy depends on whether the query contains elements which path depends on the context
 * (e.g. a composition).
 * <ul>
 * <li>If a query contains path expression that need to be resolved, the query process consists in
 * evaluating the path for each composition (iteration)
 * <li>If the query contains only static fields (columns), a single query execution is done.
 * </ul>
 * </p>
 * <p>
 * Created by christian on 4/28/2016.
 */
@SuppressWarnings({"java:S3776", "java:S3740"})
public class QueryProcessor extends TemplateMetaData {

    public static final String NIL_TEMPLATE = "*";

    /**
     *
     */
    public static class AqlSelectQuery {
        private final SelectQuery<Record> selectQuery;
        private final Collection<List<QuerySteps>> querySteps;
        private final boolean outputWithJson;

        AqlSelectQuery(
                SelectQuery<Record> selectQuery, Collection<List<QuerySteps>> querySteps, boolean outputWithJson) {
            this.selectQuery = selectQuery;
            this.querySteps = querySteps;
            this.outputWithJson = outputWithJson;
        }

        public SelectQuery<Record> getSelectQuery() {
            return selectQuery;
        }

        public boolean isOutputWithJson() {
            return outputWithJson;
        }

        Collection<List<QuerySteps>> getQuerySteps() {
            return querySteps;
        }
    }

    private final I_DomainAccess domainAccess;
    private final Contains contains;
    private Statements statements;
    private final String serverNodeId;
    private JoinSetup joinSetup = new JoinSetup();

    public QueryProcessor(
            I_DomainAccess domainAccess,
            IntrospectService introspectCache,
            Contains contains,
            Statements statements,
            String serverNodeId) {
        super(introspectCache);
        this.domainAccess = domainAccess;
        this.contains = contains;
        this.statements = statements;
        this.serverNodeId = serverNodeId;
    }

    public AqlResult execute() {
        AqlSelectQuery aqlSelectQuery = buildAqlSelectQuery();

        Result<Record> result = fetchResultSet(aqlSelectQuery.getSelectQuery());

        // if any jsonb data field transform them into raw json
        RawJsonTransform.toRawJson(result);
        DurationFormatter.toISO8601(result);

        List<List<String>> explainList = buildExplain(aqlSelectQuery.getSelectQuery());

        return new AqlResult(result, explainList);
    }

    public AqlSelectQuery buildAqlSelectQuery() {

        Map<String, List<QuerySteps>> cacheQuery = new HashMap<>();

        boolean containsJson = false;

        statements = new OrderByField(statements).merge();

        List<QuerySteps> querySteps;

        if (contains.getTemplates().isEmpty()) {
            if (contains.hasContains() && contains.requiresTemplateWhereClause()) {
                try {
                    querySteps = buildNullSelect();
                    cacheQuery.put(NIL_TEMPLATE, querySteps);
                } catch (UnknownVariableException e) {
                    // do nothing
                }
            } else
                try {
                    querySteps = buildQuerySteps(NIL_TEMPLATE);
                    cacheQuery.put(NIL_TEMPLATE, querySteps);
                } catch (UnknownVariableException e) {
                    // do nothing
                }
        } else {
            for (String templateId : contains.getTemplates()) {
                try {
                    querySteps = buildQuerySteps(templateId);
                    cacheQuery.put(templateId, querySteps);
                } catch (UnknownVariableException e) {
                    // ignore
                }
            }
        }

        // assemble the query from the cache
        SelectQuery unionSetQuery = domainAccess.getContext().selectQuery();
        boolean first = true;

        for (List<QuerySteps> queryStepList : cacheQuery.values()) {

            for (QuerySteps queryStep : queryStepList) {

                SelectQuery select = queryStep.getSelectQuery();

                if (!queryStep.getTemplateId().equalsIgnoreCase(NIL_TEMPLATE)) joinSetup.setUseEntry(true);

                JoinBinder joinBinder = new JoinBinder(domainAccess, joinSetup);

                select.addFrom(joinBinder.initialFrom());

                select = joinBinder.addJoinClause(select);

                select = setLateralJoins(queryStep.getLateralJoins(), select);

                // this deals with 'contains c' which adds an implicit where on template id
                if (!queryStep.getTemplateId().equals(NIL_TEMPLATE)) {
                    select.addConditions(ENTRY.TEMPLATE_ID.eq(queryStep.getTemplateId()));
                }
                Condition whereCondition = queryStep.getWhereCondition();
                if (whereCondition != null) select.addConditions(Operator.AND, whereCondition);

                if (first) {
                    unionSetQuery = select;
                    first = false;
                } else unionSetQuery.union(select);
            }
        }

        // Add function or Distinct
        SuperQuery superQuery = new SuperQuery(domainAccess, statements.getVariables(), unionSetQuery, containsJson);
        if (new Variables(statements.getVariables()).hasDefinedDistinct()
                || new Variables(statements.getVariables()).hasDefinedFunction()) {
            unionSetQuery = superQuery.select();
            if (statements.getOrderAttributes() != null
                    && !statements.getOrderAttributes().isEmpty()) {
                unionSetQuery = superQuery.setOrderBy(statements.getOrderAttributes(), unionSetQuery);
            }
            containsJson = superQuery.isOutputWithJson();
        } else if (statements.getOrderAttributes() != null
                && !statements.getOrderAttributes().isEmpty()) {
            unionSetQuery = superQuery.selectOrderBy(statements.getOrderAttributes());
            containsJson = superQuery.isOutputWithJson();
        }

        // Add Top , Limit or Offset; Top and Limit can not be both present.
        LimitBinding limitBinding = new LimitBinding(
                Optional.ofNullable(statements.getTopAttributes())
                        .map(TopAttributes::getWindow)
                        .orElse(statements.getLimitAttribute()),
                statements.getOffsetAttribute(),
                unionSetQuery);

        unionSetQuery = limitBinding.bind();

        return new AqlSelectQuery(unionSetQuery, cacheQuery.values(), containsJson);
    }

    private List<QuerySteps> buildQuerySteps(String templateId) throws UnknownVariableException {

        List<QuerySteps> queryStepsList = new ArrayList<>();

        // process WHERE clause first
        WhereMultiFields whereMultiFields = new WhereMultiFields(
                domainAccess, introspectCache, contains, statements.getWhereClause(), serverNodeId);
        MultiFieldsMap multiWhereFieldsMap = new MultiFieldsMap(whereMultiFields.bind(templateId));
        joinSetup = joinSetup.merge(whereMultiFields.getJoinSetup());

        // process SELECT and check to reconciliate where and select columns
        SelectBinder selectBinder = new SelectBinder(domainAccess, introspectCache, contains, statements, serverNodeId);
        MultiFieldsMultiMap multiSelectFieldsMap = new MultiFieldsMultiMap(selectBinder.bind(templateId));
        joinSetup = joinSetup.merge(selectBinder.getCompositionAttributeQuery().getJoinSetup());

        int selectCursor = 0;
        int whereCursor = 0;

        int selectCursorMax = multiSelectFieldsMap.upperPathBoundary();
        int whereCursorMax = multiWhereFieldsMap.upperPathBoundary();

        // build the actual sets of fields depending on the generated multi fields
        // ...

        SelectQuery<?> select = domainAccess.getContext().selectQuery();

        while (whereCursorMax == 0 || whereCursor < whereCursorMax) {

            // iterate on variable
            while (selectCursor < selectCursorMax) {
                // iterate on paths for the variable
                for (Iterator<MultiFields> it = multiSelectFieldsMap.multiFieldsIterator(); it.hasNext(); ) {
                    MultiFields multiSelectFields = it.next();
                    select.addSelect(multiSelectFields
                            .getQualifiedFieldOrLast(selectCursor)
                            .getSQLField());
                }

                Condition condition = selectBinder.getWhereConditions(
                        templateId, whereCursor, multiWhereFieldsMap, selectCursor, multiSelectFieldsMap);
                if (condition != null && condition.equals(DSL.falseCondition()))
                    break; // do not add since it is always false

                List<LateralJoinDefinition> joins =
                        new ArrayList<>(lateralJoinsSelectClause(NIL_TEMPLATE, 0)); // composition attributes
                if (!templateId.equals(NIL_TEMPLATE)) {
                    joins.addAll(lateralJoinsSelectClause(templateId, selectCursor)); // select clause fields
                    joins.addAll(lateralJoinsWhereClause(templateId, whereCursor)); // where clause fields
                }

                // check whether the *same* query step is already in the list
                QuerySteps querySteps = new QuerySteps(select, condition, joins, templateId);
                if (QuerySteps.isIncludedInList(querySteps, queryStepsList)) {
                    // re-initialize select
                    selectCursor++;
                    select = domainAccess.getContext().selectQuery();

                    continue;
                }

                queryStepsList.add(querySteps);
                selectCursor++;
                // re-initialize select
                select = domainAccess.getContext().selectQuery();
            }
            if (whereCursorMax == 0) // no where clause
            break;
            whereCursor++;
            selectCursor = 0;
            // re-initialize select
            select = domainAccess.getContext().selectQuery();
        }
        return queryStepsList;
    }

    private SelectQuery<?> setLateralJoins(List<LateralJoinDefinition> lateralJoins, SelectQuery<?> selectQuery) {
        if (lateralJoins == null) return selectQuery;

        HashSet<String> usedLaterals = new HashSet<>();

        for (LateralJoinDefinition lateralJoinDefinition : lateralJoins) {

            if (usedLaterals.contains(
                    lateralJoinDefinition.getTable().getName() + "." + lateralJoinDefinition.getLateralVariable()))
                continue;

            if (lateralJoinDefinition.getCondition() == null)
                selectQuery.addJoin(lateralJoinDefinition.getTable(), lateralJoinDefinition.getJoinType());
            else
                selectQuery.addJoin(
                        lateralJoinDefinition.getTable(),
                        lateralJoinDefinition.getJoinType(),
                        lateralJoinDefinition.getCondition());

            usedLaterals.add(
                    lateralJoinDefinition.getTable().getName() + "." + lateralJoinDefinition.getLateralVariable());
        }

        return selectQuery;
    }

    private List<LateralJoinDefinition> lateralJoinsSelectClause(String templateId, int cursor) {
        List<LateralJoinDefinition> lateralJoinsList = new ArrayList<>();

        // traverse the lateral joins derived from SELECT clause
        for (VariableDefinitions it = statements.getVariables(); it.hasNext(); ) {
            I_VariableDefinition item = it.next();
            if (item != null && item.isLateralJoin(templateId)) {
                Set<LateralJoinDefinition> listOfLaterals = item.getLateralJoinDefinitions(templateId);
                int index = cursor < listOfLaterals.size() ? cursor : listOfLaterals.size() - 1;
                LateralJoinDefinition encapsulatedLateralJoinDefinition =
                        item.getLateralJoinDefinition(templateId, index);
                LateralJoinDefinition lateralJoinDefinition = new LateralJoinDefinition(
                        encapsulatedLateralJoinDefinition.getSqlExpression(),
                        DSL.lateral(encapsulatedLateralJoinDefinition.getTable()),
                        encapsulatedLateralJoinDefinition.getLateralVariable(),
                        encapsulatedLateralJoinDefinition.getJoinType(),
                        encapsulatedLateralJoinDefinition.getCondition(),
                        encapsulatedLateralJoinDefinition.getClause());
                lateralJoinsList.add(lateralJoinDefinition);
            }
        }

        return lateralJoinsList;
    }

    private List<LateralJoinDefinition> lateralJoinsWhereClause(String templateId, int cursor) {
        List<LateralJoinDefinition> lateralJoinsList = new ArrayList<>();

        for (Object item : statements.getWhereClause()) {
            if (item instanceof I_VariableDefinition && ((I_VariableDefinition) item).isLateralJoin(templateId)) {
                if (((I_VariableDefinition) item).getLateralJoinDefinitions(templateId) == null)
                    throw new IllegalStateException("unresolved lateral join for template:" + templateId + ", path:"
                            + ((I_VariableDefinition) item).getPath());
                else if (cursor
                        > ((I_VariableDefinition) item)
                                        .getLateralJoinDefinitions(templateId)
                                        .size()
                                - 1) continue;
                // check if lateral join is borrowed from SELECT clause, if so, don't add
                else if (((I_VariableDefinition) item)
                        .getLateralJoinDefinition(templateId, cursor)
                        .getClause()
                        .equals(IQueryImpl.Clause.SELECT)) continue;

                lateralJoinsList.add(new LateralJoinDefinition(
                        ((I_VariableDefinition) item)
                                .getLateralJoinDefinition(templateId, cursor)
                                .getSqlExpression(),
                        DSL.lateral(((I_VariableDefinition) item)
                                .getLateralJoinDefinition(templateId, cursor)
                                .getTable()),
                        ((I_VariableDefinition) item).getSubstituteFieldVariable(),
                        JoinType.JOIN,
                        null,
                        IQueryImpl.Clause.WHERE));
            }
        }

        return lateralJoinsList;
    }

    private Result<Record> fetchResultSet(Select<?> select) {
        Result<Record> intermediary;

        try {
            intermediary = (Result<Record>) select.fetch();
        } catch (Exception e) {

            String reason = "Could not perform SQL query:" + e.getCause() + ", AQL expression:"
                    + statements.getParsedExpression()
                    + ", Translated SQL:"
                    + select.getSQL();
            throw new IllegalArgumentException(reason);
        }
        return intermediary;
    }

    private List<List<String>> buildExplain(Select<?> select) {
        List<List<String>> explainList = new ArrayList<>();

        DSLContext pretty = DSL.using(domainAccess.getContext().dialect(), new Settings().withRenderFormatted(true));
        String sql = pretty.render(select);
        List<String> details = new ArrayList<>();
        details.add(sql);
        for (Param<?> parameter : select.getParams().values()) {
            if (parameter.getValue() != null) details.add(parameter.getValue().toString());
        }
        explainList.add(details);
        return explainList;
    }

    private List<QuerySteps> buildNullSelect() throws UnknownVariableException {

        List<QuerySteps> queryStepsList = buildQuerySteps(NIL_TEMPLATE);

        // force a null condition for these steps
        for (QuerySteps querySteps : queryStepsList) {
            querySteps.setWhereCondition(DSL.condition("1 = 0"));
        }

        return queryStepsList;
    }
}
