/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.queryimpl.attribute;

import static org.junit.Assert.*;

import org.junit.Test;

public class GenericJsonPathTest {

    @Test
    public void jqueryPath() {

        assertEquals(
                "'{context,health_care_facility,external_ref,id}'",
                new GenericJsonPath("context/health_care_facility/external_ref/id").jqueryPath());
        assertEquals("'{setting,defining_code}'", new GenericJsonPath("setting/defining_code").jqueryPath());
        assertEquals(
                "'{context,other_context,/items[openEHR-EHR-CLUSTER.composition_context_detail.v1],$AQL_NODE_ITERATIVE$}'",
                new GenericJsonPath("context/other_context/items[openEHR-EHR-CLUSTER.composition_context_detail.v1]")
                        .jqueryPath());
        assertEquals(
                "'{context,other_context,/items[openEHR-EHR-CLUSTER.composition_context_detail.v1],$AQL_NODE_ITERATIVE$,/items[at0001],$AQL_NODE_ITERATIVE$}'",
                new GenericJsonPath(
                                "context/other_context/items[openEHR-EHR-CLUSTER.composition_context_detail.v1]/items[at0001]")
                        .jqueryPath());
        assertEquals(
                "'{context,other_context,/items[openEHR-EHR-CLUSTER.composition_context_detail.v1],$AQL_NODE_ITERATIVE$,/items[at0001],$AQL_NODE_ITERATIVE$,/value,value}'",
                new GenericJsonPath(
                                "context/other_context/items[openEHR-EHR-CLUSTER.composition_context_detail.v1]/items[at0001]/value/value")
                        .jqueryPath());
        assertEquals("'{context,name,0,value}'", new GenericJsonPath("context/name/value").jqueryPath());
    }

    @Test
    public void jqueryPathOtherDetails() {

        assertEquals(
                "'{other_details,/items[at0111],$AQL_NODE_ITERATIVE$,/value}'",
                new GenericJsonPath("other_details/items[at0111]/value").jqueryPath());
        assertEquals(
                "'{other_details,/items[at0111],$AQL_NODE_ITERATIVE$,/name,0}'",
                new GenericJsonPath("other_details/items[at0111]/name").jqueryPath());
        assertEquals(
                "'{other_details,/items[at0111],$AQL_NODE_ITERATIVE$,/value,value}'",
                new GenericJsonPath("other_details/items[at0111]/value/value").jqueryPath());
        assertEquals(
                "'{other_details,/items[at0111],$AQL_NODE_ITERATIVE$,/name,0,value}'",
                new GenericJsonPath("other_details/items[at0111]/name/value").jqueryPath());
    }
}
