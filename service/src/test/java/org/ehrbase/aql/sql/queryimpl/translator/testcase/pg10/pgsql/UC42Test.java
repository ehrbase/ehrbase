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

import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC41;
import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC42;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UC42Test extends UC42 {

    public UC42Test(){
        super();
        this.expectedSqlExpression =
                "(select ARRAY.COLUMN as \"Diagnose\", ARRAY.COLUMN as \"MabuseComposition\", ARRAY.COLUMN as \"NewerComposition\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" join \"ehr\".\"event_context\" on \"ehr\".\"event_context\".\"composition_id\" = \"ehr\".\"entry\".\"composition_id\" join \"ehr\".\"party_identified\" as \"composer_ref\" on \"composition_join\".\"composer\" = \"composer_ref\".\"id\" left outer join lateral (select (select \"composition_join\".\"id\"||'::'||'local'||'::'||1 + COALESCE(\n" +
                        "(select count(*)\n" +
                        "from \"ehr\".\"composition_history\"\n" +
                        "where \"composition_join\".\"id\" = \"ehr\".\"composition_history\".\"id\"\n" +
                        "group by \"ehr\".\"composition_history\".\"id\"), 0) as \"Diagnose\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_dv_coded_text_inner\"(\"ehr\".\"entry\".\"name\") as jsonb),'value') as varchar) = cast('Diagnose' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? left outer join lateral (select (select \"composition_join\".\"id\"||'::'||'local'||'::'||1 + COALESCE(\n" +
                        "(select count(*)\n" +
                        "from \"ehr\".\"composition_history\"\n" +
                        "where \"composition_join\".\"id\" = \"ehr\".\"composition_history\".\"id\"\n" +
                        "group by \"ehr\".\"composition_history\".\"id\"), 0) as \"MabuseComposition\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_party_ref\"(\n" +
                        "  \"composer_ref\".\"party_ref_value\", \n" +
                        "  \"composer_ref\".\"party_ref_scheme\", \n" +
                        "  \"composer_ref\".\"party_ref_namespace\", \n" +
                        "  \"composer_ref\".\"party_ref_type\"\n" +
                        ") as jsonb),'id','value') as varchar) = cast('Dr Mabuse' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? left outer join lateral (select (select \"composition_join\".\"id\"||'::'||'local'||'::'||1 + COALESCE(\n" +
                        "(select count(*)\n" +
                        "from \"ehr\".\"composition_history\"\n" +
                        "where \"composition_join\".\"id\" = \"ehr\".\"composition_history\".\"id\"\n" +
                        "group by \"ehr\".\"composition_history\".\"id\"), 0) as \"NewerComposition\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_dv_date_time\"(\n" +
                        "  \"ehr\".\"event_context\".\"start_time\", \n" +
                        "  coalesce(\n" +
                        "    event_context.START_TIME_TZID, \n" +
                        "    'UTC'\n" +
                        "  )\n" +
                        ") as jsonb),'value') as varchar) > cast('2020-01-01' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? where (\"ehr\".\"entry\".\"template_id\" = ? and ARRAY.COLUMN is not null and ARRAY.COLUMN is not null and ARRAY.COLUMN is not null)) union (select ARRAY.COLUMN as \"Diagnose\", ARRAY.COLUMN as \"MabuseComposition\", ARRAY.COLUMN as \"NewerComposition\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" join \"ehr\".\"event_context\" on \"ehr\".\"event_context\".\"composition_id\" = \"ehr\".\"entry\".\"composition_id\" join \"ehr\".\"party_identified\" as \"composer_ref\" on \"composition_join\".\"composer\" = \"composer_ref\".\"id\" left outer join lateral (select (select \"composition_join\".\"id\"||'::'||'local'||'::'||1 + COALESCE(\n" +
                        "(select count(*)\n" +
                        "from \"ehr\".\"composition_history\"\n" +
                        "where \"composition_join\".\"id\" = \"ehr\".\"composition_history\".\"id\"\n" +
                        "group by \"ehr\".\"composition_history\".\"id\"), 0) as \"Diagnose\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_dv_coded_text_inner\"(\"ehr\".\"entry\".\"name\") as jsonb),'value') as varchar) = cast('Diagnose' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? left outer join lateral (select (select \"composition_join\".\"id\"||'::'||'local'||'::'||1 + COALESCE(\n" +
                        "(select count(*)\n" +
                        "from \"ehr\".\"composition_history\"\n" +
                        "where \"composition_join\".\"id\" = \"ehr\".\"composition_history\".\"id\"\n" +
                        "group by \"ehr\".\"composition_history\".\"id\"), 0) as \"MabuseComposition\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_party_ref\"(\n" +
                        "  \"composer_ref\".\"party_ref_value\", \n" +
                        "  \"composer_ref\".\"party_ref_scheme\", \n" +
                        "  \"composer_ref\".\"party_ref_namespace\", \n" +
                        "  \"composer_ref\".\"party_ref_type\"\n" +
                        ") as jsonb),'id','value') as varchar) = cast('Dr Mabuse' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? left outer join lateral (select (select \"composition_join\".\"id\"||'::'||'local'||'::'||1 + COALESCE(\n" +
                        "(select count(*)\n" +
                        "from \"ehr\".\"composition_history\"\n" +
                        "where \"composition_join\".\"id\" = \"ehr\".\"composition_history\".\"id\"\n" +
                        "group by \"ehr\".\"composition_history\".\"id\"), 0) as \"NewerComposition\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_dv_date_time\"(\n" +
                        "  \"ehr\".\"event_context\".\"start_time\", \n" +
                        "  coalesce(\n" +
                        "    event_context.START_TIME_TZID, \n" +
                        "    'UTC'\n" +
                        "  )\n" +
                        ") as jsonb),'value') as varchar) > cast('2020-01-01' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? where (\"ehr\".\"entry\".\"template_id\" = ? and ARRAY.COLUMN is not null and ARRAY.COLUMN is not null and ARRAY.COLUMN is not null))";
    }

    @Test
    public void testIt(){
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
