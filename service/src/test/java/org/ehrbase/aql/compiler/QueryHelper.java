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

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ehrbase.aql.parser.AqlLexer;
import org.ehrbase.aql.parser.AqlParser;

public class QueryHelper {
    private QueryHelper() {}

    public static ParseTree setupParseTree(String aql) {
        AqlLexer aqlLexer = new AqlLexer(new ANTLRInputStream(aql));
        CommonTokenStream commonTokenStream = new CommonTokenStream(aqlLexer);
        AqlParser aqlParser = new AqlParser(commonTokenStream);

        // define our own error listener (default one just display a message on System.err
        aqlLexer.removeErrorListeners();
        aqlLexer.addErrorListener(AqlErrorHandler.INSTANCE);
        aqlParser.removeErrorListeners();
        aqlParser.addErrorListener(AqlErrorHandler.INSTANCE);

        return aqlParser.query();
    }
}
