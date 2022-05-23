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
package org.ehrbase.aql.compiler.recovery;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.RuleContext;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.parser.AqlParser;

public class RecoverArchetypeId {
    /**
     * archetype id is potentially recoverable if it is a single quoted string.
     * We check however, that the unquoted string is still a valid archetype id.
     *
     * @param context
     * @param offendingSymbol
     * @return
     */
    public boolean isRecoverableArchetypeId(RuleContext context, Object offendingSymbol) {
        if (context instanceof AqlParser.ArchetypedClassExprContext && offendingSymbol instanceof CommonToken) {
            CommonToken symbol = (CommonToken) offendingSymbol;
            if (symbol.getText().startsWith("'") && symbol.getText().endsWith("'"))
                return true; // ignore since it will be 'fixed' by the recognizer
        }
        return false;
    }

    public String recoverInvalidArchetypeId(RuleContext context, CommonToken offendingSymbol) {
        String offendingToken = offendingSymbol.getText();
        String archetypeId = StringUtils.stripEnd(StringUtils.stripStart(offendingToken, "'"), "'");
        String expression = context.getText().replace(offendingToken, archetypeId);

        // will throw an exception if not valid
        new AqlExpression().parse(expression, "archetypedClassExpr");

        return archetypeId;
    }
}
