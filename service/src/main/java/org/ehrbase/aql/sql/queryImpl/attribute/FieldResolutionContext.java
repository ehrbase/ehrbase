/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
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
package org.ehrbase.aql.sql.queryImpl.attribute;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryImpl.I_QueryImpl;
import org.ehrbase.service.IntrospectService;
import org.jooq.DSLContext;

import java.util.UUID;

public class FieldResolutionContext {

    private final String columnAlias;
    private final UUID compositionId;
    private final String identifier;
    private final I_VariableDefinition variableDefinition;
    private final boolean withAlias;
    private final I_QueryImpl.Clause clause;
    private final DSLContext context;
    private final String serverNodeId;
    private final PathResolver pathResolver;
    private final IntrospectService introspectCache;
    private final String entry_root;
    private boolean jsonDatablock = false;
    private String rmType;

    public FieldResolutionContext(DSLContext context, String serverNodeId, UUID compositionId, String identifier, I_VariableDefinition variableDefinition, I_QueryImpl.Clause clause, PathResolver pathResolver, IntrospectService introspectCache, String entry_root) {
        this.compositionId = compositionId;
        this.identifier = identifier;
        this.variableDefinition = variableDefinition;
        this.withAlias = clause.equals(I_QueryImpl.Clause.SELECT) && variableDefinition.getPath() != null;
        this.clause = clause;
        this.context = context;
        this.serverNodeId = serverNodeId;
        this.pathResolver = pathResolver;
        this.entry_root = entry_root;
        this.introspectCache = introspectCache;
        columnAlias = variableDefinition.getPath();
    }

    public String getColumnAlias() {
        return columnAlias;
    }

    public UUID getCompositionId() {
        return compositionId;
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

    public I_QueryImpl.Clause getClause() {
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

    public String getEntry_root() {
        return entry_root;
    }

    public boolean isJsonDatablock() {
        return jsonDatablock;
    }

    public void setJsonDatablock(boolean jsonDatablock) {
        this.jsonDatablock = jsonDatablock;
    }
}
