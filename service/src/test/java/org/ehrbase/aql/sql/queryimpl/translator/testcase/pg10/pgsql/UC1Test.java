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

import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC1;
import org.junit.Test;

public class UC1Test extends UC1 {

    public UC1Test() {
        super();
        this.expectedSqlExpression = "select distinct on (\"/ehr_id/value\") \"\".\"/ehr_id/value\" " + "from ("
                + "       select \"ehr_join\".\"id\" as \"/ehr_id/value\" from \"ehr\".\"ehr\" as \"ehr_join\""
                + ") as \"\"";
    }

    @Test
    public void doTest() {
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
