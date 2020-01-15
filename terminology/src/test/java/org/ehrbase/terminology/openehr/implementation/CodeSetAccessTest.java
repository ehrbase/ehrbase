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

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.ehrbase.terminology.openehr.CodeSetAccess;
import org.ehrbase.terminology.openehr.OpenEHRCodeSetIdentifiers;
import org.ehrbase.terminology.openehr.TerminologyInterface;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class CodeSetAccessTest {

    @Test
    public void testCodeSetAccess() throws Exception {
        TerminologyInterface simpleTerminologyInterface = new SimpleTerminologyInterface("en");

        CodeSetAccess codeSetAccess = simpleTerminologyInterface.codeSetForId(OpenEHRCodeSetIdentifiers.INTEGRITY_CHECK_ALGORITHMS);

        assertEquals("openehr_integrity_check_algorithms", codeSetAccess.id());

        assertEquals(2, codeSetAccess.allCodes().size());

        assertTrue(codeSetAccess.hasCode(new CodePhrase(new TerminologyId("openehr_integrity_check_algorithms"), "SHA-1")));

    }
}
