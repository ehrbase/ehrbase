/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime;

import java.util.HashMap;
import java.util.Map;

public abstract class TemporalAttributes implements I_TemporalAttributes {

    protected TemporalAttributes() {}

    /**
     * ChronoField as bitmask
     * 4 bytes
     * 76543210765432107654321076543210
     * 00000000000000000000000000000000
     * type   + ext.  + date  + time  +
     * ext -> future extensions if applicable
     **/
    public static final int DV_DATE = 0b00000001000000000000000000000000;

    public static final int DV_TIME = 0b00000010000000000000000000000000;
    public static final int DV_DATE_TIME = 0b00000100000000000000000000000000;
    public static final int MILLI_OF_SECOND = 0b00000000000000000000000000000001;
    public static final int SECOND_OF_MINUTE = 0b00000000000000000000000000000010;
    public static final int MINUTE_OF_HOUR = 0b00000000000000000000000000000100;
    public static final int HOUR = 0b00000000000000000000000000001000;
    public static final int DAY_OF_MONTH = 0b00000000000000000000000100000000;
    public static final int MONTH_OF_YEAR = 0b00000000000000000000001000000000;
    public static final int YEAR = 0b00000000000000000000010000000000;

    /**
     * SQL formatter depending on the actual bitmask and attributes
     * to be used with to_char(timestamp, text) (https://www.postgresql.org/docs/current/functions-formatting.html)
     */
    protected static final Map<Integer, String> ISODateTimeSQLFormatters = new HashMap<>();

    static {
        // Non partial representations
        ISODateTimeSQLFormatters.put(DV_DATE, "YYYY-MM-DD");
        ISODateTimeSQLFormatters.put(DV_TIME, "HH24:MI:SS.MS");
        ISODateTimeSQLFormatters.put(DV_DATE_TIME, "YYYY-MM-DD\"T\"HH24:MI:SS.MS");
        // DvDate
        ISODateTimeSQLFormatters.put(DV_DATE | YEAR, "YYYY");
        ISODateTimeSQLFormatters.put(DV_DATE | YEAR | MONTH_OF_YEAR, "YYYY-MM");
        ISODateTimeSQLFormatters.put(DV_DATE | YEAR | MONTH_OF_YEAR | DAY_OF_MONTH, "YYYY-MM-DD");
        // DvDateTime
        ISODateTimeSQLFormatters.put(DV_DATE_TIME | YEAR, "YYYY");
        ISODateTimeSQLFormatters.put(DV_DATE_TIME | YEAR | MONTH_OF_YEAR, "YYYY-MM");
        ISODateTimeSQLFormatters.put(DV_DATE_TIME | YEAR | MONTH_OF_YEAR | DAY_OF_MONTH, "YYYY-MM-DD");
        ISODateTimeSQLFormatters.put(
                DV_DATE_TIME | YEAR | MONTH_OF_YEAR | DAY_OF_MONTH | HOUR, "YYYY-MM-DD\"T\"HH24:00");
        ISODateTimeSQLFormatters.put(
                DV_DATE_TIME | YEAR | MONTH_OF_YEAR | DAY_OF_MONTH | HOUR | MINUTE_OF_HOUR, "YYYY-MM-DD\"T\"HH24:MI");
        ISODateTimeSQLFormatters.put(
                DV_DATE_TIME | YEAR | MONTH_OF_YEAR | DAY_OF_MONTH | HOUR | MINUTE_OF_HOUR | SECOND_OF_MINUTE,
                "YYYY-MM-DD\"T\"HH24:MI:SS");
        ISODateTimeSQLFormatters.put(
                DV_DATE_TIME
                        | YEAR
                        | MONTH_OF_YEAR
                        | DAY_OF_MONTH
                        | HOUR
                        | MINUTE_OF_HOUR
                        | SECOND_OF_MINUTE
                        | MILLI_OF_SECOND,
                "YYYY-MM-DD\"T\"HH24:MI:SS");
        // DvTime
        ISODateTimeSQLFormatters.put(DV_TIME | HOUR, "YYYY-MM-DD\"T\"HH24:00");
        ISODateTimeSQLFormatters.put(DV_TIME | HOUR | MINUTE_OF_HOUR, "YYYY-MM-DD\"T\"HH24:MI");
        ISODateTimeSQLFormatters.put(DV_TIME | HOUR | MINUTE_OF_HOUR | SECOND_OF_MINUTE, "YYYY-MM-DD\"T\"HH24:MI:SS");
        ISODateTimeSQLFormatters.put(
                DV_TIME | HOUR | MINUTE_OF_HOUR | SECOND_OF_MINUTE | MILLI_OF_SECOND, "YYYY-MM-DD\"T\"HH24:MI:SS");
    }

    public String getISOdateTimeSQLFormatter(Integer bitmask) {
        return ISODateTimeSQLFormatters.get(bitmask);
    }
}
