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

import static org.ehrbase.aql.sql.queryimpl.IQueryImpl.Clause.SELECT;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryimpl.*;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheService;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;

/**
 * Bind the abstract representation of a SELECT clause into a SQL expression
 * Created by christian on 5/4/2016.
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class SelectBinder extends TemplateMetaData implements ISelectBinder {

    public static final String DATA = "data";
    private final JsonbEntryQuery jsonbEntryQuery;
    private final CompositionAttributeQuery compositionAttributeQuery;
    private final PathResolver pathResolver;
    private final VariableDefinitions variableDefinitions;
    private final WhereBinder whereBinder;
    private final I_DomainAccess domainAccess;

    SelectBinder(
            I_DomainAccess domainAccess,
            IntrospectService introspectCache,
            PathResolver pathResolver,
            VariableDefinitions variableDefinitions,
            List whereClause,
            String serverNodeId) {
        super(introspectCache);
        this.domainAccess = domainAccess;
        this.pathResolver = pathResolver;
        this.variableDefinitions = variableDefinitions;
        this.jsonbEntryQuery = new JsonbEntryQuery(domainAccess, introspectCache, pathResolver);
        this.compositionAttributeQuery =
                new CompositionAttributeQuery(domainAccess, pathResolver, serverNodeId, introspectCache);
        this.whereBinder = new WhereBinder(whereClause, pathResolver);
    }

    private SelectBinder(
            I_DomainAccess domainAccess,
            IntrospectService introspectCache,
            IdentifierMapper mapper,
            VariableDefinitions variableDefinitions,
            List whereClause,
            String serverNodeId) {
        this(
                domainAccess,
                introspectCache,
                new PathResolver((KnowledgeCacheService) introspectCache, mapper),
                variableDefinitions,
                whereClause,
                serverNodeId);
    }

    public SelectBinder(
            I_DomainAccess domainAccess,
            IntrospectService introspectCache,
            Contains contains,
            Statements statements,
            String serverNodeId) {
        this(
                domainAccess,
                introspectCache,
                contains.getIdentifierMapper(),
                statements.getVariables(),
                statements.getWhereClause(),
                serverNodeId);
    }

    /**
     * bind with path resolution depending on composition
     *
     * @param templateId
     * @return
     */
    public List<MultiFields> bind(String templateId) throws UnknownVariableException {
        ObjectQuery.reset();

        List<MultiFields> multiFieldsList = new ArrayList<>();

        while (variableDefinitions.hasNext()) {
            I_VariableDefinition variableDefinition = variableDefinitions.next();
            MultiFields multiFields;
            if (variableDefinition.isFunction() || variableDefinition.isExtension()) {
                continue;
            } else if (variableDefinition.isConstant()) {
                multiFields =
                        new MultiFields(variableDefinition, new ConstantField(variableDefinition).toSql(), templateId);

            } else {
                String identifier = variableDefinition.getIdentifier();
                String className = pathResolver.classNameOf(identifier);

                ExpressionField expressionField =
                        new ExpressionField(variableDefinition, jsonbEntryQuery, compositionAttributeQuery);

                multiFields = expressionField.toSql(className, templateId, identifier, SELECT);

                if (multiFields == null
                        || multiFields.isEmpty()) { // the field cannot be resolved with containment (f.e. empty DB)
                    continue;
                }

                encodeForLateral(className, templateId, variableDefinition, multiFields, multiFieldsList);
            }
            multiFieldsList.add(multiFields);
            ObjectQuery.inc();
        }

        return multiFieldsList;
    }

    public Condition getWhereConditions(
            String templateId,
            int whereCursor,
            MultiFieldsMap multiWhereFieldsMap,
            int selectCursor,
            MultiFieldsMap multiSelectFieldsMap)
            throws UnknownVariableException {
        return whereBinder.bind(templateId, whereCursor, multiWhereFieldsMap, selectCursor, multiSelectFieldsMap);
    }

    public CompositionAttributeQuery getCompositionAttributeQuery() {
        return compositionAttributeQuery;
    }

    private void encodeForLateral(
            String className,
            String templateId,
            I_VariableDefinition variableDefinition,
            MultiFields multiFields,
            List<MultiFields> multiFieldsList)
            throws UnknownVariableException {
        for (Iterator<QualifiedAqlField> it = multiFields.iterator(); it.hasNext(); ) {
            QualifiedAqlField qualifiedAqlField = it.next();
            Field sqlField = qualifiedAqlField.getSQLField();
            SelectQuery selectQuery = domainAccess.getContext().selectQuery();
            selectQuery.addSelect(sqlField);
            if (SetReturningFunction.isUsed(selectQuery.toString())) { // what does selectQuery.toString() ?
                String alias = sqlField.getName();
                MultiFields unaliasedFields = new ExpressionField(
                                variableDefinition, jsonbEntryQuery, compositionAttributeQuery)
                        .toSql(className, templateId, variableDefinition.getIdentifier(), IQueryImpl.Clause.WHERE);

                TaggedStringBuilder taggedStringBuilder = new TaggedStringBuilder();
                taggedStringBuilder.append(
                        unaliasedFields.getLastQualifiedField().getSQLField().toString());

                // check if this expression is already used as a lateral join for another variable
                MultiFieldsMap.matchingLateralJoin(multiFieldsList, templateId, taggedStringBuilder.toString())
                        .ifPresentOrElse(
                                lj -> LateralJoins.reuse(lj, templateId, variableDefinition),
                                () -> LateralJoins.create(templateId, taggedStringBuilder, variableDefinition, SELECT));

                // substitute the field to use the lateral join with the same alias!
                variableDefinition.getLastLateralJoin(templateId).setClause(SELECT);
                String sqlToLateralJoin = variableDefinition
                                .getLastLateralJoin(templateId)
                                .getTable()
                                .getName() + "."
                        + variableDefinition.getLastLateralJoin(templateId).getLateralVariable();

                if (qualifiedAqlField.hasRightMostJsonbExpression())
                    sqlToLateralJoin = sqlToLateralJoin + qualifiedAqlField.getRightMostJsonbExpression();

                variableDefinition.setAlias(alias);
                Field substituteField = DSL.field(sqlToLateralJoin).as(alias);

                if (variableDefinition.getSelectType() != null
                        && !variableDefinition.getSelectType().getCastTypeName().equals("interval"))
                    substituteField = DSL.field("(" + sqlToLateralJoin + ")::"
                                    + variableDefinition.getSelectType().getCastTypeName())
                            .as(alias);
                else // default it to text so it can be interpreted
                substituteField = DSL.field("(" + sqlToLateralJoin + ")::TEXT").as(alias);
                multiFields.replaceField(qualifiedAqlField, substituteField);
            }
        }
    }
}
