/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Stefan Spiska (Vitasystems GmbH).

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

package org.ehrbase.aql.compiler;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.aql.definition.*;
import org.ehrbase.aql.parser.AqlBaseListener;
import org.ehrbase.aql.parser.AqlParser;

import java.util.*;

/**
 * AQL compilation pass 2<p>
 * This pass uses the results of pass 1 to:
 * <ul>
 * <li>resolve AQL paths from symbols, example: c/items[at0002]/items[at0001]/value/value/magnitude
 * <li>create the list of variables used in SELECT
 * <li>create the list of ORDER BY expression parts
 * <li>set the TOP clause if specified
 * </ul>
 * Created by christian on 4/1/2016.
 */
public class QueryCompilerPass2 extends AqlBaseListener {

    private String[] allowedFunctions = {"COUNT"};

    private Logger logger = LogManager.getLogger(QueryCompilerPass2.class);

    private Deque<I_VariableDefinition> variableStack = new ArrayDeque<>();
    private Deque<OrderAttribute> orderAttributes = null;
    private Integer limitAttribute = null;
    private Integer offsetAttribute = null;

    private TopAttributes topAttributes = null;

    @Override
    public void exitObjectPath(AqlParser.ObjectPathContext objectPathContext) {
        logger.debug("Object Path->");
    }


    @Override
    public void exitSelectExpr(AqlParser.SelectExprContext selectExprContext) {
        boolean isDistinct = false;

        AqlParser.IdentifiedPathContext identifiedPathContext = selectExprContext.identifiedPath();

        if (selectExprContext.DISTINCT() != null) {
            //has distinct expression
            isDistinct = true;
        }

        if (identifiedPathContext != null) {
            VariableDefinition variableDefinition = new IdentifiedPathVariable(identifiedPathContext, selectExprContext, isDistinct).definition();
            pushVariableDefinition(variableDefinition);
        } else if (selectExprContext.stdExpression() != null) {
            //function handling
            logger.debug("Found standard expression");
            //set alias if any (function AS alias
            if (selectExprContext.stdExpression().function() != null) {
                logger.debug("Found function");
                AqlParser.FunctionContext functionContext = selectExprContext.stdExpression().function();
                String name = functionContext.FUNCTION_IDENTIFIER().getText();

                if (!Arrays.asList(allowedFunctions).contains(name.toUpperCase()))
                    throw new IllegalArgumentException("Found not supported function:'"+name+"'");

                List<FuncParameter> parameters = new ArrayList<>();

                int serial = 0;

                for (ParseTree pathTree : functionContext.children) {
                    if (pathTree instanceof AqlParser.IdentifiedPathContext) {
                        AqlParser.IdentifiedPathContext pathContext = (AqlParser.IdentifiedPathContext) pathTree;
                        VariableDefinition variableDefinition = new IdentifiedPathVariable(pathContext, selectExprContext, false).definition();
                        //by default postgresql limits the size of column name to 63 bytes
                        if (variableDefinition.getAlias() == null || variableDefinition.getAlias().isEmpty() || variableDefinition.getAlias().length() > 63)
                            variableDefinition.setAlias("_FCT_ARG_"+serial++);
                        pushVariableDefinition(variableDefinition);
                        parameters.add(new FuncParameter(FuncParameterType.VARIABLE, variableDefinition.getAlias() == null ? variableDefinition.getPath() : variableDefinition.getAlias()));
                    } else if (pathTree instanceof AqlParser.OperandContext) {
                        parameters.add(new FuncParameter(FuncParameterType.OPERAND, pathTree.getText()));
                    } else if (pathTree instanceof TerminalNode) {
                        parameters.add(new FuncParameter(FuncParameterType.IDENTIFIER, pathTree.getText()));
                    }
                }

                String alias = selectExprContext.IDENTIFIER() == null ? name : selectExprContext.IDENTIFIER().getText();
                String path = functionContext.getText();
                FunctionDefinition definition = new FunctionDefinition(name, alias, path, parameters);
                pushVariableDefinition(definition);
            }
            if (selectExprContext.stdExpression().extension() != null) {
                logger.debug("Found extension");
                AqlParser.ExtensionContext extensionContext = selectExprContext.stdExpression().extension();
                String context = extensionContext.getChild(2).getText();
                String parsableExpression = extensionContext.getChild(4).getText();
                String alias = selectExprContext.IDENTIFIER() == null ? "_alias_" + Math.abs(new Random().nextLong()) : selectExprContext.IDENTIFIER().getText();
                ExtensionDefinition definition = new ExtensionDefinition(context, parsableExpression, alias);
                pushVariableDefinition(definition);
            }

        } else
            throw new IllegalArgumentException("Could not interpret select context");
    }

    private void pushVariableDefinition(I_VariableDefinition variableDefinition){
        isUnique(variableDefinition);
        variableStack.push(variableDefinition);
    }

    /**
     * check if a variable definition is unique (e.g. new aliase)
     * @param variableDefinition
     */
    private void isUnique(I_VariableDefinition variableDefinition) {

        //allow duplicate aliases whe used in function expressions
        if (variableDefinition.isFunction())
            return;

        String alias = variableDefinition.getAlias();

        for (I_VariableDefinition stackedVariableDefinition: variableStack){
            if (!StringUtils.isEmpty(stackedVariableDefinition.getAlias()) && stackedVariableDefinition.getAlias().equals(alias))
                throw new IllegalArgumentException("Duplicated alias detected:"+alias);
        }
    }

//    @Override
//    public void exitIdentifiedPath(AqlParser.IdentifiedPathContext identifiedPathContext){
//        logger.debug("IdentifiedPathSeq->");
////        AqlParser.IdentifiedPathContext identifiedPathContext = identifiedPathContext.identifiedPath();
//        String path = identifiedPathContext.objectPath().getText();
//        String identifier = identifiedPathContext.IDENTIFIER().getText();
//        String alias = null;
//        Object parent = identifiedPathContext.getParent(); //either selectExpr or identifiedOperand (no alias)
//        if (parent instanceof AqlParser.SelectExprContext) {
//            AqlParser.SelectExprContext selectExprContext = (AqlParser.SelectExprContext) identifiedPathContext.getParent();
//            if (selectExprContext.IDENTIFIER() != null)
//                alias = selectExprContext.IDENTIFIER().getText();
//
//            VariableDefinition variableDefinition = new VariableDefinition(path, alias, identifier);
//            variableStack.push(variableDefinition);
//        }
//    }



    @Override
    public void exitTopExpr(AqlParser.TopExprContext context) {
        Integer window = null;
        TopAttributes.TopDirection direction = null;
        if (context.TOP() != null) {
            window = new Integer(context.INTEGER().getText());
            if (context.BACKWARD() != null)
                direction = TopAttributes.TopDirection.BACKWARD;
            else if (context.FORWARD() != null)
                direction = TopAttributes.TopDirection.FORWARD;
        }
        topAttributes = new TopAttributes(window, direction);
    }


    @Override
    public void exitOrderBySeq(AqlParser.OrderBySeqContext context) {
        if (orderAttributes == null)
            orderAttributes = new ArrayDeque<>();

        for (ParseTree tree : context.children) {
            if (tree instanceof AqlParser.OrderByExprContext) {
                AqlParser.OrderByExprContext context1 = (AqlParser.OrderByExprContext) tree;
                AqlParser.IdentifiedPathContext identifiedPathContext = context1.identifiedPath();
                String path;
                if (identifiedPathContext.objectPath() != null)
                    path = identifiedPathContext.objectPath().getText();
                else
                    path = null;

                String identifier = identifiedPathContext.IDENTIFIER().getText();  //f.e. 'e' in 'e/time_created/value

                OrderAttribute orderAttribute;
                if (path == null)
                    orderAttribute = new OrderAttribute(new VariableDefinition(path, identifier, null, false));
                else
                    orderAttribute = new OrderAttribute(new VariableDefinition(path, null, identifier, false));

                if (context1.ASC() != null || context1.ASCENDING() != null)
                    orderAttribute.setDirection(OrderAttribute.OrderDirection.ASC);
                else if (context1.DESC() != null || context1.DESCENDING() != null)
                    orderAttribute.setDirection(OrderAttribute.OrderDirection.DESC);
                orderAttributes.push(orderAttribute);
            }
        }
    }

    @Override
    public void exitOffset(AqlParser.OffsetContext ctx) {
        offsetAttribute = new Integer(ctx.INTEGER().getText());
    }

    @Override
    public void exitLimit(AqlParser.LimitContext ctx) {
        limitAttribute = new Integer(ctx.INTEGER().getText());
    }

    @Override
    public void exitFunction(AqlParser.FunctionContext functionContext) {
        //get the function id and parameters
        logger.debug("in function");
    }

    public List<I_VariableDefinition> getVariables() {
        return new ArrayList<>(variableStack);
    }

    TopAttributes getTopAttributes() {
        return topAttributes;
    }

    List<OrderAttribute> getOrderAttributes() {
        if (orderAttributes == null)
            return null;
        return new ArrayList<>(orderAttributes);
    }


    Integer getLimitAttribute() {
        return limitAttribute;
    }

    Integer getOffsetAttribute() {
        return offsetAttribute;
    }
}
