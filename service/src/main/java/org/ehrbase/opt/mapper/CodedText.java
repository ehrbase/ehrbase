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
import org.ehrbase.service.TerminologyServiceImp;
import org.openehr.schemas.v1.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 1/31/2018.
 */
public class CodedText {

    final String type = "DV_CODED_TEXT";

    CCOMPLEXOBJECT ccomplexobject;
    Map<String, TermDefinition> termDef;

    public CodedText(CCOMPLEXOBJECT ccomplexobject, Map<String, TermDefinition> termDef) {
        this.ccomplexobject = ccomplexobject;
        this.termDef = termDef;
    }

    public Map<String, Object> toMap(String name) {
//        Map<String, Object> reddtmap = new HashMap<>();

        Map<String, Object> attributeMap = new HashMap<>();
        Map<String, Object> constraintsMap = new HashMap<>();

        boolean isComplete = false;

        COBJECT[] defining_codes = null;
        for (CATTRIBUTE cattribute : ccomplexobject.getAttributesArray()) {
            if (cattribute.getRmAttributeName().equals(Constants.DEFINING_CODE)) {
                defining_codes = cattribute.getChildrenArray();
            }
        }

        List valueList = new ArrayList<>();

        if (defining_codes != null) {
            for (COBJECT cobject : defining_codes) {
                if (cobject instanceof CCODEPHRASE) {
                    attributeMap.put(Constants.TYPE, type);
                    isComplete = true;
                    for (String code : ((CCODEPHRASE) cobject).getCodeListArray()) {
                        Map<String, Object> codeMap = new HashMap<>();
                        codeMap.put(Constants.CODE_STRING, code);
                        codeMap.put(Constants.TERMINOLOGY, ((CCODEPHRASE) cobject).getTerminologyId().getValue());
                        if (termDef.get(code) != null) {
                            codeMap.put(Constants.VALUE, termDef.get(code).getValue());
                            codeMap.put(Constants.DESCRIPTION, termDef.get(code).getDescription());
                        } else if (((CCODEPHRASE) cobject).getTerminologyId() != null && "openehr".equals(((CCODEPHRASE) cobject).getTerminologyId().getValue())) {
                            if (TerminologyServiceImp.getInstance() != null)
                                codeMap.put(Constants.VALUE, TerminologyServiceImp.getInstance().getLabelForCode(code, "en"));

                        }
                        valueList.add(codeMap);
                    }
                } else if (cobject instanceof CONSTRAINTREF) {
                    CONSTRAINTREF constraintref = (CONSTRAINTREF) cobject;
                    String reference = constraintref.getReference();
                    //and then...
                    //can be safely ignored...???
//                    throw new IllegalArgumentException("buh...");
                }
            }
        }

        if (valueList.size() > 0)
            constraintsMap.put(Constants.DEFINING_CODE, valueList);

        Map<String, Object> range = new HashMap<>();

        attributeMap.put(Constants.CONSTRAINT, constraintsMap);
        constraintsMap.put(Constants.OCCURRENCE, range);
        range.put(Constants.MIN_OP, ccomplexobject.getOccurrences().isSetLowerIncluded() ? ">=" : ">");
        range.put(Constants.MIN, ccomplexobject.getOccurrences().isSetLower() ? ccomplexobject.getOccurrences().getLower() : -1);
        range.put(Constants.MAX_OP, ccomplexobject.getOccurrences().isSetUpperIncluded() ? "<=" : "<");
        range.put(Constants.MAX, ccomplexobject.getOccurrences().isSetUpper() ? ccomplexobject.getOccurrences().getUpper() : -1);

        if (!isComplete) {
            return null;
        }

        attributeMap.put(Constants.MANDATORY_ATTRIBUTES, new ValuePoint(type).attributes());
        attributeMap.put(Constants.ATTRIBUTE_NAME, name);

        return attributeMap;
    }
}
