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
package org.ehrbase.aql.sql.queryimpl.translator.testcase;

import org.ehrbase.aql.sql.queryimpl.translator.QueryProcessorTestBase;

public abstract class UC16 extends QueryProcessorTestBase {

    protected UC16() {
        this.aql =
                "select a/description[at0001]/items[openEHR-EHR-CLUSTER.test_all_types.v1]/items[at0001]/items[at0002]/items[at0003]/value/value "
                        + "from EHR e "
                        + "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  "
                        + "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]"
                        + "WHERE a/description[at0001]/items[at0001]/items[at0002]/items[at0003]/value/value = true "
                        + "OR "
                        + "("
                        + "a/description[at0001]/items[at0001]/items[at0002]/items[at0003]/value/value = true "
                        + "AND"
                        + " a/description[at0001]/items[at0001]/items[at0002]/items[at0003]/value/value = true"
                        + ")";
        this.expectedOutputWithJson = true;
    }
}
