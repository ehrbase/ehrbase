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
public abstract class UC43 extends QueryProcessorTestBase {

    protected UC43() {
        this.aql = "select c[name/value = 'Laborbefund-1']/uid/value as uid1,\n"
                + "\t\t\t   c[name/value = 'Laborbefund-2']/uid/value as uid2\n"
                + "\t\t\t\tfrom EHR e \n"
                + "\t\t\t\tcontains COMPOSITION c[openEHR-EHR-COMPOSITION.report-result.v1]";
        this.expectedOutputWithJson = false;
    }
}
