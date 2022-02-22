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
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryimpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery;
import org.ehrbase.aql.sql.queryimpl.MultiFields;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheService;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind the abstract WHERE clause parameters into a SQL expression
 * Created by christian on 5/20/2016.
 */
//ignore cognitive complexity flag as this code as evolved historically and adjusted depending on use cases
@SuppressWarnings({"java:S3776","java:S135"})
public class WhereMultiFields {

    private JsonbEntryQuery jsonbEntryQuery;
    private CompositionAttributeQuery compositionAttributeQuery;
    private final List<Object> whereClause;
    private PathResolver pathResolver;

    public WhereMultiFields(JsonbEntryQuery jsonbEntryQuery, CompositionAttributeQuery compositionAttributeQuery, List<Object> whereClause, PathResolver pathResolver) {
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.whereClause = whereClause;
        this.pathResolver = pathResolver;
    }

    public WhereMultiFields(I_DomainAccess domainAccess, IntrospectService introspectCache, Contains contains, List<Object> whereClause, String serverNodeId) {
        this.pathResolver = new PathResolver((KnowledgeCacheService)introspectCache, contains.getIdentifierMapper());
        this.jsonbEntryQuery = new JsonbEntryQuery(domainAccess, introspectCache, pathResolver);
        this.compositionAttributeQuery = new CompositionAttributeQuery(domainAccess, pathResolver, serverNodeId, introspectCache);
        this.whereClause = whereClause;
    }

    private void buildWhereCondition(List<MultiFields> multiFieldsList, String templateId, List<Object> item) {

        for (Object part : item) {
            if (part instanceof VariableDefinition) {
                //substitute the identifier
                multiFieldsList.add(new ExpressionField((I_VariableDefinition) part, jsonbEntryQuery, compositionAttributeQuery).
                        toSql(pathResolver.classNameOf(((I_VariableDefinition) part).getIdentifier()),
                                templateId,
                                ((I_VariableDefinition) part).getIdentifier(),
                                IQueryImpl.Clause.WHERE));
            } else if (part instanceof List) {
                buildWhereCondition(multiFieldsList, templateId, (List) part);
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((compositionAttributeQuery == null) ? 0 : compositionAttributeQuery.hashCode());
        result = prime * result + ((jsonbEntryQuery == null) ? 0 : jsonbEntryQuery.hashCode());
        result = prime * result + ((pathResolver == null) ? 0 : pathResolver.hashCode());
        result = prime * result + ((whereClause == null) ? 0 : whereClause.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WhereMultiFields other = (WhereMultiFields) obj;
        if (compositionAttributeQuery == null) {
            if (other.compositionAttributeQuery != null)
                return false;
        } else if (!compositionAttributeQuery.equals(other.compositionAttributeQuery))
            return false;
        if (jsonbEntryQuery == null) {
            if (other.jsonbEntryQuery != null)
                return false;
        } else if (!jsonbEntryQuery.equals(other.jsonbEntryQuery))
            return false;
        if (pathResolver == null) {
            if (other.pathResolver != null)
                return false;
        } else if (!pathResolver.equals(other.pathResolver))
            return false;
        if (whereClause == null) {
            if (other.whereClause != null)
                return false;
        } else if (!whereClause.equals(other.whereClause))
            return false;
        return true;
    }

    public  List<MultiFields> bind(String templateId) {

        List<MultiFields> multiFieldsList = new ArrayList<>();

        if (whereClause.isEmpty())
            return multiFieldsList;

        //work on a copy since Exist is destructive
        List<Object> whereItems = new ArrayList<>(whereClause);
        buildWhereCondition(multiFieldsList, templateId, whereItems );

        return multiFieldsList;
    }

    public JoinSetup getJoinSetup(){
        return compositionAttributeQuery.getJoinSetup();
    }

}
