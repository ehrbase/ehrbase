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

import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC4;
import org.junit.Test;

public class UC4Test extends UC4 {

    public UC4Test() {
        super();
        this.expectedSqlExpression =
                "select \"\".\"/composer/name\", \"\".\"/context/start_time/value\" from (select \"composer_ref\".\"name\" as \"/composer/name\", jsonb_extract_path_text(cast(\"ehr\".\"js_dv_date_time\"(\n"
                        + "  \"ehr\".\"event_context\".\"start_time\",\n"
                        + "  event_context.START_TIME_TZID\n"
                        + ") as jsonb),'value') as \"/context/start_time/value\""
                        + " from \"ehr\".\"entry\""
                        + " right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\""
                        + " join \"ehr\".\"event_context\" on \"ehr\".\"event_context\".\"composition_id\" = \"ehr\".\"entry\".\"composition_id\""
                        + " join \"ehr\".\"party_identified\" as \"composer_ref\" on \"composition_join\".\"composer\" = \"composer_ref\".\"id\""
                        + " where \"ehr\".\"entry\".\"template_id\" = ?) as \"\" order by \"/context/start_time/value\" desc";
    }

    @Test
    public void testIt() {
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
