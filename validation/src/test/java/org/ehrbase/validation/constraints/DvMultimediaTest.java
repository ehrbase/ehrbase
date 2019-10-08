/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.validation.constraints;

import org.ehrbase.validation.constraints.wrappers.CArchetypeConstraint;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvURI;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public class DvMultimediaTest extends ConstraintTestBase {

    @Before
    public void setUp(){
        try {
            setUpContext("./src/test/resources/constraints/dvmultimedia.xml");
        } catch (Exception e){
            fail();
        }
    }

    @Test
    public void testConstraintValidation() {
        CodePhrase charset = new CodePhrase(new TerminologyId("IANA_character-sets"), "UTF-8");
        CodePhrase language = new CodePhrase(new TerminologyId("ISO_639-1"), "en");
        String alternateText = "alternative text";
        CodePhrase mediaType = new CodePhrase( new TerminologyId("IANA_media-types"), "text/plain");
        CodePhrase compressionAlgorithm = new CodePhrase( new TerminologyId("openehr_compression_algorithms"), "other");
        //byte[] integrityCheck = new byte[0];
        CodePhrase integrityCheckAlgorithm = new CodePhrase( new TerminologyId("openehr_integrity_check_algorithms"), "SHA-1");
        DvMultimedia thumbnail = null;
        DvURI uri = new DvURI("www.iana.org");

        DvMultimedia multimedia = new DvMultimedia();
        multimedia.setCharset(charset);
        multimedia.setLanguage(language);
        multimedia.setAlternateText(alternateText);
        multimedia.setMediaType(mediaType);
        multimedia.setCompressionAlgorithm(compressionAlgorithm);
        multimedia.setIntegrityCheckAlgorithm(integrityCheckAlgorithm);
        multimedia.setThumbnail(thumbnail);
        multimedia.setUri(uri);

        try {
            new CArchetypeConstraint(null).validate("test", multimedia, archetypeconstraint);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
