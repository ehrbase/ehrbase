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
package org.ehrbase.aql.compiler;

import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.aql.definition.FromEhrDefinition;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.binding.VariableDefinitions;
import org.ehrbase.aql.sql.queryimpl.attribute.ehr.EhrResolver;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;

@SuppressWarnings({"java:S3740"})
public class Statements {

    private ParseTree parseTree;
    private List<Object> whereClause;
    private List<I_VariableDefinition> variables;
    private TopAttributes topAttributes;
    private List<OrderAttribute> orderAttributes;
    private IdentifierMapper identifierMapper;

    private Integer limitAttribute;
    private Integer offsetAttribute;
    private ExternalTerminologyValidation tsAdapter;

    public Statements(ParseTree parseTree, IdentifierMapper identifierMapper, ExternalTerminologyValidation tsAdapter) {
        this.parseTree = parseTree;
        this.identifierMapper = identifierMapper;
        this.tsAdapter = tsAdapter;
    }

    public Statements process() {
        QueryCompilerPass2 queryCompilerPass2 = new QueryCompilerPass2();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(queryCompilerPass2, parseTree);
        variables = queryCompilerPass2.getVariables();

        whereClause = visitWhere();

        // from Contains
        if (identifierMapper.hasEhrContainer()) appendEhrPredicate(identifierMapper.getEhrContainer());

        topAttributes = queryCompilerPass2.getTopAttributes();
        orderAttributes = queryCompilerPass2.getOrderAttributes();
        limitAttribute = queryCompilerPass2.getLimitAttribute();
        offsetAttribute = queryCompilerPass2.getOffsetAttribute();
        return this;
    }

    private List visitWhere() {

        WhereVisitor whereVisitor = new WhereVisitor(tsAdapter);
        whereVisitor.visit(parseTree);
        return whereVisitor.getWhereExpression();
    }

    private void appendEhrPredicate(FromEhrDefinition.EhrPredicate ehrPredicate) {
        if (ehrPredicate == null) return;

        // append field, operator and value to the where clause
        if (!whereClause.isEmpty()) {
            whereClause.add("and");
        }
        whereClause.add(new VariableDefinition(ehrPredicate.getField(), null, ehrPredicate.getIdentifier(), false));
        whereClause.add(ehrPredicate.getOperator());
        whereClause.add(ehrPredicate.getValue());
    }

    public List getWhereClause() {
        return whereClause;
    }

    public VariableDefinitions getVariables() {
        boolean containsNonEhrVariable = variables.stream()
                .map(I_VariableDefinition::getIdentifier)
                .map(s -> identifierMapper.getContainer(s))
                .anyMatch(c -> c != null && !c.getClass().isAssignableFrom(FromEhrDefinition.EhrPredicate.class));
        boolean containsOnlyEhrAttributes = variables.stream()
                .filter(v -> identifierMapper.getContainer(v.getIdentifier()) != null
                        && identifierMapper
                                .getContainer(v.getIdentifier())
                                .getClass()
                                .isAssignableFrom(FromEhrDefinition.EhrPredicate.class))
                .allMatch(v -> EhrResolver.isEhrAttribute(v.getPath()));

        // Force distinct If only contains ehr variables
        if (!containsNonEhrVariable && containsOnlyEhrAttributes) {
            variables.forEach(v -> v.setDistinct(true));
        }
        return new VariableDefinitions(variables);
    }

    public TopAttributes getTopAttributes() {
        return topAttributes;
    }

    public List<OrderAttribute> getOrderAttributes() {
        return orderAttributes;
    }

    public Integer getLimitAttribute() {
        return limitAttribute;
    }

    public Integer getOffsetAttribute() {
        return offsetAttribute;
    }

    public void put(I_VariableDefinition variableDefinition) {
        variables.add(variableDefinition);
    }

    public String getParsedExpression() {
        return parseTree.getText();
    }
}
