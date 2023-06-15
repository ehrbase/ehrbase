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

public interface I_TimeAttributes extends I_TemporalAttributes {

    /**
     * true if time is an RM DvTime instance
     */
    boolean isRmDvTime();

    /**
     * true if time is hours only
     * NB. never true with current Java API which defaults it to HH:MM
     */
    boolean isTimeHH();

    /**
     * true if time is hours-minutes only
     */
    boolean isTimeHHMM();

    /**
     * true if time is hours-minutes-seconds only
     */
    boolean isTimeHHMMSS();

    /**
     * true if time is hours-minutes-seconds-millisecs only
     */
    boolean isTimeHHMMSSmmm();

    /**
     * return the zone offset for this time if any
     * @return ZoneOffset
     */
    ZoneOffset getZoneOffset();
}
