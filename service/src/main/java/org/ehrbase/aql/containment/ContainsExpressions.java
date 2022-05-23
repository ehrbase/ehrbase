/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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

import org.ehrbase.aql.parser.AqlLexer;
import org.ehrbase.aql.parser.AqlParser;

/**
 * utilities to handle CONTAINS expressions
 */
public class ContainsExpressions {

    private final String UNDEFINED_ = "*undef*";

    AqlParser.ContainExpressionBoolContext containExpressionBoolContext;

    public ContainsExpressions(AqlParser.ContainExpressionBoolContext containExpressionBoolContext) {
        this.containExpressionBoolContext = containExpressionBoolContext;
    }

    public ContainsExpressions(AqlParser.ContainsExpressionContext containsExpressionContext) {
        this.containExpressionBoolContext =
                (AqlParser.ContainExpressionBoolContext) containsExpressionContext.getChild(0);
    }

    public ContainsExpressions(Object containExpression) {
        if (containExpression instanceof AqlParser.ContainExpressionBoolContext)
            this.containExpressionBoolContext = (AqlParser.ContainExpressionBoolContext) containExpression;
        else if (containExpression instanceof AqlParser.ContainsExpressionContext)
            this.containExpressionBoolContext = (AqlParser.ContainExpressionBoolContext)
                    ((AqlParser.ContainsExpressionContext) containExpression).getChild(0);
        else throw new IllegalArgumentException("Invalid/unhandled ParseTree:" + containExpression.getClass());
    }

    public boolean isExplicitContainsClause() {
        if (containExpressionBoolContext.getChild(0) instanceof AqlParser.ContainsContext)
            if (containExpressionBoolContext.getChild(0).getChildCount() > 1)
                return new CommonTokenCompare(
                                containExpressionBoolContext.getChild(0).getChild(1))
                        .isEquals(AqlLexer.CONTAINS);

        return false;
    }

    private String childLabel(AqlParser.ArchetypedClassExprContext archetypedClassExprContext, boolean fullLabel) {
        if (fullLabel) return archetypedClassExprContext.getText();
        else return archetypedClassExprContext.getChild(1).getText();
    }

    private String childLabel(AqlParser.SimpleClassExprContext simpleClassExprContext, boolean fullLabel) {
        if (simpleClassExprContext.getChild(0) instanceof AqlParser.ArchetypedClassExprContext)
            return childLabel((AqlParser.ArchetypedClassExprContext) simpleClassExprContext.getChild(0), fullLabel);
        return UNDEFINED_;
    }

    private String childLabel(AqlParser.ContainsExpressionContext containsExpressionContext, boolean fullLabel) {
        if (containsExpressionContext.getChild(0) instanceof AqlParser.ContainsContext)
            return childLabel((AqlParser.ContainsContext) containsExpressionContext.getChild(0), fullLabel);
        else if (containsExpressionContext.getChild(0) instanceof AqlParser.ContainExpressionBoolContext)
            return childLabel(
                    (AqlParser.ContainExpressionBoolContext) containsExpressionContext.getChild(0), fullLabel);
        return UNDEFINED_;
    }

    private String childLabel(AqlParser.ContainsContext containsContext, boolean fullLabel) {
        if (containsContext.getChildCount() == 3) // form: [...] contains [...], we take the right most expression
        return childLabel((AqlParser.ContainsExpressionContext) containsContext.getChild(2), fullLabel);
        else if (containsContext.getChildCount() == 1)
            return childLabel((AqlParser.SimpleClassExprContext) containsContext.getChild(0), fullLabel);
        return UNDEFINED_;
    }

    private String childLabel(AqlParser.ContainExpressionBoolContext containExpressionBoolContext, boolean fullLabel) {
        if (containExpressionBoolContext.getChild(0) instanceof AqlParser.ContainsContext) {
            return childLabel((AqlParser.ContainsContext) containExpressionBoolContext.getChild(0), fullLabel);
        }
        return UNDEFINED_;
    }

    public String containedItemLabel(boolean fullLabel) {
        return childLabel(containExpressionBoolContext, fullLabel);
    }
}
