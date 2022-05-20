/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql.queryimpl.value_field;

import java.util.regex.Pattern;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * check for various ISO8601 patterns
 * Created by christian on 1/8/2018.
 */
public class ISODateTime {

    String isoDateTimeZ8601regex =
            "^(?:[1-9]\\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:Z|[+-][01]\\d:[0-5]\\d)$";
    String isoDate8601regex =
            "^(?:[1-9]\\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)$";
    String isoTime8601regex = "^(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:Z|[+-][01]\\d:[0-5]\\d)$";

    String dateCandidate;

    public ISODateTime(String dateCandidate) {
        this.dateCandidate = dateCandidate;
    }

    public boolean isValidDateTimeExpression() {
        Pattern pattern = Pattern.compile(isoDateTimeZ8601regex);
        return pattern.matcher(dateCandidate).matches();
    }

    public boolean isValidDateExpression() {
        Pattern pattern = Pattern.compile(isoDate8601regex);
        return pattern.matcher(dateCandidate).matches();
    }

    public boolean isValidTimeExpression() {
        Pattern pattern = Pattern.compile(isoTime8601regex);
        return pattern.matcher(dateCandidate).matches();
    }

    public long toTimeStamp() {
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis();
        return parser.parseDateTime(dateCandidate).getMillis();
    }
}
