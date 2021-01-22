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
import org.ehrbase.aql.definition.Variables;
import org.ehrbase.aql.sql.binding.*;
import org.ehrbase.aql.sql.postprocessing.RawJsonTransform;
import org.ehrbase.aql.sql.queryimpl.TemplateMetaData;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
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
     */
    public static class AqlSelectQuery {
        private final SelectQuery<Record> selectQuery;
        private final Collection<QuerySteps> querySteps;
        private boolean outputWithJson;


        AqlSelectQuery(SelectQuery<Record> selectQuery, Collection<QuerySteps> querySteps, boolean outputWithJson) {
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

        Collection<QuerySteps> getQuerySteps() {
            return querySteps;
        }
    }

    private final I_DomainAccess domainAccess;
    private final I_KnowledgeCache knowledgeCache;
    private final Contains contains;
    private Statements statements;
    private final String serverNodeId;

    public QueryProcessor(I_DomainAccess domainAccess, I_KnowledgeCache knowledgeCache, IntrospectService introspectCache, Contains contains, Statements statements, String serverNodeId) {
        super(introspectCache);
        this.domainAccess = domainAccess;
        this.knowledgeCache = knowledgeCache;
        this.contains = contains;
        this.statements = statements;
        this.serverNodeId = serverNodeId;
    }


    public AqlResult execute() {
        AqlSelectQuery aqlSelectQuery = buildAqlSelectQuery();

        Result<Record> result = fetchResultSet(aqlSelectQuery.getSelectQuery(), null);

        //if any jsonb data field transform them into raw json
        if (aqlSelectQuery.isOutputWithJson() && knowledgeCache != null) {
            RawJsonTransform.toRawJson(result, aqlSelectQuery.getQuerySteps());
        }

        List<List<String>> explainList = buildExplain(aqlSelectQuery.getSelectQuery());

        return new AqlResult(result, explainList);
    }

    public AqlSelectQuery buildAqlSelectQuery() {

        Map<String, QuerySteps> cacheQuery = new HashMap<>();

        boolean isOutputWithJson = true;

        statements = new OrderByField(statements).merge();

        if (contains.getTemplates().isEmpty()){
            if (contains.hasContains() && contains.useSimpleCompositionContains())
                cacheQuery.put(NIL_TEMPLATE, buildNullSelect(NIL_TEMPLATE));
            else
                cacheQuery.put(NIL_TEMPLATE, buildQuerySteps(NIL_TEMPLATE));
        }
        else {
            for (String templateId : contains.getTemplates()) {
                cacheQuery.put(templateId, buildQuerySteps(templateId));
            }
        }

        //assemble the query from the cache
        SelectQuery unionSetQuery = domainAccess.getContext().selectQuery();
        boolean first = true;
        for (QuerySteps queryStep : cacheQuery.values()) {

            SelectQuery select = queryStep.getSelectQuery();

            select.addFrom(ENTRY);

            if (!queryStep.getTemplateId().equalsIgnoreCase(NIL_TEMPLATE))
                queryStep.getCompositionAttributeQuery().setUseEntry(true);

            select = new JoinBinder(domainAccess, select).addJoinClause(queryStep.getCompositionAttributeQuery());

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
                unionSetQuery.unionAll(select);

        }


        // Add function or Distinct
        if (new Variables(statements.getVariables()).hasDefinedDistinct() || new Variables(statements.getVariables()).hasDefinedFunction()) {
            SuperQuery superQuery = new SuperQuery(domainAccess, statements.getVariables(), unionSetQuery);
            unionSetQuery = superQuery.select();
            if (statements.getOrderAttributes() != null && !statements.getOrderAttributes().isEmpty()){
                unionSetQuery = superQuery.setOrderBy(statements.getOrderAttributes(), unionSetQuery);
            }
            isOutputWithJson = superQuery.isOutputWithJson();
        }
        else if (statements.getOrderAttributes() != null && !statements.getOrderAttributes().isEmpty()) {
            unionSetQuery = new SuperQuery(domainAccess, statements.getVariables(), unionSetQuery).selectOrderBy(statements.getOrderAttributes());
        }

        // Add Top , Limit or Offset; Top and Limit can not be both present.
        LimitBinding limitBinding = new LimitBinding(Optional
                .ofNullable(statements.getTopAttributes())
                .map(TopAttributes::getWindow)
                .orElse(statements.getLimitAttribute()), statements.getOffsetAttribute(), unionSetQuery);

        unionSetQuery = limitBinding.bind();

        if (!isOutputWithJson) //superceded by aggregate
            return new AqlSelectQuery(unionSetQuery, cacheQuery.values(), isOutputWithJson);
        else
            return new AqlSelectQuery(unionSetQuery, cacheQuery.values(), cacheQuery.values().stream().anyMatch(QuerySteps::isContainsJson));
    }

    private QuerySteps buildQuerySteps(String templateId) {
        SelectBinder selectBinder = new SelectBinder(domainAccess, introspectCache, contains, statements, serverNodeId);

        SelectQuery<?> select = selectBinder.bind(templateId);
        return new QuerySteps(select,
                selectBinder.getWhereConditions(templateId),
                templateId,
                selectBinder.getCompositionAttributeQuery(),
                selectBinder.getJsonDataBlock(), selectBinder.containsJQueryPath());
    }

    private QuerySteps buildNullSelect(String templateId) {
        SelectBinder selectBinder = new SelectBinder(domainAccess, introspectCache, contains, statements, serverNodeId);

        SelectQuery<?> select = selectBinder.bind(templateId);
        return new QuerySteps(select,
                DSL.condition("1 = 0"),
                templateId,
                selectBinder.getCompositionAttributeQuery(),
                selectBinder.getJsonDataBlock(), selectBinder.containsJQueryPath());
    }


    private Result<Record> fetchResultSet(Select<?> select, Result<Record> result) {
        Result<Record> intermediary;
        try {
            intermediary = (Result<Record>) select.fetch();
        } catch (Exception e){

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
