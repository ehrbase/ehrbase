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

package org.ehrbase.aql.sql.queryImpl;

import org.ehrbase.aql.definition.FromEhrDefinition;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.binding.I_JoinBinder;
import org.ehrbase.aql.sql.queryImpl.attribute.*;
import org.ehrbase.aql.sql.queryImpl.attribute.composer.ComposerResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.composition.CompositionResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.composition.FullCompositionJson;
import org.ehrbase.aql.sql.queryImpl.attribute.ehr.EhrResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.eventcontext.EventContextResolver;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * map an AQL datavalue expression into a SQL field
 * <p>
 * Created by christian on 5/6/2016.
 */
public class CompositionAttributeQuery extends ObjectQuery implements I_QueryImpl, I_JoinBinder {

    private String serverNodeId;

    protected JoinSetup joinSetup = new JoinSetup(); //used to pass join metadata to perform binding
    private final String entry_root;
    //    private MetaData metaData;
    private final IntrospectService introspectCache;


    public CompositionAttributeQuery(DSLContext context, PathResolver pathResolver, String serverNodeId, String entry_root, IntrospectService introspectCache) {
        super(context, pathResolver);
        this.serverNodeId = serverNodeId;
        this.entry_root = entry_root;
        this.introspectCache = introspectCache;
    }

    @Override
    public Field<?> makeField(String templateId, UUID compositionId, String identifier, I_VariableDefinition variableDefinition, Clause clause) {
        //resolve composition attributes and/or context
        String columnAlias = variableDefinition.getPath();
        jsonDataBlock = false;
        FieldResolutionContext fieldResolutionContext =
                new FieldResolutionContext(context,
                        serverNodeId,
                        compositionId,
                        identifier,
                        variableDefinition,
                        clause,
                        pathResolver,
                        introspectCache,
                        entry_root);

        Field retField;

        if (columnAlias == null) {
            if (clause.equals(Clause.SELECT)) {
                if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("COMPOSITION"))
                    retField = new FullCompositionJson(fieldResolutionContext, joinSetup).sqlField();
                else
                    throw new IllegalArgumentException("Only full composition canonical json is supported at this stage, found class:" + pathResolver.classNameOf(variableDefinition.getIdentifier()));
            }
            else
                retField = null;
        } else {
            if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("EHR")) {
                retField = new EhrResolver(fieldResolutionContext, joinSetup).sqlField(columnAlias);
            } else if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("COMPOSITION")) {
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
    public Field<?> whereField(String templateId, UUID compositionId, String identifier, I_VariableDefinition variableDefinition) {
        return makeField(templateId, compositionId, identifier, variableDefinition, Clause.WHERE);
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
}
