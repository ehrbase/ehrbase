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

import org.ehrbase.aql.parser.AqlParser;

public class SimpleClassExpressionIdentifier {

    AqlParser.SimpleClassExprContext simpleClassExprContext;

    public SimpleClassExpressionIdentifier(AqlParser.SimpleClassExprContext simpleClassExprContext) {
        this.simpleClassExprContext = simpleClassExprContext;
    }

    public String resolve() {
        String symbol;

        if (simpleClassExprContext.IDENTIFIER().isEmpty())
            throw new IllegalArgumentException("Void SimpleClassExpression:" + simpleClassExprContext.getText());
        else if (simpleClassExprContext.IDENTIFIER(1) != null)
            symbol = simpleClassExprContext.IDENTIFIER(1).getSymbol().getText();
        else
            symbol = new AnonymousSymbol()
                    .generate(simpleClassExprContext
                            .IDENTIFIER(0)
                            .getSymbol()
                            .getText()
                            .toUpperCase());

        return symbol;
    }
}
