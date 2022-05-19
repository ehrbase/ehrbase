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
package org.ehrbase.aql.definition;

import org.ehrbase.aql.parser.AqlParser;

/**
 * Created by christian on 9/22/2017.
 */
public class IdentifiedPathVariable {

    private AqlParser.IdentifiedPathContext identifiedPathContext;
    private AqlParser.SelectExprContext selectExprContext;
    private final PredicateDefinition predicateDefinition;
    private boolean isDistinct;

    public IdentifiedPathVariable(
            AqlParser.IdentifiedPathContext identifiedPathContext,
            AqlParser.SelectExprContext selectExprContext,
            boolean isDistinct,
            PredicateDefinition predicateDefinition) {
        this.identifiedPathContext = identifiedPathContext;
        this.selectExprContext = selectExprContext;
        this.isDistinct = isDistinct;
        this.predicateDefinition = predicateDefinition;
    }

    public VariableDefinition definition() {
        String identifier = identifiedPathContext.IDENTIFIER().getText();
        String path = null;
        if (identifiedPathContext.objectPath() != null
                && !identifiedPathContext.objectPath().isEmpty())
            path = identifiedPathContext.objectPath().getText();
        String alias = null;
        // get an alias if any
        if (selectExprContext.AS() != null) {
            alias = selectExprContext.IDENTIFIER().getText();
        }

        VariableDefinition variableDefinition;
        if (predicateDefinition == null)
            variableDefinition = new VariableDefinition(path, alias, identifier, isDistinct);
        else variableDefinition = new VariableDefinition(path, alias, identifier, isDistinct, predicateDefinition);
        return variableDefinition;
    }
}
