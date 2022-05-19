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

import java.util.List;

/**
 * A complex contains expression is a logical expressions using simple contain chains.
 * F.e. CONTAINS ( CLUSTER k[..] AND CLUSTER z[...]).
 */
public class ComplexContainsCheck extends ContainsCheck {

    private List<Object> tokens;

    public ComplexContainsCheck(String label, List<Object> tokens) {
        super(label);
        this.tokens = tokens;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Object item : tokens) {
            if (item instanceof SimpleChainedCheck) stringBuilder.append(item.toString());
            else if (item instanceof ContainOperator) stringBuilder.append(((ContainOperator) item).getOperator());
            else if (item instanceof ComplexContainsCheck) stringBuilder.append(item.toString());
            else stringBuilder.append(item.toString());
        }
        this.checkExpression = stringBuilder.toString();
        return checkExpression;
    }

    @Override
    public String getSymbol() {
        return null;
    }

    public List<Object> getTokens() {
        return tokens;
    }
}
