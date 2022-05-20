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

public abstract class UC39 extends QueryProcessorTestBase {

    protected UC39() {
        this.aql = "select" + "  max (d/description[at0001]/items[at0004]/value/magnitude) as max_magnitude"
                + " from EHR e"
                + "  contains COMPOSITION"
                + "  contains ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]";
        this.expectedOutputWithJson = false;
    }
}
