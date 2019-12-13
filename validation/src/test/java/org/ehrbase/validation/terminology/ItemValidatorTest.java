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
import org.junit.Test;

import static org.junit.Assert.*;

public class ItemValidatorTest {

    @Test
    public void matchValidator() throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException {

        ItemValidator itemValidator = new ItemValidator();

        itemValidator
                .add(new org.ehrbase.validation.terminology.validator.DvCodedText());

        DvCodedText dvCodedText = new DvCodedText("secondary allied health care", new CodePhrase(new TerminologyId("openehr"), "234"));

        assertTrue(itemValidator.isValidatedRmObjectType(dvCodedText));

        try {
            itemValidator.validate(new LocalizedTerminologies().locale("en"), AttributeCodesetMapping.getInstance(),"setting", dvCodedText, "en");
        } catch (Throwable throwable){
            fail();
        }

    }
}