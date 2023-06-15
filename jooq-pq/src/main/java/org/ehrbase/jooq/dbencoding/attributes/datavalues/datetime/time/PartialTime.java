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
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.I_PartialTime;

public abstract class PartialTime implements I_PartialTime {

    String timeZoneString;
    String dvTimeRepresentation;

    protected PartialTime(DvTime dvTime) {
        this.dvTimeRepresentation = dvTime.getValue().toString();

        if (dvTime.getValue().isSupported(ChronoField.OFFSET_SECONDS)) {
            timeZoneString = ZoneOffset.from(dvTime.getValue()).toString();
            dvTimeRepresentation = StringUtils.remove(dvTimeRepresentation, timeZoneString);
        }
    }

    public static I_PartialTime getInstance(DvTime dvTime) {
        if (dvTime.getValue().toString().contains(":")) return new StandardPartialTime(dvTime);
        else throw new IllegalArgumentException("ISO8601 time compact form is not yet supported");
    }

    @Override
    public boolean hasTZString() {
        return timeZoneString != null;
    }

    @Override
    public boolean isNonCompactIS8601Representation() {
        return dvTimeRepresentation.contains(":");
    }
}
