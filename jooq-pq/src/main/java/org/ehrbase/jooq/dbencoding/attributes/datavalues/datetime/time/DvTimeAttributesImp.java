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
import java.time.temporal.Temporal;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.I_TimeAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.TemporalAttributes;

abstract class DvTimeAttributesImp extends TemporalAttributes implements I_TimeAttributes {

    protected final DvTime dvTime;
    protected ZoneOffset zoneOffset;

    DvTimeAttributesImp(DvTime dvTime) {
        super();
        this.dvTime = dvTime;
        if (dvTime.getValue().isSupported(ChronoField.OFFSET_SECONDS)) zoneOffset = ZoneOffset.from(dvTime.getValue());
    }

    @Override
    public Temporal getValueAsProvided() {
        return (Temporal) dvTime.getValue();
    }

    @Override
    public Long getMagnitude() {
        return Math.round(dvTime.getMagnitude());
    }

    @Override
    public Long getTimeStamp() {
        // since time is not related to a date, return the magnitude
        return getMagnitude();
    }

    public Integer supportedChronoFields(Integer chronoFieldBitmask) {
        return TemporalAttributes.DV_TIME | chronoFieldBitmask;
    }

    @Override
    public boolean isRmDvTime() {
        return (getSupportedChronoFields() & DV_TIME) == DV_TIME;
    }

    @Override
    public boolean isTimeHH() {
        return getSupportedChronoFields() == (DV_TIME | HOUR);
    }

    @Override
    public boolean isTimeHHMM() {
        return getSupportedChronoFields() == (DV_TIME | HOUR | MINUTE_OF_HOUR);
    }

    @Override
    public boolean isTimeHHMMSS() {
        return getSupportedChronoFields() == (DV_TIME | HOUR | MINUTE_OF_HOUR | SECOND_OF_MINUTE);
    }

    @Override
    public boolean isTimeHHMMSSmmm() {
        return getSupportedChronoFields() == (DV_TIME | HOUR | MINUTE_OF_HOUR | SECOND_OF_MINUTE | MILLI_OF_SECOND);
    }

    @Override
    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }
}
