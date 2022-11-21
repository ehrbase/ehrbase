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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ehrbase.aql.compiler.recovery.RecoverArchetypeId;
import org.ehrbase.aql.containment.*;
import org.ehrbase.aql.definition.FromEhrDefinition;
import org.ehrbase.aql.definition.FromForeignDataDefinition;
import org.ehrbase.aql.definition.I_FromEntityDefinition;
import org.ehrbase.aql.parser.AqlBaseListener;
import org.ehrbase.aql.parser.AqlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AQL compilation pass 1<br>
 * This pass consists in evaluating the CONTAINS clauses and generate an internal representation of the
 * contain expressions and operators. The internal representation is then translated into a SQL equivalent
 * from the containment index.
 * Created by christian on 4/1/2016.
 */
public class QueryCompilerPass1 extends AqlBaseListener {

    // an equality expression consists of a left operand, an operator, a right operand
    // from grammar: predicateOperand COMPARABLEOPERATOR predicateOperand
    public static final int EQUALITY_ARGUMENTS_COUNT = 3;

    // from grammar: OPEN_BRACKET JOINON predicateEquality CLOSE_BRACKET
    public static final int JOIN_ARGUMENTS_COUNT = 4;

    private Logger logger = LoggerFactory.getLogger(QueryCompilerPass1.class);

    private IdentifierMapper identifierMapper = new IdentifierMapper(); // the map of identified contained nodes
    private Map<String, ContainmentSet> containmentSetMap = new HashMap<>(); // labelized contains sets
    private ContainPropositions containPropositions; // ordered contain evaluation map

    public QueryCompilerPass1() {
        containPropositions = new ContainPropositions(identifierMapper);
    }

    @Override
    public void exitFromEHR(AqlParser.FromEHRContext context) {
        FromEhrDefinition fromEhrDefinition = new FromEhrDefinition();
        if (context.IDENTIFIER() != null) {
            visitFromExpressionChildren(fromEhrDefinition, context.children);
            String identifier = context.IDENTIFIER().getText();
            fromEhrDefinition.setIdentifier(identifier);
            if (!fromEhrDefinition.getEhrPredicates().isEmpty()) {
                fromEhrDefinition.getEhrPredicates().get(0).setIdentifier(identifier);
                identifierMapper.add(fromEhrDefinition.getEhrPredicates().get(0));
            } else identifierMapper.add(new FromEhrDefinition.EhrPredicate(identifier));
        }
        if (context.EHR() != null) {
            fromEhrDefinition.setIsEHR(true);
        }

        logger.debug("FromEHR");
    }

    private void visitFromExpressionChildren(I_FromEntityDefinition fromEntityDefinition, List<ParseTree> children) {
        if (children.isEmpty()) return;

        for (ParseTree node : children) {

            if (node.getText().equals("[") || node.getText().equals("]")) continue;

            if (node instanceof AqlParser.StandardPredicateContext) {
                AqlParser.StandardPredicateContext equalityContext = (AqlParser.StandardPredicateContext) node;
                if (equalityContext.getChildCount() == EQUALITY_ARGUMENTS_COUNT) {
                    AqlParser.PredicateExprContext predicateExprContext =
                            (AqlParser.PredicateExprContext) equalityContext.getChild(1);
                    AqlParser.PredicateAndContext predicateAndContext =
                            (AqlParser.PredicateAndContext) predicateExprContext.getChild(0);
                    AqlParser.PredicateEqualityContext predicateEqualityContext =
                            (AqlParser.PredicateEqualityContext) predicateAndContext.getChild(0);
                    if (predicateEqualityContext.getChildCount() != EQUALITY_ARGUMENTS_COUNT)
                        throw new IllegalArgumentException(
                                "Could not handle predicateEqualityContext:" + predicateAndContext.getText());
                    fromEntityDefinition.add(
                            predicateEqualityContext.getChild(0).getText(),
                            predicateEqualityContext.getChild(2).getText(),
                            predicateEqualityContext.getChild(1).getText());
                }
            }
        }
    }

    private void visitJoinExpressionChildren(I_FromEntityDefinition fromEntityDefinition, List<ParseTree> children) {
        if (children.isEmpty()) return;

        for (ParseTree node : children) {

            if (node.getText().equals("[") || node.getText().equals("]")) continue;

            if (node instanceof AqlParser.JoinPredicateContext) {
                AqlParser.JoinPredicateContext joinContext = (AqlParser.JoinPredicateContext) node;
                if (joinContext.getChildCount() == JOIN_ARGUMENTS_COUNT) {
                    AqlParser.PredicateEqualityContext predicateEqualityContext =
                            (AqlParser.PredicateEqualityContext) joinContext.getChild(2);
                    if (predicateEqualityContext.getChildCount() != EQUALITY_ARGUMENTS_COUNT)
                        throw new IllegalArgumentException(
                                "Could not handle predicateEqualityContext:" + predicateEqualityContext.getText());
                    fromEntityDefinition.add(
                            predicateEqualityContext.getChild(0).getText(),
                            predicateEqualityContext.getChild(2).getText(),
                            predicateEqualityContext.getChild(1).getText());
                }
            }
        }
    }

    @Override
    public void exitFromForeignData(AqlParser.FromForeignDataContext context) {
        FromForeignDataDefinition fromForeignDataDefinition =
                new FromForeignDataDefinition(context.getChild(0).getText());
        if (context.IDENTIFIER() != null) {
            visitJoinExpressionChildren(fromForeignDataDefinition, context.children);
            String identifier = context.IDENTIFIER().getText();
            fromForeignDataDefinition.setIdentifier(identifier);
            if (!fromForeignDataDefinition.getFDPredicates().isEmpty()) {
                fromForeignDataDefinition.getFDPredicates().get(0).setIdentifier(identifier);
                identifierMapper.add(fromForeignDataDefinition.getFDPredicates().get(0));
            } else identifierMapper.add(new FromForeignDataDefinition.NodePredicate(identifier));
        }

        logger.debug("exitFromForeignData");
    }

    @Override
    public void exitContainExpressionBool(AqlParser.ContainExpressionBoolContext containExpressionBoolContext) {

        // evaluate the containment expression
        if (containExpressionBoolContext.OPEN_PAR() != null && containExpressionBoolContext.CLOSE_PAR() != null) {
            List<Object> objects = new ArrayList<>();
            for (ParseTree token : containExpressionBoolContext.children) {
                if (token.getText().matches("\\(|\\)")) objects.add(token.getText());
                else if (token instanceof AqlParser.ContainsExpressionContext) {
                    AqlParser.ContainsExpressionContext containsExpressionContext =
                            (AqlParser.ContainsExpressionContext) token;
                    objects.add(containPropositions.get(containsExpressionContext.getText()));
                }
            }
            containPropositions.put(
                    containExpressionBoolContext.getText(),
                    new ComplexContainsCheck(containExpressionBoolContext.getText(), objects));
        } else if (!new ContainsExpressions(containExpressionBoolContext).isExplicitContainsClause()) {
            SimpleChainedCheck simpleChainedCheck = new SimpleChainedCheck(
                    new ContainsExpressions(containExpressionBoolContext).containedItemLabel(false),
                    containmentSetMap.get(containExpressionBoolContext.getText()));
            containPropositions.put(containExpressionBoolContext.getText(), simpleChainedCheck);
        }
    }

    @Override
    public void exitContainsExpression(AqlParser.ContainsExpressionContext containsExpressionContext) {
        ContainsProposition proposition = new ContainsProposition(containsExpressionContext, identifierMapper);
        // check if expression is boolean or a single contains chain
        if (!proposition.isSingleChain()) {

            List<Object> developedExpression = proposition.develop(containPropositions);

            if (developedExpression.isEmpty())
                throw new IllegalStateException("Could not develop:" + containsExpressionContext.getText());

            containPropositions.put(
                    containsExpressionContext.getText(),
                    new ComplexContainsCheck(containsExpressionContext.getText(), developedExpression));
        }
    }

    @Override
    public void exitSimpleClassExpr(AqlParser.SimpleClassExprContext simpleClassExprContext) {
        logger.debug("from exitSimpleClassExpr: ENTER");
        if (!simpleClassExprContext.IDENTIFIER().isEmpty()) {
            // CHC, 160808: make classname case insensitive
            String className =
                    simpleClassExprContext.IDENTIFIER(0).getSymbol().getText().toUpperCase();
            String symbol = new SimpleClassExpressionIdentifier(simpleClassExprContext).resolve();
            Containment containment = new Containment(className, symbol, "");
            identifierMapper.add(containment);
            containmentSetMap.put(
                    simpleClassExprContext.getText(),
                    new ContainsProposition(simpleClassExprContext, identifierMapper).containmentSet(containment));

        } else if (simpleClassExprContext.getChild(0) instanceof AqlParser.ArchetypedClassExprContext) {
            // CHC, 160808: make classname case insensitive
            AqlParser.ArchetypedClassExprContext archetypedClassExprContext =
                    (AqlParser.ArchetypedClassExprContext) simpleClassExprContext.getChild(0);
            String className = archetypedClassExprContext
                    .IDENTIFIER(0)
                    .getSymbol()
                    .getText()
                    .toUpperCase();

            String symbol = archetypedClassExprContext.IDENTIFIER(1) != null
                    ? archetypedClassExprContext.IDENTIFIER(1).getSymbol().getText()
                    : archetypedClassExprContext.getText().toUpperCase();

            String archetypeId;
            if (archetypedClassExprContext.ARCHETYPEID() == null
                    && archetypedClassExprContext.exception instanceof InputMismatchException) {
                // check out if this is caused by a quoted archetypeId
                InputMismatchException inputMismatchException =
                        (InputMismatchException) archetypedClassExprContext.exception;
                if (inputMismatchException.getOffendingToken() instanceof CommonToken) {
                    archetypeId = new RecoverArchetypeId()
                            .recoverInvalidArchetypeId(archetypedClassExprContext, (CommonToken)
                                    inputMismatchException.getOffendingToken());
                } else throw new IllegalArgumentException("AQL Parse exception: " + simpleClassExprContext.getText());
            } else {
                archetypeId = archetypedClassExprContext.ARCHETYPEID().getText();
            }
            Containment containment = new Containment(className, symbol, archetypeId);

            identifierMapper.add(containment);

            containmentSetMap.put(
                    archetypedClassExprContext.getText(),
                    new ContainsProposition(archetypedClassExprContext, identifierMapper).containmentSet(containment));
        }
    }
    /**
     * returns the mapper of resolved identifiers in contains (including resolved paths
     * @return
     */
    public IdentifierMapper getIdentifierMapper() {
        return identifierMapper;
    }

    public ContainPropositions containPropositions() {
        return containPropositions;
    }
}
