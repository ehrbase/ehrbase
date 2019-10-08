/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
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

package org.ehrbase.opt.mapper;

import org.ehrbase.opt.TermDefinition;
import org.ehrbase.opt.ValuePoint;
import org.openehr.schemas.v1.CCODEPHRASE;
import org.openehr.schemas.v1.TERMINOLOGYID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 1/31/2018.
 */
public class CodePhrase {

    final String type = "CODE_PHRASE";

    CCODEPHRASE ccodephrase;
    Map<String, TermDefinition> termDef;

    public CodePhrase(CCODEPHRASE ccodephrase, Map<String, TermDefinition> termDef) {
        this.ccodephrase = ccodephrase;
        this.termDef = termDef;
    }

    public Map toMap(String name) {
        Map<String, Object> retmap = new HashMap<>();

        retmap.put(Constants.MANDATORY_ATTRIBUTES, new ValuePoint(type).attributes());
        retmap.put(Constants.TYPE, type);
        retmap.put(Constants.ATTRIBUTE_NAME, name);

        TERMINOLOGYID tid = ccodephrase.getTerminologyId();
        String[] codeList = ccodephrase.getCodeListArray();

        List attributeList = new ArrayList<>();

        if (tid != null)
            retmap.put(Constants.TERMINOLOGY, tid.getValue());

        retmap.put(Constants.CONSTRAINT, attributeList);

        for (String code : codeList) {

            Map<String, Object> codeMap = new HashMap<>();

            codeMap.put(Constants.VALUE, code);
            if (termDef.get(code) != null) {
                codeMap.put(Constants.LABEL, termDef.get(code).getValue());
                codeMap.put(Constants.DESCRIPTION, termDef.get(code).getDescription());
            }

            attributeList.add(codeMap);
        }

        return retmap;

    }
}
