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

public interface I_DateAttributes extends I_TemporalAttributes {

    /**
     * true if the date is an RM DvDate instance
     * @see com.nedap.archie.rm.datavalues.quantity.datetime.DvDate
     */
    boolean isRmDvDate();

    /**
     * true if the date is partial with year only
     */
    boolean isDateYYYY();

    /**
     * true if the date is partial with year-month only
     */
    boolean isDateYYYYMM();

    /**
     * true if date is ISO8601 canonical
     */
    boolean isDateYYYYMMDD();
}
