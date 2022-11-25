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

import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC27;
import org.junit.Test;

public class UC27Test extends UC27 {

    public UC27Test() {
        super();
        this.expectedSqlExpression =
                "select (ARRAY.COLUMN)::TEXT as \"/folders/name/value\" from \"ehr\".\"ehr\" as \"ehr_join\" join lateral (\n"
                        + "  select jsonb_extract_path_text(cast(ehr.xjsonb_array_elements(cast(jsonb_extract_path(cast(\"ehr\".\"js_ehr\"(\n"
                        + "  cast(ehr_join.id as uuid),\n"
                        + "  'local'\n"
                        + ") as jsonb),'folders') as jsonb)) as jsonb),'name','0','value')\n"
                        + " AS COLUMN) as \"ARRAY\" on true where (\"ehr_join\".\"id\" = 'c2561bab-4d2b-4ffd-a893-4382e9048f8c' and 'case1'IN ( (ARRAY.COLUMN)::TEXT  ) )";
    }

    @Test
    public void testIt() {
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
