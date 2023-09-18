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

import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC42;
import org.junit.Test;

public class UC42Test extends UC42 {

    public UC42Test() {
        super();
        this.expectedSqlExpression =
                "select ARRAY.COLUMN as \"Diagnose\", ARRAY.COLUMN as \"MabuseComposition\", ARRAY.COLUMN as \"NewerComposition\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" join \"ehr\".\"event_context\" on \"ehr\".\"event_context\".\"composition_id\" = \"ehr\".\"entry\".\"composition_id\" join \"ehr\".\"party_identified\" as \"composer_ref\" on \"composition_join\".\"composer\" = \"composer_ref\".\"id\" left outer join lateral (select (select jsonb_extract_path_text(cast(\"ehr\".\"js_composition\"(cast(\"composition_join\".\"id\" as uuid), cast('local' as text)) as jsonb), 'uid', 'value') as \"Diagnose\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_dv_coded_text_inner\"(\"ehr\".\"entry\".\"name\") as jsonb), 'value') as varchar) = cast('Diagnose' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? left outer join lateral (select (select jsonb_extract_path_text(cast(\"ehr\".\"js_composition\"(cast(\"composition_join\".\"id\" as uuid), cast('local' as text)) as jsonb), 'uid', 'value') as \"MabuseComposition\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_party_ref\"(\"composer_ref\".\"party_ref_value\", \"composer_ref\".\"party_ref_scheme\", \"composer_ref\".\"party_ref_namespace\", \"composer_ref\".\"party_ref_type\") as jsonb), 'id', 'value') as varchar) = cast('Dr Mabuse' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? left outer join lateral (select (select jsonb_extract_path_text(cast(\"ehr\".\"js_composition\"(cast(\"composition_join\".\"id\" as uuid), cast('local' as text)) as jsonb), 'uid', 'value') as \"NewerComposition\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_dv_date_time\"(\"ehr\".\"event_context\".\"start_time\", event_context.START_TIME_TZID) as jsonb), 'value') as varchar) > cast('2020-01-01' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? where \"ehr\".\"entry\".\"template_id\" = ? union all select ARRAY.COLUMN as \"Diagnose\", ARRAY.COLUMN as \"MabuseComposition\", ARRAY.COLUMN as \"NewerComposition\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" join \"ehr\".\"event_context\" on \"ehr\".\"event_context\".\"composition_id\" = \"ehr\".\"entry\".\"composition_id\" join \"ehr\".\"party_identified\" as \"composer_ref\" on \"composition_join\".\"composer\" = \"composer_ref\".\"id\" left outer join lateral (select (select jsonb_extract_path_text(cast(\"ehr\".\"js_composition\"(cast(\"composition_join\".\"id\" as uuid), cast('local' as text)) as jsonb), 'uid', 'value') as \"Diagnose\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_dv_coded_text_inner\"(\"ehr\".\"entry\".\"name\") as jsonb), 'value') as varchar) = cast('Diagnose' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? left outer join lateral (select (select jsonb_extract_path_text(cast(\"ehr\".\"js_composition\"(cast(\"composition_join\".\"id\" as uuid), cast('local' as text)) as jsonb), 'uid', 'value') as \"MabuseComposition\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_party_ref\"(\"composer_ref\".\"party_ref_value\", \"composer_ref\".\"party_ref_scheme\", \"composer_ref\".\"party_ref_namespace\", \"composer_ref\".\"party_ref_type\") as jsonb), 'id', 'value') as varchar) = cast('Dr Mabuse' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? left outer join lateral (select (select jsonb_extract_path_text(cast(\"ehr\".\"js_composition\"(cast(\"composition_join\".\"id\" as uuid), cast('local' as text)) as jsonb), 'uid', 'value') as \"NewerComposition\" where cast(jsonb_extract_path_text(cast(\"ehr\".\"js_dv_date_time\"(\"ehr\".\"event_context\".\"start_time\", event_context.START_TIME_TZID) as jsonb), 'value') as varchar) > cast('2020-01-01' as varchar)) as \"COLUMN\") as \"ARRAY\" on ? where \"ehr\".\"entry\".\"template_id\" = ?";
    }

    @Test
    public void testIt() {
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
