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
import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC17;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UC17Test extends UC17 {

    public UC17Test(){
        super();
        this.expectedSqlExpression =
                "select ARRAY.COLUMN as \"a\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" join lateral (\n" +
                        "  select (ehr.xjsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{}') \n" +
                        " AS COLUMN) as \"ARRAY\" on 1 = 1 where (\"ehr\".\"entry\".\"template_id\" = ? and (\"ehr_join\".\"id\" = '4a7c01cf-bb1c-4d3d-8385-4ae0674befb1') and ARRAY.COLUMN is not null)";
    }

    @Test
    public void testIt(){
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
