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
import org.openehr.schemas.v1.CDVORDINAL;
import org.openehr.schemas.v1.DVCODEDTEXT;
import org.openehr.schemas.v1.DVORDINAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 1/31/2018.
 */
public class Ordinal {

    static final String type = "DV_ORDINAL";

    CDVORDINAL cdvordinal;
    Map<String, TermDefinition> termDef;

    public Ordinal(CDVORDINAL cdvordinal, Map<String, TermDefinition> termDef) {
        this.cdvordinal = cdvordinal;
        this.termDef = termDef;
    }

    public Map toMap(String name) {
        Map<String, Object> retmap = new HashMap<>();

        retmap.put(Constants.MANDATORY_ATTRIBUTES, new ValuePoint(type).attributes());
        retmap.put(Constants.TYPE, type);
        retmap.put(Constants.ATTRIBUTE_NAME, name);

        List mappingList = new ArrayList<>();

        if (cdvordinal.isSetAssumedValue())
            retmap.put(Constants.ASSUMED_VALUE, cdvordinal.getAssumedValue());

        Map<String, Object> mappingMap = new HashMap<>();

        mappingMap.put(Constants.SYMBOL, mappingList);

        retmap.put(Constants.CONSTRAINT, mappingMap);


        for (DVORDINAL dvordinal : cdvordinal.getListArray()) {
            HashMap mapping = new HashMap();

            mapping.put(Constants.VALUE, dvordinal.getValue());

            if (!dvordinal.getSymbol().getValue().isEmpty())
                mapping.put(Constants.SYMBOL, dvordinal.getSymbol().getValue());

            //get the code and term associated
            DVCODEDTEXT dvcodedtext = dvordinal.getSymbol();

            org.openehr.schemas.v1.CODEPHRASE code = dvcodedtext.getDefiningCode();

            mapping.put(Constants.CODE, code.getCodeString());
            mapping.put(Constants.TERMINOLOGY, code.getTerminologyId().getValue());
            mapping.put(Constants.DESCRIPTION, termDef.get(code.getCodeString()).getValue());

            if (dvordinal.getNormalRange() != null) {
                Map rangeMap = new HashMap<>();

                mapping.put(Constants.LIMITS, rangeMap);

                rangeMap.put(Constants.MIN_OP, dvordinal.getNormalRange().isSetLowerIncluded() ? ">=" : ">");
                rangeMap.put(Constants.MIN, dvordinal.getNormalRange().isSetLower() ? dvordinal.getNormalRange().getLower() : -1);
                rangeMap.put(Constants.MAX_OP, dvordinal.getNormalRange().isSetUpperIncluded() ? "<=" : "<");
                rangeMap.put(Constants.MAX, dvordinal.getNormalRange().isSetUpper() ? dvordinal.getNormalRange().getUpper() : -1);
            }

            if (dvordinal.getNormalStatus() != null) {
                Map<String, Object> statusMap = new HashMap<>();
                mapping.put(Constants.NORMAL_STATUS, statusMap);

                org.openehr.schemas.v1.CODEPHRASE codephrase = dvordinal.getNormalStatus();

                statusMap.put(Constants.TERMINOLOGY, codephrase.getTerminologyId().getValue());
                statusMap.put(Constants.CODE, codephrase.getCodeString());
            }
            mappingList.add(mapping);

        }

        return retmap;
    }
}
