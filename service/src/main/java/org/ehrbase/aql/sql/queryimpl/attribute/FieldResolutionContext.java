/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql.queryimpl.attribute;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;

@SuppressWarnings("java:S107")
public class FieldResolutionContext {

    private final String columnAlias;
    private final String identifier;
    private final I_VariableDefinition variableDefinition;
    private boolean withAlias;
    private final IQueryImpl.Clause clause;
    private final DSLContext context;
    private final String serverNodeId;
    private final PathResolver pathResolver;
    private final IntrospectService introspectCache;
    private final String entryRoot;
    private boolean jsonDatablock = false;
    private boolean isUsingSetReturningFunction = false;
    private String rmType;

    public FieldResolutionContext(
            DSLContext context,
            String serverNodeId,
            String identifier,
            I_VariableDefinition variableDefinition,
            IQueryImpl.Clause clause,
            PathResolver pathResolver,
            IntrospectService introspectCache,
            String entryRoot) {
        this.identifier = identifier;
        this.variableDefinition = variableDefinition;
        this.withAlias = clause.equals(IQueryImpl.Clause.SELECT)
                && (variableDefinition.getPath() != null || variableDefinition.getAlias() != null);
        this.clause = clause;
        this.context = context;
        this.serverNodeId = serverNodeId;
        this.pathResolver = pathResolver;
        this.entryRoot = entryRoot;
        this.introspectCache = introspectCache;
        columnAlias = variableDefinition.getPath();
    }

    public String getColumnAlias() {
        return columnAlias;
    }

    public String getIdentifier() {
        return identifier;
    }

    public I_VariableDefinition getVariableDefinition() {
        return variableDefinition;
    }

    public String getRmType() {
        return rmType;
    }

    public void setRmType(String rmType) {
        this.rmType = rmType;
    }

    public boolean isWithAlias() {
        return withAlias;
    }

    public IQueryImpl.Clause getClause() {
        return clause;
    }

    public DSLContext getContext() {
        return context;
    }

    public String getServerNodeId() {
        return serverNodeId;
    }

    public PathResolver getPathResolver() {
        return pathResolver;
    }

    public IntrospectService getIntrospectCache() {
        return introspectCache;
    }

    public String getEntryRoot() {
        return entryRoot;
    }

    public boolean isJsonDatablock() {
        return jsonDatablock;
    }

    public void setJsonDatablock(boolean jsonDatablock) {
        this.jsonDatablock = jsonDatablock;
    }

    public void setWithAlias(boolean b) {
        withAlias = b;
    }

    public boolean isUsingSetReturningFunction() {
        return isUsingSetReturningFunction;
    }

    public void setUsingSetReturningFunction(boolean usingSetReturningFunction) {
        isUsingSetReturningFunction = usingSetReturningFunction;
    }
}
