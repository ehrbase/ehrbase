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
package org.ehrbase.aql.sql.queryimpl.translator.testcase.pg10.pgsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC32;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("not yet supported")
public class UC32Test extends UC32 {

    public UC32Test() {
        super();
        this.expectedSqlExpression = "select " + QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION
                + "(ehr.js_ehr(ehr_join.id,'local')::jsonb #>'{folders}') #>>'{name,value}' as \"/folders/name/value\""
                + " from \"ehr\".\"ehr\" as \"ehr_join\""
                + " where ("
                + "   'case1' IN (SELECT regexp_split_to_array('case1,case2', ','))"
                + "        and "
                + "       \"ehr_join\".\"id\"='c2561bab-4d2b-4ffd-a893-4382e9048f8c')";
    }

    @Test
    public void testIt() {
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
