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

public abstract class UC17 extends QueryProcessorTestBase {

    protected UC17() {
        this.aql = "select a from EHR e [ehr_id/value = '4a7c01cf-bb1c-4d3d-8385-4ae0674befb1']"
                + "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  "
                + "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]";
        this.expectedOutputWithJson = true;
    }
}
