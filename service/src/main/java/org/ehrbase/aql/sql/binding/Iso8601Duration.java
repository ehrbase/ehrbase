/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.binding;

import org.jooq.types.DayToSecond;
import org.jooq.types.YearToMonth;
import org.jooq.types.YearToSecond;
import org.postgresql.util.PGInterval;

public class Iso8601Duration {

    private final PGInterval interval;

    public Iso8601Duration(PGInterval interval) {
        this.interval = interval;
    }

    public String toIsoString() {
        YearToSecond yearToSecond = new YearToSecond(
                new YearToMonth(interval.getYears(), interval.getMonths()),
                new DayToSecond(
                        interval.getDays(), interval.getHours(), interval.getMinutes(), (int) interval.getSeconds()));
        return yearToSecond.toDuration().toString();
    }
}
