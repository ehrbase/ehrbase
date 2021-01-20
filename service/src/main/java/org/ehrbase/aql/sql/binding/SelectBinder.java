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
import org.ehrbase.aql.sql.queryimpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery;
import org.ehrbase.aql.sql.queryimpl.ObjectQuery;
import org.ehrbase.aql.sql.queryimpl.TemplateMetaData;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheService;
import org.jooq.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind the abstract representation of a SELECT clause into a SQL expression
 * Created by christian on 5/4/2016.
 */
@SuppressWarnings({"java:S3776","java:S3740","java:S1452"})
public class SelectBinder extends TemplateMetaData implements ISelectBinder {

    public static final String DATA = "data";
    private final JsonbEntryQuery jsonbEntryQuery;
    private final CompositionAttributeQuery compositionAttributeQuery;
    private final PathResolver pathResolver;
    private final VariableDefinitions variableDefinitions;
    private final List<JsonbBlockDef> jsonDataBlock = new ArrayList<>();
    private final DSLContext context;
    private final WhereBinder whereBinder;

    SelectBinder(I_DomainAccess domainAccess, IntrospectService introspectCache, PathResolver pathResolver, VariableDefinitions variableDefinitions, List whereClause, String serverNodeId) {
        super(introspectCache);
        this.context = domainAccess.getContext();
        this.pathResolver = pathResolver;

        this.variableDefinitions = variableDefinitions;
        this.jsonbEntryQuery = new JsonbEntryQuery(domainAccess, introspectCache, pathResolver);
        this.compositionAttributeQuery = new CompositionAttributeQuery(domainAccess, pathResolver, serverNodeId, introspectCache);
        this.whereBinder = new WhereBinder(domainAccess, jsonbEntryQuery, compositionAttributeQuery, whereClause, pathResolver.getMapper());
    }

    private SelectBinder(I_DomainAccess domainAccess, IntrospectService introspectCache, IdentifierMapper mapper, VariableDefinitions variableDefinitions, List whereClause, String serverNodeId) {
        this(domainAccess, introspectCache, new PathResolver((KnowledgeCacheService)introspectCache, mapper), variableDefinitions, whereClause, serverNodeId);
    }

    public SelectBinder(I_DomainAccess domainAccess, IntrospectService introspectCache, Contains contains, Statements statements, String serverNodeId) {
        this(domainAccess, introspectCache, contains.getIdentifierMapper(), statements.getVariables(), statements.getWhereClause(), serverNodeId);
    }


    /**
     * bind with path resolution depending on composition
     *
     * @param templateId
     * @return
     */
    public SelectQuery<Record> bind(String templateId) {
        ObjectQuery.reset();

        SelectQuery<Record> selectQuery = context.selectQuery();

        while (variableDefinitions.hasNext()) {
            I_VariableDefinition variableDefinition = variableDefinitions.next();
            if (variableDefinition.isFunction() || variableDefinition.isExtension()) {
                continue;
            }
            String identifier = variableDefinition.getIdentifier();
            String className = pathResolver.classNameOf(identifier);

            ExpressionField expressionField = new ExpressionField(variableDefinition, jsonbEntryQuery, compositionAttributeQuery);

            Field<?> field = expressionField.toSql(className, templateId, identifier);

            handleJsonDataBlock(expressionField, field, expressionField.getRootJsonKey(), expressionField.getOptionalPath());

            if (field == null) { //the field cannot be resolved with containment (f.e. empty DB)
                continue;
            }
            selectQuery.addSelect(field);
            ObjectQuery.inc();
        }

        return selectQuery;
    }

    private void handleJsonDataBlock(ExpressionField expressionField, Field field, String rootJsonKey, String optionalPath){
        if (expressionField.isContainsJsonDataBlock())
            jsonDataBlock.add(new JsonbBlockDef(optionalPath == null ? expressionField.getJsonbItemPath() : optionalPath, field, rootJsonKey));
    }


    public Condition getWhereConditions(String templateId) {

        return whereBinder.bind(templateId);
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

}
