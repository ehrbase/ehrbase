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
package org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.time;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;

public class StandardPartialTime extends PartialTime {

    public StandardPartialTime(DvTime dvTime) {
        super(dvTime);
    }

    @Override
    public boolean ishhmmssfff() {
        return dvTimeRepresentation.length() > 8;
    }

    @Override
    public boolean ishhmmss() {
        return dvTimeRepresentation.length() == 8;
    }

    @Override
    public boolean ishhmm() {
        return dvTimeRepresentation.length() == 5;
    }

    @Override
    public boolean ishh() {
        return dvTimeRepresentation.length() == 2;
    }
}
