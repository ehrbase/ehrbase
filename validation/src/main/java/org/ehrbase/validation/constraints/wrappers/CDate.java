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

import org.ehrbase.validation.constraints.util.DateTimeSyntax;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import org.openehr.schemas.v1.CDATE;
import org.openehr.schemas.v1.CPRIMITIVE;

import java.time.temporal.Temporal;
import java.util.Map;

/**
 * Validate a DvDate
 *
 * @see DvDate
 * @see com.nedap.archie.aom.primitives.CDate
 * @link https://specifications.openehr.org/releases/AM/latest/AOM1.4.html#_c_date_class
 *
 * Created by christian on 7/23/2016.
 */
public class CDate extends CConstraint implements I_CTypeValidate {

    CDate(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, CPRIMITIVE cprimitive) throws IllegalArgumentException {
        if (!(aValue instanceof Temporal))
            ValidationException.raise(path, "INTERNAL: Time validation expects a java-date-time argument", "DATE_01");

        CDATE cdate = (CDATE) cprimitive;
        String dvDateStr = aValue.toString();

        //check pattern if any
        new DateTimeSyntax(path, dvDateStr, cdate.isSetPattern() ? cdate.getPattern() : null).validate();

        //range check
        DvDate date = new DvDate(dvDateStr);

        if (cdate.isSetRange())
            IntervalComparator.isWithinBoundaries(date, cdate.getRange());

        validateTimeZone(path, date, cdate);
    }

    private void validateTimeZone(String path, DvDate dvDate, CDATE cdate) throws IllegalArgumentException {
        //TODO: migrate to Temporal
//        if (cdate.isSetTimezoneValidity() && cdate.getTimezoneValidity().equals(new BigInteger("1001")) && dvDate.getValue().get(ChronoField.OFFSET_SECONDS) == null) {
//            ValidationException.raise(path, "Time zone is mandatory", "DATE02");
//        }
//        if (cdate.isSetTimezoneValidity() && cdate.getTimezoneValidity().equals(new BigInteger("1003")) && dvDate.getValue().getZone() != null) {
//            ValidationException.raise(path, "Time zone is not allowed", "DATE03");
//        }
    }
}
