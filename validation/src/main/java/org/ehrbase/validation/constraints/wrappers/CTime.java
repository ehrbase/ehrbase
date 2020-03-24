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
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import org.openehr.schemas.v1.CPRIMITIVE;
import org.openehr.schemas.v1.CTIME;

import java.time.temporal.Temporal;
import java.util.Map;

/**
 * Validate a DvTime
 *
 * @see DvTime
 * @link https://specifications.openehr.org/releases/AM/latest/AOM1.4.html#_c_time_class
 *
 * Created by christian on 7/22/2016.
 */
public class CTime extends CConstraint implements I_CTypeValidate {
    CTime(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, CPRIMITIVE cprimitive) throws IllegalArgumentException {

        if (!(aValue instanceof Temporal))
            ValidationException.raise(path, "INTERNAL: Time validation expects a java-date-time argument", "TIME_01");

        CTIME ctime = (CTIME) cprimitive;
        String dvTimeStr = aValue.toString();

        //check pattern if any
        new DateTimeSyntax(path, dvTimeStr, ctime.isSetPattern() ? ctime.getPattern() : null).validate();

        //range check
        DvTime time = new DvTime(dvTimeStr);

        if (ctime.isSetRange())
            IntervalComparator.isWithinBoundaries(time, ctime.getRange());

        validateTimeZone(path, time, ctime);
    }

    private void validateTimeZone(String path, DvTime dvTime, CTIME ctime) throws IllegalArgumentException {
//        if (ctime.isSetTimezoneValidity() && ctime.getTimezoneValidity().equals(new BigInteger("1001")) && dvTime.getDateTime().getZone() == null) {
//            ValidationException.raise(path, "Time zone is mandatory", "DATE02");
//        }
//        if (ctime.isSetTimezoneValidity() && ctime.getTimezoneValidity().equals(new BigInteger("1003")) && dvTime.getDateTime().getZone() != null) {
//            ValidationException.raise(path, "Time zone is not allowed", "DATE03");
//        }
    }
}
