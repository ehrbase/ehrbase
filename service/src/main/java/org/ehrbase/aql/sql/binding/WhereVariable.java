/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryimpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.JsonbEntryQuery;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.serialisation.dbencoding.CompositionSerializer;
import org.jooq.Field;

import static org.ehrbase.aql.sql.queryimpl.IterativeNodeConstants.ENV_AQL_USE_JSQUERY;

public class WhereVariable {

    public static final String COMPOSITION = "COMPOSITION";
    public static final String CONTENT = "content";
    public static final String EHR = "EHR";

    private final PathResolver pathResolver;
    private final I_DomainAccess domainAccess;
    private final JsonbEntryQuery jsonbEntryQuery;
    private final CompositionAttributeQuery compositionAttributeQuery;

    private boolean isFollowedBySQLConditionalOperator = false;

    public WhereVariable(PathResolver pathResolver, I_DomainAccess domainAccess, JsonbEntryQuery jsonbEntryQuery, CompositionAttributeQuery compositionAttributeQuery) {
        this.pathResolver = pathResolver;
        this.domainAccess = domainAccess;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.compositionAttributeQuery = compositionAttributeQuery;
    }

    public  TaggedStringBuilder encode(String templateId, I_VariableDefinition variableDefinition, boolean forceSQL, String compositionName) {
        String identifier = variableDefinition.getIdentifier();
        String className = pathResolver.classNameOf(identifier);
        if (className == null)
            throw new IllegalArgumentException("Could not bind identifier in WHERE clause:'" + identifier + "'");
        Field<?> field;
        //EHR-327: if force SQL is set to true via environment, jsquery extension is not required
        //this allows to deploy on AWS since jsquery is not supported by this provider
        Boolean usePgExtensions;
        if (System.getenv(ENV_AQL_USE_JSQUERY) != null)
            usePgExtensions = Boolean.parseBoolean(System.getenv(ENV_AQL_USE_JSQUERY));
        else if (domainAccess.getServerConfig().getUseJsQuery() != null)
            usePgExtensions = domainAccess.getServerConfig().getUseJsQuery();
        else
            usePgExtensions = false;

        if (forceSQL || Boolean.FALSE.equals(usePgExtensions)) {
            //EHR-327: also supports EHR attributes in WHERE clause
            ExpressionField expressionField = new ExpressionField(variableDefinition, jsonbEntryQuery, compositionAttributeQuery);
            field = expressionField.toSql(className, templateId, identifier, IQueryImpl.Clause.WHERE);

            if (field == null)
                return null;
            return new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.SQLQUERY);

        } else {
            switch (className) {
                case COMPOSITION:
                    if (variableDefinition.getPath().startsWith(CONTENT)) {
                        field = jsonbEntryQuery.whereField(templateId, identifier, variableDefinition);
                        TaggedStringBuilder taggedStringBuilder = new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.JSQUERY);
                        if (compositionName != null && taggedStringBuilder.startWith(CompositionSerializer.TAG_COMPOSITION)) {
                            //add the composition name into the composition predicate
                            taggedStringBuilder.replace("]", " and name/value='" + compositionName + "']");
                        }
                        return taggedStringBuilder;
                    }
                    break;
                case EHR:
                    field = compositionAttributeQuery.whereField(templateId, identifier, variableDefinition);
                    if (field == null)
                        return null;
                    isFollowedBySQLConditionalOperator = true;
                    return new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.SQLQUERY);

                default:
                    if (compositionAttributeQuery.isCompositionAttributeItemStructure(templateId, identifier)){
                        field = new ContextualAttribute(compositionAttributeQuery, jsonbEntryQuery, IQueryImpl.Clause.WHERE).toSql(templateId, variableDefinition);
                        return new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.SQLQUERY);
                    }
                    else {
                        field = jsonbEntryQuery.whereField(templateId, identifier, variableDefinition);
                        return new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.JSQUERY);
                    }
            }
            throw new IllegalStateException("Unhandled class name:"+className);
        }
    }

    public boolean isFollowedBySQLConditionalOperator() {
        return isFollowedBySQLConditionalOperator;
    }
}
