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
import org.ehrbase.aql.sql.queryimpl.translator.testcase.UC16;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("doesn't match template, hence cast to boolean cannot be done using this path")
public class UC16Test extends UC16 {

    public UC16Test() {
        super();
        this.expectedSqlExpression = "select " + "("
                + QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION
                + "((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[openEHR-EHR-CLUSTER.test_all_types.v1],0,/items[at0001],0,/items[at0002],0,/items[at0003],0,/value,value}') as \"/description[at0001]/items[openEHR-EHR-CLUSTER.test_all_types.v1]/items[at0001]/items[at0002]/items[at0003]/value/value\""
                + " from \"ehr\".\"entry\""
                + " where (\"ehr\".\"entry\".\"template_id\" = ?"
                + " and (\"ehr\".\"entry\".\"entry\" #>> '{/composition[openEHR-EHR-COMPOSITION.health_summary.v1],/content[openEHR-EHR-ACTION.immunisation_procedure.v1],0,/description[at0001],/items[at0001],0,/items[at0002],0,/items[at0003],0,/value,value}'=true OR (\"ehr\".\"entry\".\"entry\" #>> '{/composition[openEHR-EHR-COMPOSITION.health_summary.v1],/content[openEHR-EHR-ACTION.immunisation_procedure.v1],0,/description[at0001],/items[at0001],0,/items[at0002],0,/items[at0003],0,/value,value}'=true AND \"ehr\".\"entry\".\"entry\" #>> '{/composition[openEHR-EHR-COMPOSITION.health_summary.v1],/content[openEHR-EHR-ACTION.immunisation_procedure.v1],0,/description[at0001],/items[at0001],0,/items[at0002],0,/items[at0003],0,/value,value}'=true)))";
    }

    @Test
    public void testIt() {
        assertThat(testAqlSelectQuery()).isTrue();
    }
}
