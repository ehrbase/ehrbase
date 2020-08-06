/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

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

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryImpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryImpl.JsonbEntryQuery;
import org.ehrbase.aql.sql.queryImpl.TemplateMetaData;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheService;
import org.jooq.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bind the abstract representation of a SELECT clause into a SQL expression
 * Created by christian on 5/4/2016.
 */
public class SelectBinder extends TemplateMetaData implements I_SelectBinder {

    private final JsonbEntryQuery jsonbEntryQuery;
    private final CompositionAttributeQuery compositionAttributeQuery;
    private final PathResolver pathResolver;
    private final VariableDefinitions variableDefinitions;
    private final List<JsonbBlockDef> jsonDataBlock = new ArrayList<>();
    private final DSLContext context;
    private final WhereBinder whereBinder;

    private boolean isWholeComposition = false;

    SelectBinder(DSLContext context, IntrospectService introspectCache, PathResolver pathResolver, VariableDefinitions variableDefinitions, List whereClause, String serverNodeId) {
        super(introspectCache);
        this.context = context;
        this.pathResolver = pathResolver;

        this.variableDefinitions = variableDefinitions;
        this.jsonbEntryQuery = new JsonbEntryQuery(context, introspectCache, pathResolver);
        this.compositionAttributeQuery = new CompositionAttributeQuery(context, pathResolver, serverNodeId, introspectCache);
        this.whereBinder = new WhereBinder(jsonbEntryQuery, compositionAttributeQuery, whereClause, pathResolver.getMapper());
    }

    private SelectBinder(DSLContext context, IntrospectService introspectCache, IdentifierMapper mapper, VariableDefinitions variableDefinitions, List whereClause, String serverNodeId) {
        this(context, introspectCache, new PathResolver((KnowledgeCacheService)introspectCache, mapper), variableDefinitions, whereClause, serverNodeId);
    }

    public SelectBinder(DSLContext context, IntrospectService introspectCache, Contains contains, Statements statements, String serverNodeId) {
        this(context, introspectCache, contains.getIdentifierMapper(), statements.getVariables(), statements.getWhereClause(), serverNodeId);
    }


    /**
     * bind with path resolution depending on composition
     *
     * @param template_id
     * @return
     */
    public SelectQuery<Record> bind(String template_id) {
//        pathResolver.resolvePaths(template_id, comp_id);

        jsonbEntryQuery.reset();

        SelectQuery<Record> selectQuery = context.selectQuery();

        while (variableDefinitions.hasNext()) {
            I_VariableDefinition variableDefinition = variableDefinitions.next();
            if (variableDefinition.isFunction() || variableDefinition.isExtension()) {
                continue;
            }
            String identifier = variableDefinition.getIdentifier();
            String className = pathResolver.classNameOf(identifier);

            ExpressionField expressionField = new ExpressionField(variableDefinition, jsonbEntryQuery, compositionAttributeQuery);

            Field<?> field = expressionField.toSql(className, template_id, identifier);

            handleJsonDataBlock(expressionField, field, expressionField.getRootJsonKey(), expressionField.getOptionalPath());
//            field = DSL.field(field);
            if (field == null) { //the field cannot be resolved with containment (f.e. empty DB)
                continue;
            }
            selectQuery.addSelect(field);
            jsonbEntryQuery.inc();
        }

        return selectQuery;
    }

    private void handleJsonDataBlock(ExpressionField expressionField, Field field, String rootJsonKey, String optionalPath){
        if (expressionField.isContainsJsonDataBlock())
            jsonDataBlock.add(new JsonbBlockDef(optionalPath == null ? expressionField.getJsonbItemPath() : optionalPath, field, rootJsonKey));
    }


    public Condition getWhereConditions(String templateId, UUID comp_id) {

        return whereBinder.bind(templateId, comp_id);
    }

    public boolean containsJQueryPath() {
        return jsonbEntryQuery.isContainsJqueryPath();
    }


    public CompositionAttributeQuery getCompositionAttributeQuery() {
        return compositionAttributeQuery;
    }

    public List<JsonbBlockDef> getJsonDataBlock() {
        return jsonDataBlock;
    }

    public SelectBinder setUsePgExtensions(boolean usePgExtensions) {
        whereBinder.setUsePgExtensions(usePgExtensions);
        return this;
    }

}
