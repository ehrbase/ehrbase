/*
 * Copyright (C) 2020 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase
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
package org.ehrbase.aql.containment;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ehrbase.aql.parser.AqlLexer;

/**
 * Compare if two ANTLR parsed tokens are equivalent.
 */
public class CommonTokenCompare {

    ParseTree token;

    public CommonTokenCompare(ParseTree token) {
        this.token = token;
    }

    public boolean equals(int value){
        return  (token instanceof TerminalNode
                && (token.getPayload()) instanceof CommonToken
                    && ((CommonToken)(token.getPayload())).getType() == value);
    }
}
