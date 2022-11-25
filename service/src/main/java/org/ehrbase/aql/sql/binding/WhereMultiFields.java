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
package org.ehrbase.aql.sql.binding;

import java.util.ArrayList;
import java.util.List;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryimpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery;
import org.ehrbase.aql.sql.queryimpl.MultiFields;
import org.ehrbase.aql.sql.queryimpl.UnknownVariableException;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheService;

/**
 * Bind the abstract WHERE clause parameters into a SQL expression
 * Created by christian on 5/20/2016.
 */
// ignore cognitive complexity flag as this code as evolved historically and adjusted depending on use cases
@SuppressWarnings({"java:S3776", "java:S135"})
public class WhereMultiFields {

    private JsonbEntryQuery jsonbEntryQuery;
    private CompositionAttributeQuery compositionAttributeQuery;
    private final List<Object> whereClause;
    private PathResolver pathResolver;

    public WhereMultiFields(
            JsonbEntryQuery jsonbEntryQuery,
            CompositionAttributeQuery compositionAttributeQuery,
            List<Object> whereClause,
            PathResolver pathResolver) {
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.whereClause = whereClause;
        this.pathResolver = pathResolver;
    }

    public WhereMultiFields(
            I_DomainAccess domainAccess,
            IntrospectService introspectCache,
            Contains contains,
            List<Object> whereClause,
            String serverNodeId) {
        this.pathResolver = new PathResolver((KnowledgeCacheService) introspectCache, contains.getIdentifierMapper());
        this.jsonbEntryQuery = new JsonbEntryQuery(domainAccess, introspectCache, pathResolver);
        this.compositionAttributeQuery =
                new CompositionAttributeQuery(domainAccess, pathResolver, serverNodeId, introspectCache);
        this.whereClause = whereClause;
    }

    private void buildWhereCondition(List<MultiFields> multiFieldsList, String templateId, List<Object> item)
            throws UnknownVariableException {

        for (Object part : item) {
            if (part instanceof VariableDefinition) {
                // substitute the identifier
                multiFieldsList.add(
                        new ExpressionField((I_VariableDefinition) part, jsonbEntryQuery, compositionAttributeQuery)
                                .toSql(
                                        pathResolver.classNameOf(((I_VariableDefinition) part).getIdentifier()),
                                        templateId,
                                        ((I_VariableDefinition) part).getIdentifier(),
                                        IQueryImpl.Clause.WHERE));
            } else if (part instanceof List) {
                buildWhereCondition(multiFieldsList, templateId, (List) part);
            }
        }
    }

    public List<MultiFields> bind(String templateId) throws UnknownVariableException {

        List<MultiFields> multiFieldsList = new ArrayList<>();

        if (whereClause.isEmpty()) return multiFieldsList;

        // work on a copy since Exist is destructive
        List<Object> whereItems = new ArrayList<>(whereClause);
        buildWhereCondition(multiFieldsList, templateId, whereItems);

        return multiFieldsList;
    }

    public JoinSetup getJoinSetup() {
        return compositionAttributeQuery.getJoinSetup();
    }
}
