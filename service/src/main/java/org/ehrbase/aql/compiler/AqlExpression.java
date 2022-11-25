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

import java.lang.reflect.Method;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ehrbase.aql.parser.AqlLexer;
import org.ehrbase.aql.parser.AqlParser;

/**
 * Wrap the walkers for pass1 and pass2 as well as invoke the WHERE getQueryOptMetaData
 * <p>
 * The purpose of this class is to assemble all query parts from the AQL expression. The parts
 * are then passed to specific binders to translate and/or perform the query to
 * a backend.
 * Created by christian on 4/1/2016.
 * Refactored 13.8.2019
 */
public class AqlExpression {

    private AqlParser aqlParser;
    private ParseTree parseTree;

    public AqlExpression parse(String query) {
        ANTLRInputStream antlrInputStream = new ANTLRInputStream(query);
        Lexer aqlLexer = new AqlLexer(antlrInputStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(aqlLexer);
        this.aqlParser = new AqlParser(commonTokenStream);

        // define our own error listener (default one just display a message on System.err
        aqlLexer.removeErrorListeners();
        aqlLexer.addErrorListener(AqlErrorHandler.INSTANCE);
        aqlParser.removeErrorListeners();
        aqlParser.addErrorListener(AqlErrorHandler.INSTANCE);

        this.parseTree = aqlParser.query(); // begin parsing at query rule

        return this;
    }

    public AqlExpression parse(String expression, String ruleName) {
        ANTLRInputStream antlrInputStream = new ANTLRInputStream(expression);
        Lexer aqlLexer = new AqlLexer(antlrInputStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(aqlLexer);
        this.aqlParser = new AqlParser(commonTokenStream);

        // define our own error listener (default one just display a message on System.err
        aqlLexer.removeErrorListeners();
        aqlLexer.addErrorListener(AqlErrorHandlerNoRecovery.INSTANCE);
        aqlParser.removeErrorListeners();
        aqlParser.addErrorListener(AqlErrorHandlerNoRecovery.INSTANCE);

        try {
            Method ruleMethod = aqlParser.getClass().getMethod(ruleName);

            this.parseTree = (ParseTree) ruleMethod.invoke(aqlParser); // begin parsing at query rule
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getCause().getMessage());
        }

        return this;
    }

    public String dump() {
        if (parseTree != null && aqlParser != null) return parseTree.toStringTree(aqlParser);
        else return "**not initialized**";
    }

    public ParseTree getParseTree() {
        if (parseTree == null)
            throw new IllegalStateException(
                    "Parse tree is not initialized, use parse() method before calling this method");
        return parseTree;
    }
}
