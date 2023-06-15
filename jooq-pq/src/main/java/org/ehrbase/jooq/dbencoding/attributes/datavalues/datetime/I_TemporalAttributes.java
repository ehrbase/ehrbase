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

import java.time.temporal.Temporal;

public interface I_TemporalAttributes {

    /**
     * get the epoch offset from a defaulted representation if applicable.
     * @see I_TemporalAttributes::getValueExtended()
     * @return Long
     */
    Long getMagnitude();

    /**
     * return the value passed as an argument to the decorator
     * @return Temporal
     */
    Temporal getValueAsProvided();

    /**
     * default the missing fields according the argument class.
     * f.e.
     * - 2019-12 -> 2019-12-01 for a DvDate
     * - 2019-12 -> 2019-12-01T00:00:00 for a DvDateTime
     *
     * @return Temporal
     */
    Temporal getValueExtended();

    /**
     * return a bitmask of the supported ChronoFields for this argument
     * @link ChronoFields
     * @return a bitmask as in {@link TemporalAttributes}
     */
    Integer getSupportedChronoFields();

    /**
     * @return return the timestamp calculated from the extended representation as a full ISO8601 date/time
     * that is for a DvDate, it is converted to its ISO8601 equivalent (YYYY-MM-DDThh:mm:ss[TZ])
     */
    Long getTimeStamp();

    /**
     * return the formatter to use to represent this argument as String from SQL
     * @param bitmask bit map
     * @return the formatter string
     */
    String getISOdateTimeSQLFormatter(Integer bitmask);
}
