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

import com.nedap.archie.rm.datavalues.DvCodedText;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.parser.AqlBaseVisitor;
import org.ehrbase.aql.parser.AqlParser;
import org.ehrbase.openehr.sdk.util.functional.Try;
import org.ehrbase.openehr.sdk.validation.ConstraintViolationException;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;

/**
 * Interpret an AQL WHERE clause and set the result into a list of WHERE parts
 * Created by christian on 5/18/2016.
 *
 * @param <T>
 */
public class WhereVisitor extends AqlBaseVisitor<List<Object>> {

    private static final String MATCHES = "MATCHES";
    public static final String IN = " IN ";
    private static final String OPEN_CURL = "{";
    private static final String OPEN_PAR = "(";
    private static final String CLOSING_CURL = "}";
    private static final String CLOSING_PAR = ")";
    private static final String COMMA = ",";

    private List<Object> whereExpression = new ArrayList<>();
    private ExternalTerminologyValidation tsAdapter;

    public WhereVisitor(ExternalTerminologyValidation tsAdapter) {
        this.tsAdapter = tsAdapter;
    }

    @Override
    public List<Object> visitWhere(AqlParser.WhereContext ctx) {
        visitIdentifiedExpr(ctx.identifiedExpr());
        return whereExpression;
    }

    @Override
    public List<Object> visitIdentifiedExpr(AqlParser.IdentifiedExprContext context) {
        //        List<Object> whereExpression = new ArrayList<>();
        for (ParseTree tree : context.children) {
            if (tree instanceof TerminalNodeImpl) {
                String what = tree.getText().trim();
                whereExpression.add(what);
            } else if (tree instanceof AqlParser.IdentifiedEqualityContext) {
                visitIdentifiedEquality((AqlParser.IdentifiedEqualityContext) tree);
            }
        }
        return whereExpression;
    }

    @Override
    public List<Object> visitMatchesOperand(AqlParser.MatchesOperandContext context) {
        for (ParseTree tree : context.children) {
            if (tree instanceof AqlParser.ValueListItemsContext) {
                whereExpression.addAll(visitValueListItems((AqlParser.ValueListItemsContext) tree));
            } else if (tree instanceof AqlParser.IdentifiedEqualityContext) {
                visitIdentifiedEquality((AqlParser.IdentifiedEqualityContext) tree);
            } else if (tree instanceof AqlParser.IdentifiedPathContext) {
                parsePathContext((AqlParser.IdentifiedPathContext) tree);
            }
        }
        return whereExpression;
    }

    private void parsePathContext(AqlParser.IdentifiedPathContext identifiedPathContext) {
        if (identifiedPathContext.objectPath() == null)
            throw new IllegalArgumentException(
                    "WHERE variable should be a path, found:'" + identifiedPathContext.getText() + "'");
        String path = identifiedPathContext.objectPath().getText();
        String identifier = identifiedPathContext.IDENTIFIER().getText();
        String alias = null;
        VariableDefinition variable = new VariableDefinition(path, alias, identifier, false);
        whereExpression.add(variable);
    }

    @Override
    public List<Object> visitValueListItems(AqlParser.ValueListItemsContext ctx) {
        List<Object> operand = new ArrayList<>();
        int counter = 0; // to detect the last occurrence
        for (ParseTree tree : ctx.children) {
            if (tree instanceof AqlParser.OperandContext) {
                AqlParser.OperandContext operandContext = (AqlParser.OperandContext) tree;
                if (operandContext.STRING() != null)
                    operand.add(operandContext.STRING().getText());
                else if (operandContext.BOOLEAN() != null)
                    operand.add(operandContext.BOOLEAN().getText());
                else if (operandContext.INTEGER() != null)
                    operand.add(operandContext.INTEGER().getText());
                else if (operandContext.DATE() != null)
                    operand.add(operandContext.DATE().getText());
                else if (operandContext.FLOAT() != null)
                    operand.add(operandContext.FLOAT().getText());
                else if (operandContext.invokeOperand() != null) {
                    for (Object obj : visitInvokeOperand(operandContext.invokeOperand())) {
                        operand.add(obj);
                        operand.add(",");
                    }
                    operand.remove(operand.size() - 1);
                } else if (operandContext.PARAMETER() != null) operand.add("** unsupported operand: PARAMETER **");
                else operand.add("** unsupported operand: " + operandContext.getText());
                if (++counter < ctx.children.size()) operand.add(",");
            } else if (tree instanceof AqlParser.ValueListItemsContext) {
                List<Object> token = visitValueListItems((AqlParser.ValueListItemsContext) tree);
                operand.addAll(token);
            }
        }
        return operand;
    }

    @Override
    public List<Object> visitInvokeOperand(AqlParser.InvokeOperandContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public List<Object> visitInvokeExpr(AqlParser.InvokeExprContext ctx) {
        List<Object> invokeExpr = new ArrayList<>();
        assert (ctx.TERMINOLOGY().getText().equals("TERMINOLOGY"));
        assert (ctx.OPEN_PAR().getText().equals("("));
        assert (ctx.CLOSE_PAR().getText().equals(")"));
        List<String> codesList = new ArrayList<>();

        final String operation = ctx.STRING(0).getText().replace("'", "");
        final String adapter = ctx.STRING(1).getText().replace("'", "");
        final String parameters = ctx.STRING(2).getText().replace("'", "");

        TerminologyParam tp = TerminologyParam.ofServiceApi(adapter);
        tp.setOperation(operation);
        tp.setParameter(parameters);

        if (StringUtils.equals(operation, "expand")) {
            try {
                tp.useValueSet();
                List<DvCodedText> expansion = tsAdapter.expand(tp);
                expansion.forEach((DvCodedText dvCode) ->
                        codesList.add("'" + dvCode.getDefiningCode().getCodeString() + "'"));
                invokeExpr.addAll(codesList);
            } catch (Exception e) {
                throw new IllegalArgumentException("Terminology server operation failed:'" + e.getMessage() + "'");
            }
        } else if (StringUtils.equals(operation, "validate")) {
            try {
                Try<Boolean, ConstraintViolationException> expansion = tsAdapter.validate(tp);

                if (expansion.isSuccess()) expansion.getAsSuccess().consume((b, e) -> invokeExpr.add(b));
                else expansion.getAsFailure().consume((b, e) -> invokeExpr.add(Boolean.FALSE));
            } catch (Exception e) {
                throw new IllegalArgumentException("Terminology server operation failed:'" + e.getMessage() + "'");
            }
        }

        return invokeExpr;
    }

    @Override
    public List<Object> visitIdentifiedEquality(AqlParser.IdentifiedEqualityContext context) {
        boolean isMatchExpr = false;
        for (ParseTree tree : context.children) {
            if (tree instanceof TerminalNodeImpl) {
                String token = ((TerminalNodeImpl) tree).getSymbol().getText();
                if (token.equalsIgnoreCase(MATCHES)) {
                    isMatchExpr = true;
                    whereExpression.add(IN);
                } else if (token.equals(OPEN_CURL) && isMatchExpr) whereExpression.add(OPEN_PAR);
                else if (token.equals(CLOSING_CURL) && isMatchExpr) {
                    // if the last element in expression is a comma, overwrite it with a closing parenthesis
                    if (whereExpression.get(whereExpression.size() - 1).equals(COMMA))
                        whereExpression.set(whereExpression.size() - 1, CLOSING_PAR);
                    else whereExpression.add(CLOSING_PAR);
                    isMatchExpr = false; // closure
                } else whereExpression.add(token);

            } else if (tree instanceof AqlParser.IdentifiedOperandContext) {
                AqlParser.IdentifiedOperandContext operandContext = (AqlParser.IdentifiedOperandContext) tree;
                // translate/substitute operand
                for (ParseTree child : operandContext.children) {
                    if (child instanceof AqlParser.OperandContext || child instanceof TerminalNodeImpl) {
                        whereExpression.add(child.getText());
                    } else if (child instanceof AqlParser.IdentifiedPathContext) {
                        AqlParser.IdentifiedPathContext identifiedPathContext = (AqlParser.IdentifiedPathContext) child;
                        parsePathContext(identifiedPathContext);
                    }
                }
            } else if (tree instanceof AqlParser.IdentifiedEqualityContext) {
                visitIdentifiedEquality((AqlParser.IdentifiedEqualityContext) tree);
            } else if (tree instanceof AqlParser.MatchesOperandContext) {
                visitMatchesOperand((AqlParser.MatchesOperandContext) tree);
            } else if (tree instanceof AqlParser.IdentifiedPathContext) {
                parsePathContext((AqlParser.IdentifiedPathContext) tree);
            } else if (tree instanceof AqlParser.IdentifiedExprContext) {
                visitIdentifiedExpr((AqlParser.IdentifiedExprContext) tree);
            }
        }

        return whereExpression;
    }

    List<Object> getWhereExpression() {
        WhereClauseUtil whereClauseUtil = new WhereClauseUtil(whereExpression);

        if (!whereClauseUtil.isBalancedBlocks())
            throw new IllegalArgumentException(
                    "Unbalanced block in WHERE clause missing:'" + whereClauseUtil.getUnbalanced() + "'");

        return whereExpression;
    }
}
