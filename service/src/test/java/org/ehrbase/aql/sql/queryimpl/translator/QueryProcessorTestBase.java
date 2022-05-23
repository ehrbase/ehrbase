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
package org.ehrbase.aql.sql.queryimpl.translator;

import static org.assertj.core.api.Assertions.assertThat;

import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.aql.sql.QueryProcessor;

public class QueryProcessorTestBase extends TestAqlBase {

    protected String aql;
    protected String expectedSqlExpression;
    protected boolean expectedOutputWithJson;

    public boolean testAqlSelectQuery() {
        AqlExpression aqlExpression = new AqlExpression().parse(aql);
        Contains contains = new Contains(new AqlExpression().parse(aql).getParseTree(), knowledge).process();
        Statements statements =
                new Statements(aqlExpression.getParseTree(), contains.getIdentifierMapper(), null).process();

        QueryProcessor cut = new QueryProcessor(testDomainAccess, knowledge, contains, statements, "local");

        QueryProcessor.AqlSelectQuery actual = cut.buildAqlSelectQuery();
        // check that generated sql is expected sql
        assertThat(removeLateralVarRef(removeLateralArrayRef(
                        removeAlias(actual.getSelectQuery().getSQL()))))
                .as(aql)
                .isEqualToIgnoringWhitespace(removeAlias(expectedSqlExpression));

        return true;
    }

    private String removeAlias(String s) {
        return s.replaceAll("alias_\\d+", "");
    }

    private String removeLateralArrayRef(String s) {
        return s.replaceAll("array_\\d+_\\d+", "ARRAY");
    }

    private String removeLateralVarRef(String s) {
        return s.replaceAll("var_\\d+_\\d+", "COLUMN");
    }

    //    @Test
    //    public void testDummyForSonar(){
    //        assertThat(1 == 1).isTrue();
    //    }
}
