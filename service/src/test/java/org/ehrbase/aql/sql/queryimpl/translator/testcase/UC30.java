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

public abstract class UC30 extends QueryProcessorTestBase {

    protected UC30() {
        this.aql = "Select e/folders/name/value\n" + "               from EHR e\n"
                + "               where e/ehr_id/value = 'c2561bab-4d2b-4ffd-a893-4382e9048f8c'\n"
                + "               and 'case1' = ALL(e/folders/name/value)";
        this.expectedOutputWithJson = false;
    }
}
