/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.terminology.openehr.implementation;

import org.ehrbase.terminology.openehr.CodeSetAccess;
import org.ehrbase.terminology.openehr.OpenEHRCodeSetIdentifiers;
import org.ehrbase.terminology.openehr.TerminologyAccess;
import org.ehrbase.terminology.openehr.TerminologyInterface;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SimpleTerminologyInterfaceTest {


    @Test
    public void testSimpleTerminologyService() throws Exception {
        TerminologyInterface simpleTerminologyInterface =  new SimpleTerminologyInterface("en");
        assertNotNull(simpleTerminologyInterface);

        //test interfaces with the defined terminologies
        TerminologyAccess terminologyAccess = simpleTerminologyInterface.terminology("openehr");
        assertNotNull(terminologyAccess);

        //external id
        CodeSetAccess codeSetAccess = simpleTerminologyInterface.codeSet("openehr_normal_statuses");
        assertNotNull(codeSetAccess);

        //internal (openehr) id
        codeSetAccess = simpleTerminologyInterface.codeSetForId(OpenEHRCodeSetIdentifiers.NORMAL_STATUSES);
        assertNotNull(codeSetAccess);

        assertTrue(simpleTerminologyInterface.hasTerminology("openehr"));

        assertTrue(simpleTerminologyInterface.hasCodeSet("normal statuses"));

        List<String> terminologies = simpleTerminologyInterface.codeSetIdentifiers();
        assertEquals(7, terminologies.size());

        Map<String, String> openehrCodeSets = simpleTerminologyInterface.openehrCodeSets();
        assertEquals(7, openehrCodeSets.size());

    }

}
