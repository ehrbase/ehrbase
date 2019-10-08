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

package org.ehrbase.validation.constraints.util;

public class DateTimeSyntax {

    private final String dateString;
    private final String pattern;
    private final String path;


    public DateTimeSyntax(String path, String dateString, String pattern) {
        this.pattern = pattern;
        this.dateString = dateString;
        this.path = path; //used for error message
    }

    public void validate(){
        //TODO: do a smarter test based on the supplied pattern...
//        if (!DvDateTime.isValidISO8601DateTime(dvDateStr))
//           ValidationException.raise(path, "Supplied date/time is not ISO8601 compatible:" + dvDateStr, "DATE04");

        //check pattern if any
        if (pattern != null) {
            //check ISO8601 validity
//            try {
//
//                DvDateTimeParser.toDateTimeString(dvDateStr, cdatetime.getPattern());
//            } catch (Exception e) {
//                throw new ValidationException(path, "Supplied value does not match pattern:" + dvDateStr + " (expected:" + cdatetime.getPattern() + "), error:"+e);
//            }
        }
    }
}
