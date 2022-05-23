/*
 * Copyright (c) 2020-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.containment;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ehrbase.aql.parser.AqlLexer;
import org.ehrbase.aql.parser.AqlParser;

/**
 * handles contains expression to form matching propositions.
 *
 * @author Chrisitan Chevalley
 * @since 1.0
 */
public class ContainsProposition {

    private final ParserRuleContext parserRuleContext;
    private final IdentifierMapper identifierMapper;

    public ContainsProposition(ParserRuleContext parserRuleContext, IdentifierMapper identifierMapper) {
        this.parserRuleContext = parserRuleContext;
        this.identifierMapper = identifierMapper;
    }

    public boolean isSingleChain() {
        if (!(parserRuleContext instanceof AqlParser.ContainsExpressionContext)) {
            return false;
        } else {
            return ((AqlParser.ContainsExpressionContext) parserRuleContext).AND() == null
                    && ((AqlParser.ContainsExpressionContext) parserRuleContext).OR() == null
                    && ((AqlParser.ContainsExpressionContext) parserRuleContext).XOR() == null;
        }
    }

    /**
     * returns a list of tokens (objects) for a given contains proposition
     *
     * @param containPropositions
     * @return
     */
    public List<Object> develop(ContainPropositions containPropositions) {
        List<Object> objects = new ArrayList<>();
        for (ParseTree child : parserRuleContext.children) {
            if (child instanceof TerminalNode) {
                objects.add(new ContainOperator(child.getText()));
            } else {
                if (new ContainsExpressions(child).isExplicitContainsClause()) {
                    String expressionLabel = new ContainsExpressions(child).containedItemLabel(true);
                    objects.add(containPropositions.get(expressionLabel));
                } else {
                    String expressionLabel = child.getText();
                    objects.add(containPropositions.get(expressionLabel));
                }
            }
        }
        return objects;
    }

    private List<Containment> getChainedContainments() {
        // traverse the containment list upward
        List<Containment> containments = new ArrayList<>();

        ParserRuleContext parentContainment = parserRuleContext.getParent();

        while (!(parentContainment instanceof AqlParser.FromContext)) {
            parentContainment = parentContainment.getParent();

            if (parentContainment instanceof AqlParser.ContainsContext) {
                // it reads from right to left!
                // check for contains keyword
                AqlParser.ContainsContext containsContext = (AqlParser.ContainsContext) parentContainment;
                if (containsContext.getChildCount() > 1) {
                    if (!new CommonTokenCompare(containsContext.getChild(1)).isEquals(AqlLexer.CONTAINS)) {
                        throw new IllegalStateException("Invalid contains context:" + containsContext.getText());
                    }

                    if (containsContext.getChild(0) instanceof AqlParser.SimpleClassExprContext) {
                        AqlParser.SimpleClassExprContext simpleClassExprContext =
                                (AqlParser.SimpleClassExprContext) containsContext.getChild(0);
                        if (!simpleClassExprContext.IDENTIFIER().isEmpty()) { // form: 1
                            containments.add((Containment) identifierMapper.getContainer(
                                    new SimpleClassExpressionIdentifier(simpleClassExprContext).resolve()));
                        } else {
                            String identifier = ((AqlParser.ArchetypedClassExprContext)
                                                            simpleClassExprContext.getChild(0))
                                                    .IDENTIFIER(1)
                                            != null
                                    ? ((AqlParser.ArchetypedClassExprContext) simpleClassExprContext.getChild(0))
                                            .IDENTIFIER(1)
                                            .getText()
                                    : (simpleClassExprContext.getChild(0))
                                            .getText()
                                            .toUpperCase();
                            containments.add((Containment) identifierMapper.getContainer(identifier));
                        }
                    }
                }
            }
        }

        return containments;
    }

    /**
     * return a ContainmentSet instance from the identified list of chained containments (e.g.
     * CONTAINS...CONTAINS...)
     *
     * @param containment
     * @return
     */
    public ContainmentSet containmentSet(Containment containment) {
        ContainmentSet containmentSet = new ContainmentSet(0, null);
        containmentSet.add(containment);
        List<Containment> containmentList = getChainedContainments();
        containmentSet.addAll(containmentList);

        return containmentSet;
    }
}
