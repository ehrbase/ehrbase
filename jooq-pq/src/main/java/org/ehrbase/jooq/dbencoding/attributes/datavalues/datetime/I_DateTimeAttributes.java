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

import java.time.ZoneOffset;

public interface I_DateTimeAttributes extends I_TemporalAttributes {
    /**
     * @return true if the date/time is a RM DvDateTime
     * @see com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime
     */
    boolean isRmDvDateTime();

    /**
     * @return true if the date/time is partial with year only
     */
    boolean isDateTimeYYYY();

    /**
     * @return true if the date/time is partial with year-month only
     */
    boolean isDateTimeYYYYMM();

    /**
     * @return true if the date/time is partial with year-month-day only
     */
    boolean isDateTimeYYYYMMDD();

    /**
     * @return true if the date/time is partial with year-month-day-hour only
     * NB. this is never true as a partial time with hours only is always
     * converted to hh:00 by the Java API
     */
    boolean isDateTimeYYYYMMDDHH();

    /**
     * @return true if the date/time is partial with year-month-day-hour-minutes only
     */
    boolean isDateTimeYYYYMMDDHHMM();

    /**
     * @return true if the date/time is partial with year-month-day-hour-minutes-seconds only
     */
    boolean isDateTimeYYYYMMDDHHMMSS();

    /**
     * @return true if the date/time is partial with year-month-day-hour-minutes-millisecs
     */
    boolean isDateTimeYYYYMMDDHHMMSSmmm();

    /**
     * @return return the Zone Offset for this date/time
     */
    ZoneOffset getZoneOffset();
}
