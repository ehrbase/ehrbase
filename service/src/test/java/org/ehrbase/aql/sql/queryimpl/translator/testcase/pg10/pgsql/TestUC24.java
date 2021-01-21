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

import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC24;

public class TestUC24 extends UC24 {

    public TestUC24(){
        super();
        this.expectedSqlExpression =
                "select cast(\"ehr\".\"js_composition\"(cast(cast(composition_join.id as uuid) as uuid), cast(? as text)) as varchar) as \"c\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" where (\"ehr\".\"entry\".\"template_id\" = ? and (\"ehr\".\"entry\".\"entry\" #>> '{/composition[openEHR-EHR-COMPOSITION.health_summary.v1],/content[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]}'IS NULL))";
    }
}
