/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

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

package org.ehrbase.aql.sql.queryimpl;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.binding.IJoinBinder;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributePath;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.composer.ComposerResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.composition.CompositionResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.composition.FullCompositionJson;
import org.ehrbase.aql.sql.queryimpl.attribute.ehr.EhrResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.ehr.FullEhrJson;
import org.ehrbase.aql.sql.queryimpl.attribute.eventcontext.EventContextResolver;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.service.IntrospectService;
import org.jooq.Field;

import static org.ehrbase.aql.sql.QueryProcessor.NIL_TEMPLATE;

/**
 * map an AQL datavalue expression into a SQL field
 * <p>
 * Created by christian on 5/6/2016.
 */
@SuppressWarnings({"java:S3776","java:S3740"})
public class CompositionAttributeQuery extends ObjectQuery implements IQueryImpl, IJoinBinder {

    private String serverNodeId;

    protected JoinSetup joinSetup = new JoinSetup(); //used to pass join metadata to perform binding

    private final IntrospectService introspectCache;


    public CompositionAttributeQuery(I_DomainAccess domainAccess, PathResolver pathResolver, String serverNodeId, IntrospectService introspectCache) {
        super(domainAccess, pathResolver);
        this.serverNodeId = serverNodeId;
        this.introspectCache = introspectCache;
    }

    @Override
    public Field<?> makeField(String templateId, String identifier, I_VariableDefinition variableDefinition, Clause clause) {
        //resolve composition attributes and/or context
        String columnAlias = variableDefinition.getPath();
        jsonDataBlock = false;
        FieldResolutionContext fieldResolutionContext =
                new FieldResolutionContext(domainAccess.getContext(),
                        serverNodeId,
                        identifier,
                        variableDefinition,
                        clause,
                        pathResolver,
                        introspectCache,
                        pathResolver.entryRoot(templateId));

        Field retField;

        if (clause.equals(Clause.WHERE))
            fieldResolutionContext.setWithAlias(false);

        if (!templateId.equals(NIL_TEMPLATE))
            joinSetup.setUseEntry(true);


        if (columnAlias == null) {
            if (clause.equals(Clause.SELECT)) {
                if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("COMPOSITION"))
                    retField = new FullCompositionJson(fieldResolutionContext, joinSetup).sqlField();
                else if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("EHR"))
                    retField = new FullEhrJson(fieldResolutionContext, joinSetup).sqlField();
                else
                    throw new IllegalArgumentException("Canonical json is not supported at this stage for this Entity, found class:" + pathResolver.classNameOf(variableDefinition.getIdentifier()));
            }
            else
                retField = null;
        } else {
            if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("EHR")) {
                retField = new EhrResolver(fieldResolutionContext, joinSetup).sqlField(columnAlias);
            } else if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("COMPOSITION")||isCompositionAttributeItemStructure(templateId, variableDefinition.getIdentifier())) {
                if (columnAlias.startsWith("composer"))
                    retField = new ComposerResolver(fieldResolutionContext, joinSetup).sqlField(new AttributePath("composer").redux(columnAlias));
                else if (columnAlias.startsWith("context"))
                    retField = new EventContextResolver(fieldResolutionContext, joinSetup).sqlField(columnAlias);
                else //assume composition attribute
                    retField = new CompositionResolver(fieldResolutionContext, joinSetup).sqlField(columnAlias);
            } else
                throw new IllegalArgumentException("INTERNAL: the following class cannot be resolved for AQL querying:" + (pathResolver.classNameOf(variableDefinition.getIdentifier())));
        }

        jsonDataBlock = fieldResolutionContext.isJsonDatablock();
        return retField;
    }

    @Override
    public Field<?> whereField(String templateId,String identifier, I_VariableDefinition variableDefinition) {
        return makeField(templateId, identifier, variableDefinition, Clause.WHERE);
    }

    public boolean isJoinComposition() {
        return joinSetup.isJoinComposition();
    }

    public boolean isJoinEventContext() {
        return joinSetup.isJoinEventContext();
    }

    public boolean isJoinSubject() {
        return joinSetup.isJoinSubject();
    }

    public boolean isJoinEhr() {
        return joinSetup.isJoinEhr();
    }

    public boolean isJoinSystem() {
        return joinSetup.isJoinSystem();
    }

    public boolean isJoinEhrStatus() {
        return joinSetup.isJoinEhrStatus();
    }

    public boolean isJoinComposer() {
        return joinSetup.isJoinComposer();
    }

    public boolean isJoinContextFacility() {
        return joinSetup.isJoinContextFacility();
    }

    public boolean containsEhrStatus() {
        return joinSetup.isContainsEhrStatus();
    }


    @Override
    public boolean isContainsJqueryPath() {
        return false;
    }

    @Override
    public String getJsonbItemPath() {
        return null;
    }

    /**
     * true if the expression contains path and then use ENTRY as primary from table
     *
     * @return
     */
    public boolean useFromEntry() {
        return pathResolver.hasPathExpression();
    }

    public boolean isCompositionAttributeItemStructure(String templateId, String identifier){
        if (variableTemplatePath(templateId, identifier) == null)
            return false;
        return variableTemplatePath(templateId, identifier).contains("/context/other_context");
    }


    public void setUseEntry(boolean b) {
        joinSetup.setUseEntry(b);
    }

    public boolean isUseEntry(){
        return joinSetup.isUseEntry();
    }
}
