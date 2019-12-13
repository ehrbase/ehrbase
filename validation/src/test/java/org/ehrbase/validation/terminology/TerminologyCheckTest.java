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

package org.ehrbase.validation.terminology;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.ehrbase.terminology.openehr.implementation.AttributeCodesetMapping;
import org.ehrbase.terminology.openehr.implementation.LocalizedTerminologies;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public class TerminologyCheckTest {

    LocalizedTerminologies localizedTerminologies;
    AttributeCodesetMapping codesetMapping;

    @Before
    public void setup() throws Exception {
        localizedTerminologies = new LocalizedTerminologies();
        codesetMapping = AttributeCodesetMapping.getInstance();
    }


    @Test
    public void testSimpleValidation() throws Exception {
        DvCodedText category = new DvCodedText("event", new CodePhrase(new TerminologyId("openehr"), "433"));
        org.ehrbase.validation.terminology.validator.DvCodedText.validate(localizedTerminologies.locale("en"), codesetMapping, "category", category, "en");
    }

    @Test
    public void testSimpleValidationWrongCode() throws Exception {
        DvCodedText category = new DvCodedText("event", new CodePhrase(new TerminologyId("openehr"), "999"));
        try {
            org.ehrbase.validation.terminology.validator.DvCodedText.validate(localizedTerminologies.locale("en"), codesetMapping, "category", category, "en");
            fail("should have detected a bad code");
        }
        catch (Exception e){}
    }
    @Test
    public void testSimpleValidationWrongValue() throws Exception {
        DvCodedText category = new DvCodedText("not quite sure what to put here", new CodePhrase(new TerminologyId("openehr"), "433"));
        try {
            org.ehrbase.validation.terminology.validator.DvCodedText.validate(localizedTerminologies.locale("en"), codesetMapping, "category", category, "en");
            fail("should have detected a wrong value");
        }
        catch (Exception e){}
    }

    @Test
    public void testSimpleValidationLanguage() throws Exception {
        CodePhrase codePhrase = new CodePhrase(new TerminologyId("ISO_3166-1"), "AU");
        org.ehrbase.validation.terminology.validator.CodePhrase.validate(localizedTerminologies.getDefault(), codesetMapping, "territory", codePhrase);
        org.ehrbase.validation.terminology.validator.CodePhrase.validate(localizedTerminologies.getDefault(), codesetMapping, null, codePhrase);
    }

}
