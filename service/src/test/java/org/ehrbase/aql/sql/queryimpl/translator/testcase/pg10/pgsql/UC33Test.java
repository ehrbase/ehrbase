/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.queryimpl.translator.testcase.pg10.pgsql;

import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC33;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UC33Test extends UC33 {

    public UC33Test(){
        super();
        this.expectedSqlExpression =
                "select ARRAY.COLUMN as \"/folders/name/value\" from \"ehr\".\"ehr\" as \"ehr_join\"" +
                        " join lateral (\n" +
                        "  select jsonb_extract_path_text(cast(ehr.xjsonb_array_elements(cast(jsonb_extract_path(cast(\"ehr\".\"js_ehr\"(\n" +
                        "  cast(ehr_join.id as uuid), \n" +
                        "  'local'\n" +
                        ") as jsonb),'folders') as jsonb)) as jsonb),'name','0','value')\n" +
                        " AS COLUMN) as \"ARRAY\" on 1 = 1" +
                        " join lateral (\n" +
                        "  select jsonb_extract_path_text(cast(ehr.xjsonb_array_elements(cast(jsonb_extract_path(cast(\"ehr\".\"js_ehr\"(\n" +
                        "  cast(ehr_join.id as uuid), \n" +
                        "  'local'\n" +
                        ") as jsonb),'folders') as jsonb)) as jsonb),'name','0','value')\n" +
                        " AS COLUMN) as \"ARRAY\" on 1 = 1" +
                        " where (('case1'IN ( 'case1','case2' )  and \"ehr_join\".\"id\" = 'c2561bab-4d2b-4ffd-a893-4382e9048f8c') and (ARRAY.COLUMN is not null or ARRAY.COLUMN is not null))";
    }

    @Test
    public void testIt(){
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
