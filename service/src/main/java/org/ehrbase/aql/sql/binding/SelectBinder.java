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

import com.nedap.archie.rm.datavalues.DataValue;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.postprocessing.I_RawJsonTransform;
import org.ehrbase.aql.sql.queryImpl.*;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.validation.constraints.util.SnakeToCamel;
import org.jooq.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

/**
 * Bind the abstract representation of a SELECT clause into a SQL expression
 * Created by christian on 5/4/2016.
 */
public class SelectBinder extends TemplateMetaData implements I_SelectBinder {

    private final JsonbEntryQuery jsonbEntryQuery;
    private final CompositionAttributeQuery compositionAttributeQuery;
    private final PathResolver pathResolver;
    private final List<I_VariableDefinition> selectVariableDefinitions;
    private final List<JsonbBlockDef> jsonDataBlock = new ArrayList<>();
    private final DSLContext context;
    private final WhereBinder whereBinder;

    private boolean isWholeComposition = false;
    private boolean usePgExtensions = true;

    public SelectBinder(DSLContext context, IntrospectService introspectCache, PathResolver pathResolver, List<I_VariableDefinition> definitions, List whereClause, String serverNodeId, String entry_root) {
        super(introspectCache);
        this.context = context;
        this.pathResolver = pathResolver;

        this.selectVariableDefinitions = definitions;
        this.jsonbEntryQuery = new JsonbEntryQuery(context, introspectCache, pathResolver, entry_root);
        this.compositionAttributeQuery = new CompositionAttributeQuery(context, pathResolver, serverNodeId, entry_root, introspectCache);
        this.whereBinder = new WhereBinder(jsonbEntryQuery, compositionAttributeQuery, whereClause, pathResolver.getMapper());
    }

    public SelectBinder(DSLContext context, IntrospectService introspectCache, IdentifierMapper mapper, List<I_VariableDefinition> definitions, List whereClause, String serverNodeId, String entry_root) {
        this(context, introspectCache, new PathResolver(context, mapper), definitions, whereClause, serverNodeId, entry_root);
    }

    public SelectBinder(DSLContext context, IntrospectService introspectCache, Contains contains, Statements statements, String serverNodeId, String entry_root) {
        this(context, introspectCache, contains.getIdentifierMapper(), statements.getVariables(), statements.getWhereClause(), serverNodeId, entry_root);
    }


    /**
     * bind with path resolution depending on composition
     *
     * @param comp_id
     * @return
     */
    public SelectQuery<Record> bind(String template_id, UUID comp_id) {
        pathResolver.resolvePaths(template_id, comp_id);

        jsonbEntryQuery.reset();

        SelectQuery<Record> selectQuery = context.selectQuery();

        boolean containsJsonDataBlock = false;

        for (I_VariableDefinition variableDefinition : selectVariableDefinitions) {
            if (variableDefinition.isFunction() || variableDefinition.isExtension()) {
                continue;
            }
            String identifier = variableDefinition.getIdentifier();
            String className = pathResolver.classNameOf(identifier);
            Field<?> field;
            SelectQuery<?> subSelect = context.selectQuery();
            switch (className) {
                case "COMPOSITION":
                    if (variableDefinition.getPath() != null && variableDefinition.getPath().startsWith("content")) {
                        field = jsonbEntryQuery.makeField(template_id, comp_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
                        handleJsonDataBlock(jsonbEntryQuery, field, null, variableDefinition.getPath());
                    } else {
                        field = compositionAttributeQuery.makeField(template_id, comp_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
                        handleJsonDataBlock(compositionAttributeQuery, field, null, variableDefinition.getPath());
                    }
                    break;
                case "EHR":
                    field = compositionAttributeQuery.makeField(template_id, comp_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
                    handleJsonDataBlock(compositionAttributeQuery, field, null, variableDefinition.getPath());
                    break;
                default:
                    field = jsonbEntryQuery.makeField(template_id, comp_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
                    containsJsonDataBlock = containsJsonDataBlock | jsonbEntryQuery.isJsonDataBlock();
                    if (jsonbEntryQuery.isJsonDataBlock() ) {

                        if (jsonbEntryQuery.getItemType() != null){
                            Class itemClass = ArchieRMInfoLookup.getInstance().getClass(jsonbEntryQuery.getItemType());
                            if (DataValue.class.isAssignableFrom(itemClass)) {
                                VariableAqlPath variableAqlPath = new VariableAqlPath(variableDefinition.getPath());
                                if (variableAqlPath.getSuffix().equals("value")) { //assumes this is a data value within an ELEMENT
                                    I_VariableDefinition variableDefinition1 = variableDefinition.clone();
                                    variableDefinition1.setPath(variableAqlPath.getInfix());
                                    field = jsonbEntryQuery.makeField(template_id, comp_id, identifier, variableDefinition1, I_QueryImpl.Clause.SELECT);
                                    handleJsonDataBlock(jsonbEntryQuery, field, variableAqlPath.getSuffix(), null);
                                } else
                                    handleJsonDataBlock(jsonbEntryQuery, field, null, null);
                            } else
                                //add this field to the list of column to format as RAW JSON
                                handleJsonDataBlock(jsonbEntryQuery, field, null, null);
                        }
                        else
                            handleJsonDataBlock(jsonbEntryQuery, field, null, null);
                    }
                    break;
            }
//            field = DSL.field(field);
            if (field == null) {
                throw new IllegalArgumentException("Field expression is not supported or invalid :" + variableDefinition);
            }
            selectQuery.addSelect(field);
            jsonbEntryQuery.inc();
        }


//        if (containsJsonDataBlock) {
//            //add a template column for transformation
//            selectQuery.addSelect(ENTRY.TEMPLATE_ID.as(I_RawJsonTransform.TEMPLATE_ID));
//        }


        return selectQuery;
    }

    private void handleJsonDataBlock(I_QueryImpl queryImpl, Field field, String rootJsonKey, String optionalPath){
        if (queryImpl.isJsonDataBlock())
            jsonDataBlock.add(new JsonbBlockDef(optionalPath == null ? queryImpl.getJsonbItemPath() : optionalPath, field, rootJsonKey));
    }


    public Condition getWhereConditions(String templateId, UUID comp_id) {

        return whereBinder.bind(templateId, comp_id);
    }


//    public PathResolver getPathResolver() {
//        return pathResolver;
//    }
//
//    public boolean hasEhrIdExpression() {
//        return compositionAttributeQuery.containsEhrId();
//    }
//
//    public String getEhrIdAlias() {
//        return compositionAttributeQuery.getEhrIdAlias();
//    }
//
//    public boolean isCompositionIdFiltered() {
//        return compositionAttributeQuery.isCompositionIdFiltered();
//    }
//
//    public boolean isEhrIdFiltered() {
//        return compositionAttributeQuery.isEhrIdFiltered();
//    }

    public boolean containsJQueryPath() {
        return jsonbEntryQuery.isContainsJqueryPath();
    }


    public CompositionAttributeQuery getCompositionAttributeQuery() {
        return compositionAttributeQuery;
    }

    public List<JsonbBlockDef> getJsonDataBlock() {
        return jsonDataBlock;
    }

    public boolean isWholeComposition() {
        return isWholeComposition;
    }

    public SelectBinder setUsePgExtensions(boolean usePgExtensions) {
        this.usePgExtensions = usePgExtensions;
        whereBinder.setUsePgExtensions(usePgExtensions);
        return this;
    }
}
