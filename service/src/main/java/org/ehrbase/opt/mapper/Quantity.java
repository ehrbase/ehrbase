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
import org.openehr.schemas.v1.CDVQUANTITY;
import org.openehr.schemas.v1.CQUANTITYITEM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 1/31/2018.
 */
public class Quantity {

    final String type = "DV_QUANTITY";

    CDVQUANTITY cdq;
    Map<String, TermDefinition> termDef;

    public Quantity(CDVQUANTITY cdq, Map<String, TermDefinition> termDef) {
        this.cdq = cdq;
        this.termDef = termDef;
    }

    public Map toMap(String name) {

        Map<String, Object> retmap = new HashMap<>();

        CQUANTITYITEM[] qtyitems = cdq.getListArray();

        retmap.put(Constants.MANDATORY_ATTRIBUTES, new ValuePoint(type).attributes());
        retmap.put(Constants.TYPE, type);
        retmap.put(Constants.ATTRIBUTE_NAME, name);

        Map<String, Object> constraintsMap = new HashMap<>();

        retmap.put(Constants.CONSTRAINT, constraintsMap);

        if (cdq.isSetAssumedValue()) {
            constraintsMap.put(Constants.ASSUMED_VALUE, cdq.getAssumedValue().getMagnitude());
        }

        Map<String, Object> range = new HashMap<>();
        constraintsMap.put(Constants.OCCURRENCE, range);
        range.put(Constants.MIN_OP, cdq.getOccurrences().isSetLowerIncluded() ? ">=" : ">");
        range.put(Constants.MIN, cdq.getOccurrences().isSetLower() ? cdq.getOccurrences().getLower() : -1);
        range.put(Constants.MAX_OP, cdq.getOccurrences().isSetUpperIncluded() ? "<=" : "<");
        range.put(Constants.MAX, cdq.getOccurrences().isSetUpper() ? cdq.getOccurrences().getUpper() : -1);


        if (cdq.getProperty() != null) {
            String code = cdq.getProperty().getCodeString();
            String terminology = cdq.getProperty().getTerminologyId().getValue();


            Map<String, String> propertyMap = new HashMap<>();

            propertyMap.put(Constants.CODE, code);
            propertyMap.put(Constants.TERMINOLOGY, terminology);
            if (terminology.equals("openehr"))
                propertyMap.put(Constants.LABEL, TerminologyServiceImp.getInstance().getLabelForCode(code, "en"));

            retmap.put(Constants.TERM_BINDING, propertyMap);
        }

        List<Map<String, Object>> validationList = new ArrayList<>();

        for (CQUANTITYITEM item : qtyitems) {

            if (qtyitems == null || qtyitems.length == 0) {
                continue;
            }

//            Map itemMap = new HashMap<>();

            Map validationMap = new HashMap<>();

            Map rangeMap = new HashMap<>();

            if (item.getMagnitude() != null) {
                validationMap.put(Constants.MAGNITUDE, rangeMap);
                rangeMap.put(Constants.MIN_OP, item.getMagnitude().isSetLowerIncluded() ? ">=" : ">");
                rangeMap.put(Constants.MIN, item.getMagnitude().isSetLower() ? item.getMagnitude().getLower() : -1);
                rangeMap.put(Constants.MAX_OP, item.getMagnitude().isSetUpperIncluded() ? "<=" : "<");
                rangeMap.put(Constants.MAX, item.getMagnitude().isSetUpper() ? item.getMagnitude().getUpper() : -1);
            }
//            validationMap.put(Constants.MANDATORY_ATTRIBUTES, Constants.UNITS);
            validationMap.put(Constants.UNITS, item.getUnits());

            validationList.add(validationMap);
        }

        if (!validationList.isEmpty())
            constraintsMap.put(Constants.VALIDATION, validationList);

        return retmap;
    }
}
