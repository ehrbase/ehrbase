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
package org.ehrbase.aql.sql.queryimpl;

import static org.ehrbase.aql.sql.QueryProcessor.NIL_TEMPLATE;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.binding.IJoinBinder;
import org.ehrbase.aql.sql.binding.LateralJoins;
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
import org.jooq.Comparator;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;

/**
 * map an AQL datavalue expression into a SQL field
 * <p>
 * Created by christian on 5/6/2016.
 */
@SuppressWarnings({"java:S3776", "java:S3740"})
public class CompositionAttributeQuery extends ObjectQuery implements IQueryImpl, IJoinBinder {

    private String serverNodeId;

    protected JoinSetup joinSetup = new JoinSetup(); // used to pass join metadata to perform binding

    private final IntrospectService introspectCache;

    public CompositionAttributeQuery(
            I_DomainAccess domainAccess,
            PathResolver pathResolver,
            String serverNodeId,
            IntrospectService introspectCache) {
        super(domainAccess, pathResolver);
        this.serverNodeId = serverNodeId;
        this.introspectCache = introspectCache;
    }

    @Override
    public MultiFields makeField(
            String templateId, String identifier, I_VariableDefinition variableDefinition, Clause clause)
            throws UnknownVariableException {
        // resolve composition attributes and/or context
        String columnAlias = variableDefinition.getPath();
        FieldResolutionContext fieldResolutionContext = new FieldResolutionContext(
                domainAccess.getContext(),
                serverNodeId,
                identifier,
                variableDefinition,
                clause,
                pathResolver,
                introspectCache,
                pathResolver.entryRoot(templateId));

        Field retField;

        if (clause.equals(Clause.WHERE)) fieldResolutionContext.setWithAlias(false);

        if (!templateId.equals(NIL_TEMPLATE)) joinSetup.setUseEntry(true);

        if (columnAlias == null) {
            if (clause.equals(Clause.SELECT)) {
                if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("COMPOSITION"))
                    retField = new FullCompositionJson(fieldResolutionContext, joinSetup).sqlField();
                else if (pathResolver
                        .classNameOf(variableDefinition.getIdentifier())
                        .equals("EHR")) retField = new FullEhrJson(fieldResolutionContext, joinSetup).sqlField();
                else
                    throw new IllegalArgumentException(
                            "Canonical json is not supported at this stage for this Entity, found class:"
                                    + pathResolver.classNameOf(variableDefinition.getIdentifier()));
            } else retField = null;
        } else {
            if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("EHR")) {
                retField = new EhrResolver(fieldResolutionContext, joinSetup).sqlField(columnAlias);
            } else if (pathResolver
                            .classNameOf(variableDefinition.getIdentifier())
                            .equals("COMPOSITION")
                    || isCompositionAttributeItemStructure(templateId, variableDefinition.getIdentifier())) {
                if (columnAlias.startsWith("composer"))
                    retField = new ComposerResolver(fieldResolutionContext, joinSetup)
                            .sqlField(new AttributePath("composer").redux(columnAlias));
                else if (columnAlias.startsWith("context"))
                    retField = new EventContextResolver(fieldResolutionContext, joinSetup).sqlField(columnAlias);
                else // assume composition attribute
                retField = new CompositionResolver(fieldResolutionContext, joinSetup).sqlField(columnAlias);
            } else
                throw new IllegalArgumentException("INTERNAL: the following class cannot be resolved for AQL querying:"
                        + (pathResolver.classNameOf(variableDefinition.getIdentifier())));
        }

        // generate a lateral table for this select field
        if (variableDefinition.getPredicateDefinition() != null && retField != null) {
            deriveLateralJoinForPredicate(variableDefinition, retField);
            // edit the return field to point to the lateral join
            // the equivalent select field is the lateral join table '.' the pseudo variable used as ref in the join
            // f.e. select array_433911319_3.var_433911319_4 with a join as:
            //                    left outer join lateral (
            //                            ...
            //                             ) as "var_433911319_4"
            //                    ) as "array_433911319_3"
            //                            on true
            String sqlToLateralJoin = variableDefinition
                            .getLastLateralJoin(NIL_TEMPLATE)
                            .getTable()
                            .getName() + "." + variableDefinition.getSubstituteFieldVariable();
            retField = DSL.field(sqlToLateralJoin).as(retField.getName());
        } else if (fieldResolutionContext.isUsingSetReturningFunction()) retField = DSL.field(DSL.select(retField));

        QualifiedAqlField aqlField = new QualifiedAqlField(retField);

        return new MultiFields(variableDefinition, aqlField, templateId);
    }

    @Override
    public MultiFields whereField(String templateId, String identifier, I_VariableDefinition variableDefinition)
            throws UnknownVariableException {
        return makeField(templateId, identifier, variableDefinition, Clause.WHERE);
    }

    /**
     * true if the expression contains path and then use ENTRY as primary from table
     *
     * @return
     */
    public boolean useFromEntry() {
        return pathResolver.hasPathExpression();
    }

    public boolean isCompositionAttributeItemStructure(String templateId, String identifier) {
        if (variableTemplatePath(templateId, identifier) == null) return false;
        return variableTemplatePath(templateId, identifier).contains("/context/other_context");
    }

    public void setUseEntry(boolean b) {
        joinSetup.setUseEntry(b);
    }

    public boolean isUseEntry() {
        return joinSetup.isUseEntry();
    }

    public JoinSetup getJoinSetup() {
        return joinSetup;
    }

    private void deriveLateralJoinForPredicate(I_VariableDefinition variableDefinition, Field retField)
            throws UnknownVariableException {

        // encode a pseudo variable to get the predicate in the where clause
        VariableDefinition pseudoVar = new VariableDefinition(
                variableDefinition.getPredicateDefinition().getOperand1(),
                null,
                variableDefinition.getIdentifier(),
                false);
        CompositionAttributeQuery compositionAttributeQuery = new CompositionAttributeQuery(
                this.domainAccess, this.pathResolver, this.serverNodeId, this.introspectCache);
        MultiFields wherePredicate = compositionAttributeQuery.makeField(
                NIL_TEMPLATE, variableDefinition.getIdentifier(), pseudoVar, Clause.WHERE);

        joinSetup.merge(compositionAttributeQuery.getJoinSetup());

        SelectQuery selectQuery = domainAccess.getContext().selectQuery();

        // generate a lateral table for this select field
        selectQuery.addSelect(retField);
        Comparator comparator =
                comparatorFromSQL(variableDefinition.getPredicateDefinition().getOperator());

        selectQuery.addConditions(wherePredicate
                .getLastQualifiedField()
                .getSQLField()
                .cast(String.class)
                .compare(
                        comparator,
                        DSL.field(variableDefinition.getPredicateDefinition().getOperand2())
                                .cast(String.class)));

        LateralJoins.create(NIL_TEMPLATE, selectQuery, variableDefinition, Clause.SELECT);
    }

    private Comparator comparatorFromSQL(String sql) {
        for (Comparator comparator : Comparator.values()) {
            if (sql.equals(comparator.toSQL())) return comparator;
        }
        return null;
    }
}
