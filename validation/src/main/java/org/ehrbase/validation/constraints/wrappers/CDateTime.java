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

import com.nedap.archie.datetime.DateTimeFormatters;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import org.ehrbase.validation.constraints.util.DateTimeSyntax;
import org.openehr.schemas.v1.CDATETIME;
import org.openehr.schemas.v1.CPRIMITIVE;

import java.time.temporal.Temporal;
import java.util.Map;

/**
 * validate a DvDateTime
 *
 * @link https://specifications.openehr.org/releases/AM/latest/AOM1.4.html#_c_date_time_class
 * <p>
 * Created by christian on 7/23/2016.
 * @see DvDateTime
 */
public class CDateTime extends CConstraint implements I_CTypeValidate {

    CDateTime(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, CPRIMITIVE cprimitive) throws IllegalArgumentException {
        if (!(aValue instanceof Temporal))
            ValidationException.raise(path, "INTERNAL: Time validation expects a java-date-time argument", "DATE_TIME_01");

        CDATETIME cdatetime = (CDATETIME) cprimitive;
        String dvDateStr = DateTimeFormatters.ISO_8601_DATE_TIME.format((Temporal) aValue);

        new DateTimeSyntax(path, dvDateStr, cdatetime.isSetPattern() ? cdatetime.getPattern() : null).validate();

        //range check
        DvDateTime dateTime = new DvDateTime(dvDateStr);

        if (cdatetime.isSetRange())
            IntervalComparator.isWithinBoundaries(dateTime, cdatetime.getRange());

        validateTimeZone(path, dateTime, cdatetime);
    }

    private void validateTimeZone(String path, DvDateTime dvDateTime, CDATETIME cdatetime) throws IllegalArgumentException {
//        if (cdatetime.isSetTimezoneValidity() && cdatetime.getTimezoneValidity().equals(new BigInteger("1001")) && dvDateTime.getDateTime().getZone() == null) {
//            ValidationException.raise(path, "Time zone is mandatory", "DATE02");
//        }
//        if (cdatetime.isSetTimezoneValidity() && cdatetime.getTimezoneValidity().equals(new BigInteger("1003")) && dvDateTime.getDateTime().getZone() != null) {
//            ValidationException.raise(path, "Time zone is not allowed", "DATE03");
//        }
    }
}
