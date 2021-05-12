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
import org.ehrbase.aql.definition.ConstantDefinition;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryimpl.*;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheService;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Iterator;
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
        this.whereBinder = new WhereBinder(domainAccess, jsonbEntryQuery, compositionAttributeQuery, whereClause, pathResolver);
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
    public List<MultiFields> bind(String templateId) {
        ObjectQuery.reset();

//        SelectQuery<Record> selectQuery = context.selectQuery();

        List<MultiFields> multiFieldsList = new ArrayList<>();

        while (variableDefinitions.hasNext()) {
            I_VariableDefinition variableDefinition = variableDefinitions.next();
           MultiFields multiFields;
            if (variableDefinition.isFunction() || variableDefinition.isExtension()) {
                continue;
            }
            else if (variableDefinition.isConstant()){
                multiFields = new MultiFields(variableDefinition, new ConstantField(variableDefinition).toSql(), templateId);

            }
            else {
                String identifier = variableDefinition.getIdentifier();
                String className = pathResolver.classNameOf(identifier);

                ExpressionField expressionField = new ExpressionField(variableDefinition, jsonbEntryQuery, compositionAttributeQuery);

                multiFields = expressionField.toSql(className, templateId, identifier, IQueryImpl.Clause.SELECT);

                handleJsonDataBlock(multiFields);

                if (multiFields == null) { //the field cannot be resolved with containment (f.e. empty DB)
                    continue;
                }
            }
            multiFieldsList.add(multiFields);
            ObjectQuery.inc();
        }

        return multiFieldsList;
    }

    private void handleJsonDataBlock(MultiFields multiFields){
        if (!multiFields.isEmpty()) {
            Iterator<QualifiedAqlField> qualifiedAqlFieldIterator = multiFields.iterator();
            while(qualifiedAqlFieldIterator.hasNext()) {
                QualifiedAqlField aqlField = qualifiedAqlFieldIterator.next();
                if (aqlField.isJsonDataBlock())
                    jsonDataBlock.add(new JsonbBlockDef(aqlField.getOptionalPath() == null ? aqlField.getJsonbItemPath() : aqlField.getOptionalPath(), aqlField.getSQLField(), multiFields.getRootJsonKey()));
            }
        }
    }


    public Condition getWhereConditions(int whereCursor, MultiFieldsMap multiFieldsMap) {

        return whereBinder.bind(whereCursor, multiFieldsMap);
    }

//    public boolean containsJQueryPath() {
//        return jsonbEntryQuery.isContainsJqueryPath();
//    }


    public CompositionAttributeQuery getCompositionAttributeQuery() {
        return compositionAttributeQuery;
    }

    public List<JsonbBlockDef> getJsonDataBlock() {
        return jsonDataBlock;
    }

}
