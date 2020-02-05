/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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

package org.ehrbase.validation.constraints.wrappers;

import com.nedap.archie.rm.datatypes.CodePhrase;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.CCODEPHRASE;

import java.util.Arrays;
import java.util.Map;

/**
 * validate a code phrase (composite object)
 *
 * @see CodePhrase
 *
 * Created by christian on 7/24/2016.
 */
public class CCodePhrase extends CConstraint implements I_CArchetypeConstraintValidate {

    CCodePhrase(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, ARCHETYPECONSTRAINT archetypeconstraint) throws IllegalArgumentException {

        if (!(aValue instanceof CodePhrase))
            throw new IllegalStateException("INTERNAL: argument is not a CodePhrase");

        CodePhrase codePhrase = (CodePhrase) aValue;
        CCODEPHRASE ccodephrase = (CCODEPHRASE) archetypeconstraint;

        //check terminology
        if (ccodephrase.isSetTerminologyId()) {

            String terminologyid = ccodephrase.getTerminologyId().getValue();

            if (terminologyid != null) { //terminology ID might not be specified as a constraint

                if (!codePhrase.getTerminologyId().getValue().equals(terminologyid))
                    ValidationException.raise(path, "CodePhrase terminology does not match, expected:" + terminologyid + ", found:" + codePhrase.getTerminologyId().getValue(), "CODE_PHRASE_02");

                if (terminologyid.equals("openehr") || terminologyid.equals("local")) {

                    if (ccodephrase.sizeOfCodeListArray() > 0) {
                        //should match one in the list
                        if (!(Arrays.asList(ccodephrase.getCodeListArray()).contains(codePhrase.getCodeString())))
                            ValidationException.raise(path, "CodePhrase codeString does not match any option, found:" + codePhrase.getCodeString(), "CODE_PHRASE_03");

                    }

                }
            }
        }
    }
}
