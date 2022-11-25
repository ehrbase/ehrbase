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

/**
 * test predicates in composition attributes
 */
public abstract class UC42 extends QueryProcessorTestBase {

    protected UC42() {
        this.aql = "SELECT\n" + "  \t  c[name/value = 'Diagnose']/uid/value as Diagnose,\n"
                + "  \t  c[composer/external_ref/id/value = 'Dr Mabuse']/uid/value as MabuseComposition,\n"
                + "  \t  c[context/start_time/value > '2020-01-01']/uid/value as NewerComposition\n"
                + "\tFROM\n"
                + "  \t  EHR e\n"
                + "  \t  contains COMPOSITION c[openEHR-EHR-COMPOSITION.report-result.v1]";
        this.expectedOutputWithJson = false;
    }
}
