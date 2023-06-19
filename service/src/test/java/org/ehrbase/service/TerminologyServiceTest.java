/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.ehrbase.openehr.sdk.terminology.openehr.TerminologyService;
import org.junit.Before;
import org.junit.Test;

public class TerminologyServiceTest {

    TerminologyService terminologyService;

    @Before
    public void setUp() throws Exception {
        terminologyService = new TerminologyServiceImp();
    }

    @Test
    public void terminology() {
        assertNotNull(terminologyService.terminology("openehr"));
        assertNotNull(terminologyService.terminology("openehr", "ja"));
    }

    @Test
    public void codeSet() {
        assertNotNull(terminologyService.codeSet("openehr_integrity_check_algorithms"));
        assertNotNull(terminologyService.codeSet("openehr_integrity_check_algorithms", "ja"));
    }

    @Test
    public void codeSetForId() {
        assertNotNull(terminologyService.codeSetForId("INTEGRITY_CHECK_ALGORITHMS"));
        assertNotNull(terminologyService.codeSetForId("INTEGRITY_CHECK_ALGORITHMS", "pt"));
    }

    @Test
    public void hasTerminology() {
        assertTrue(terminologyService.hasTerminology("openehr"));
        assertTrue(terminologyService.hasTerminology("openehr", "ja"));
    }

    @Test
    public void hasCodeSet() {
        assertTrue(terminologyService.hasCodeSet("integrity check algorithms"));
        assertTrue(terminologyService.hasCodeSet("integrity check algorithms", "pt"));
    }

    @Test
    public void terminologyIdentifiers() {
        assertTrue(terminologyService.terminologyIdentifiers().length > 0);
        assertTrue(terminologyService.terminologyIdentifiers("ja").length > 0);
    }

    @Test
    public void openehrCodeSets() {
        assertTrue(terminologyService.openehrCodeSets().size() > 0);
        assertTrue(terminologyService.openehrCodeSets("pt").size() > 0);
    }

    @Test
    public void codeSetIdentifiers() {
        assertTrue(terminologyService.codeSetIdentifiers().length > 0);
        assertTrue(terminologyService.codeSetIdentifiers("ja").length > 0);
    }
}
