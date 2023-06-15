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

/**
 * handles specific time String representation evaluation
 */
public interface I_PartialTime {

    /**
     * true if time is hh:mm:ss:mmm
     */
    boolean ishhmmssfff();

    /**
     * true if time is hh:mm:ss
     */
    boolean ishhmmss();

    /**
     * true if time is hh:mm
     */
    boolean ishhmm();

    /**
     * true if time is hh
     * NB. never true with the current Java API
     */
    boolean ishh();

    /**
     * true if a Zone Offset is present
     */
    boolean hasTZString();

    /**
     * true if format: HH:MM:SS:mmm
     */
    boolean isNonCompactIS8601Representation();
}
