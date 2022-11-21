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

import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC26;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("unsupported CONTAINS syntax")
public class UC26Test extends UC26 {

    public UC26Test() {
        super();
        this.expectedSqlExpression =
                "select count(DISTINCT \"_FCT_ARG_0\") as \"count\" from (select ehr.js_dv_date_time(\"ehr_join\".\"date_created\",\"ehr_join\".\"date_created_tzid\")::text as \"_FCT_ARG_0\""
                        + " from \"ehr\".\"entry\""
                        + " right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\""
                        + " right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\") as \"\"";
    }

    @Test
    public void testIt() {
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
