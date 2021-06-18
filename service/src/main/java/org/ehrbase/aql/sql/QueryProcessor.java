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

import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.aql.compiler.TopAttributes;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.Variables;
import org.ehrbase.aql.sql.binding.*;
import org.ehrbase.aql.sql.postprocessing.RawJsonTransform;
import org.ehrbase.aql.sql.queryimpl.MultiFields;
import org.ehrbase.aql.sql.queryimpl.MultiFieldsMap;
import org.ehrbase.aql.sql.queryimpl.TemplateMetaData;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.service.IntrospectService;
import org.jooq.*;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import java.util.*;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

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
        private boolean outputWithJson;


        AqlSelectQuery(SelectQuery<Record> selectQuery, Collection<List<QuerySteps>> querySteps, boolean outputWithJson) {
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

    public QueryProcessor(I_DomainAccess domainAccess, IntrospectService introspectCache, Contains contains, Statements statements, String serverNodeId) {
        super(introspectCache);
        this.domainAccess = domainAccess;
        this.contains = contains;
        this.statements = statements;
        this.serverNodeId = serverNodeId;
    }


    public AqlResult execute() {
        AqlSelectQuery aqlSelectQuery = buildAqlSelectQuery();

        Result<Record> result = fetchResultSet(aqlSelectQuery.getSelectQuery(), null);

        //if any jsonb data field transform them into raw json

        RawJsonTransform.toRawJson(result, aqlSelectQuery.getQuerySteps());

        List<List<String>> explainList = buildExplain(aqlSelectQuery.getSelectQuery());

        return new AqlResult(result, explainList);
    }

    public AqlSelectQuery buildAqlSelectQuery() {

        Map<String, List<QuerySteps>> cacheQuery = new HashMap<>();

        boolean containsJson = false;

        statements = new OrderByField(statements).merge();

        if (contains.getTemplates().isEmpty()) {
            if (contains.hasContains() && contains.requiresTemplateWhereClause()) {
                cacheQuery.put(NIL_TEMPLATE, buildQuerySteps(NIL_TEMPLATE));
                containsJson = false;
            } else
                cacheQuery.put(NIL_TEMPLATE, buildQuerySteps(NIL_TEMPLATE));
        } else {
            for (String templateId : contains.getTemplates()) {
                cacheQuery.put(templateId, buildQuerySteps(templateId));
            }
        }

        //assemble the query from the cache
        SelectQuery unionSetQuery = domainAccess.getContext().selectQuery();
        boolean first = true;

        for (List<QuerySteps> queryStepList : cacheQuery.values()) {

            for (QuerySteps queryStep : queryStepList) {

                SelectQuery select = queryStep.getSelectQuery();

                select.addFrom(ENTRY);

                if (!queryStep.getTemplateId().equalsIgnoreCase(NIL_TEMPLATE))
                    joinSetup.setUseEntry(true);

                select = new JoinBinder(domainAccess, select).addJoinClause(joinSetup);

                select = setLateralJoins(queryStep.getLateralJoins(), select);

                //this deals with 'contains c' which adds an implicit where on template id
                if (!queryStep.getTemplateId().equals(NIL_TEMPLATE)) {
                    select.addConditions(ENTRY.TEMPLATE_ID.eq(queryStep.getTemplateId()));
                }
                Condition condition = queryStep.getWhereCondition();
                if (condition != null)
                    select.addConditions(Operator.AND, condition);

                if (first) {
                    unionSetQuery = select;
                    first = false;
                } else
                    unionSetQuery.union(select);

                containsJson = containsJson || queryStep.isContainsJson();
            }

        }


        // Add function or Distinct
        SuperQuery superQuery = new SuperQuery(domainAccess, statements.getVariables(), unionSetQuery, containsJson);
        if (new Variables(statements.getVariables()).hasDefinedDistinct() || new Variables(statements.getVariables()).hasDefinedFunction()) {
            unionSetQuery = superQuery.select();
            if (statements.getOrderAttributes() != null && !statements.getOrderAttributes().isEmpty()) {
                unionSetQuery = superQuery.setOrderBy(statements.getOrderAttributes(), unionSetQuery);
            }
            containsJson = superQuery.isOutputWithJson();
        } else if (statements.getOrderAttributes() != null && !statements.getOrderAttributes().isEmpty()) {
            unionSetQuery = superQuery.selectOrderBy(statements.getOrderAttributes());
            containsJson = superQuery.isOutputWithJson();
        }

        // Add Top , Limit or Offset; Top and Limit can not be both present.
        LimitBinding limitBinding = new LimitBinding(Optional
                .ofNullable(statements.getTopAttributes())
                .map(TopAttributes::getWindow)
                .orElse(statements.getLimitAttribute()), statements.getOffsetAttribute(), unionSetQuery);

        unionSetQuery = limitBinding.bind();

        return new AqlSelectQuery(unionSetQuery, cacheQuery.values(), containsJson);
    }

    private List<QuerySteps> buildQuerySteps(String templateId) {

        List<QuerySteps> queryStepsList = new ArrayList<>();

        SelectBinder selectBinder = new SelectBinder(domainAccess, introspectCache, contains, statements, serverNodeId);

        MultiFieldsMap multiSelectFieldsMap = new MultiFieldsMap(selectBinder.bind(templateId));

        joinSetup = joinSetup.merge(selectBinder.getCompositionAttributeQuery().getJoinSetup());

        WhereMultiFields whereMultiFields = new WhereMultiFields(domainAccess, introspectCache, contains, statements.getWhereClause(), serverNodeId);
        MultiFieldsMap multiWhereFieldsMap = new MultiFieldsMap(whereMultiFields.bind(templateId));

        joinSetup = joinSetup.merge(whereMultiFields.getJoinSetup());

        int selectCursor = 0;
        int whereCursor = 0;

        int selectCursorMax = multiSelectFieldsMap.upperPathBoundary();
        int whereCursorMax = multiWhereFieldsMap.upperPathBoundary();


        //build the actual sets of fields depending on the generated multi fields
        //...

        SelectQuery<?> select = domainAccess.getContext().selectQuery();


        while (whereCursorMax == 0 || whereCursor < whereCursorMax) {

            while (selectCursor < selectCursorMax) {
                for (Iterator<MultiFields> it = multiSelectFieldsMap.multiFieldsIterator(); it.hasNext(); ) {
                    MultiFields multiSelectFields = it.next();
                    select.addSelect(multiSelectFields.getQualifiedFieldOrLast(selectCursor).getSQLField());
                }

                Condition condition = selectBinder.getWhereConditions(templateId, whereCursor, multiWhereFieldsMap);
                List<Table<?>> joins = lateralJoins(templateId);

                queryStepsList.add(
                        new QuerySteps(
                                select,
                                condition,
                                joins,
                                templateId,
                                selectBinder.getCompositionAttributeQuery(),
                                selectBinder.getJsonDataBlock(),
                                false));
                selectCursor++;
                //re-initialize select
                select = domainAccess.getContext().selectQuery();
            }
            if (whereCursorMax == 0) //no where clause
                break;
            whereCursor++;
            selectCursor = 0;
            //re-initialize select
            select = domainAccess.getContext().selectQuery();

        }
        return queryStepsList;
    }

    private SelectQuery<?> setLateralJoins(List<Table<?>> lateralJoins, SelectQuery<?> selectQuery) {
        for (Table<?> joinLateralTable : lateralJoins) {
                selectQuery.addFrom(joinLateralTable);
        }

        return selectQuery;
    }


    private List<Table<?>> lateralJoins(String templateId) {
        List<Table<?>> lateralJoinsList = new ArrayList<>();

        for (Object item : statements.getWhereClause()) {
            if (item instanceof I_VariableDefinition && ((I_VariableDefinition) item).isLateralJoin(templateId)) {
                if (((I_VariableDefinition) item).getLateralJoinTable(templateId) == null)
                    throw new IllegalStateException("unresolved lateral join for template:"+templateId+", path:"+((I_VariableDefinition) item).getPath());
                lateralJoinsList.add(DSL.lateral(((I_VariableDefinition) item).getLateralJoinTable(templateId)));
            }
        }

        return lateralJoinsList;
    }

    private Result<Record> fetchResultSet(Select<?> select, Result<Record> result) {
        Result<Record> intermediary;
        try {
            intermediary = (Result<Record>) select.fetch();
        } catch (Exception e) {

            String reason = "Could not perform SQL query:" + e.getCause() +
                    ", AQL expression:" +
                    statements.getParsedExpression() +
                    ", Translated SQL:" +
                    select.getSQL();
            throw new IllegalArgumentException(reason);
        }
        if (result != null) {
            result.addAll(intermediary);
        } else if (intermediary != null) {
            result = intermediary;
        }
        return result;
    }

    private List<List<String>> buildExplain(Select<?> select) {
        List<List<String>> explainList = new ArrayList<>();

        DSLContext pretty = DSL.using(domainAccess.getContext().dialect(), new Settings().withRenderFormatted(true));
        String sql = pretty.render(select);
        List<String> details = new ArrayList<>();
        details.add(sql);
        for (Param<?> parameter : select.getParams().values()) {
            details.add(parameter.getValue().toString());
        }
        explainList.add(details);
        return explainList;
    }
}
