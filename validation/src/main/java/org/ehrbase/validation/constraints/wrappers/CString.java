/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

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

import org.openehr.schemas.v1.CPRIMITIVE;

import java.util.Map;

/**
 * Validate a String
 *
 * @see com.nedap.archie.aom.primitives.CString
 * @link https://specifications.openehr.org/releases/AM/latest/AOM1.4.html#_c_string_class
 *
 * Created by christian on 7/22/2016.
 */
public class CString extends CConstraint implements I_CTypeValidate {
    protected CString(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, CPRIMITIVE cprimitive) {
        return;
        /* TICKET #31: disable test until we find something better...
        String string = (String)aValue;
        CSTRING cstring = (CSTRING)cprimitive;

        //check pattern matching
        check_list:
        {
            if (cstring.sizeOfListArray() > 0) {
                for (String pattern : cstring.getListArray()) {
                    if (string.matches(pattern))
                        break check_list;
                }
                ValidationException.raise(path, "Could not find a pattern matching string:'" + string+"'", "STR01");
            }
        }
        */
    }
}
