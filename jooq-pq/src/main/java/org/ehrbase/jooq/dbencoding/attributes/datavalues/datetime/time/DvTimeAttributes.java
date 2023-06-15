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
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.I_PartialTime;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.I_TimeAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.TemporalAttributes;

/**
 * decorator for DvTime.
 * Add attributes and handling for partial time (f.e. 10:00)
 * Provide a defaulted representation (f.e. 10:00:00 for the above)
 * Calculate a timestamp based on the defaulted representation
 */
public class DvTimeAttributes extends TemporalAttributes implements I_TimeAttributes {

    private I_TimeAttributes timeAttributes;
    private static ZoneOffset zoneOffset;

    private DvTimeAttributes(I_TimeAttributes timeAttributes) {
        this.timeAttributes = timeAttributes;
    }

    public static DvTimeAttributes instanceFromValue(TemporalAccessor timeValue) {

        if (!timeValue.isSupported(ChronoField.HOUR_OF_DAY)) return null;

        try {
            zoneOffset = ZoneOffset.from(timeValue);
        } catch (Exception e) {
            zoneOffset = null;
        }

        if (zoneOffset != null) {
            OffsetTime offsetTime = OffsetTime.of(
                    timeValue.get(ChronoField.HOUR_OF_DAY),
                    timeValue.get(ChronoField.MINUTE_OF_HOUR),
                    timeValue.get(ChronoField.SECOND_OF_MINUTE),
                    timeValue.get(ChronoField.NANO_OF_SECOND),
                    zoneOffset);

            return instanceFromValue(new DvTime(offsetTime));
        } else {
            LocalTime localTime = LocalTime.of(
                    timeValue.get(ChronoField.HOUR_OF_DAY),
                    timeValue.get(ChronoField.MINUTE_OF_HOUR),
                    timeValue.get(ChronoField.SECOND_OF_MINUTE),
                    timeValue.get(ChronoField.NANO_OF_SECOND));
            return instanceFromValue(new DvTime(localTime));
        }
    }

    public static DvTimeAttributes instanceFromValue(DvTime dvTime) {
        I_TimeAttributes timeAttributes;
        I_PartialTime partialTime = PartialTime.getInstance(dvTime);

        if (partialTime.ishhmmssfff()) {
            timeAttributes = new DvTimehhmmssfffImp(dvTime);
        } else if (partialTime.ishhmmss()) {
            timeAttributes = new DvTimehhmmssImp(dvTime);
        } else if (partialTime.ishhmm()) {
            timeAttributes = new DvTimehhmmImp(dvTime);
        } else if (partialTime.ishh()) {
            timeAttributes = new DvTimehhImp(dvTime);
        } else
            throw new IllegalArgumentException(
                    "Invalid time:" + dvTime.getValue().toString());

        return new DvTimeAttributes(timeAttributes);
    }

    @Override
    public Long getMagnitude() {
        return timeAttributes.getMagnitude();
    }

    @Override
    public Temporal getValueAsProvided() {
        return timeAttributes.getValueAsProvided();
    }

    @Override
    public Temporal getValueExtended() {
        return timeAttributes.getValueExtended();
    }

    @Override
    public Integer getSupportedChronoFields() {
        return timeAttributes.getSupportedChronoFields();
    }

    @Override
    public Long getTimeStamp() {
        return timeAttributes.getTimeStamp();
    }

    @Override
    public boolean isRmDvTime() {
        return timeAttributes.isRmDvTime();
    }

    @Override
    public boolean isTimeHH() {
        return timeAttributes.isTimeHH();
    }

    @Override
    public boolean isTimeHHMM() {
        return timeAttributes.isTimeHHMM();
    }

    @Override
    public boolean isTimeHHMMSS() {
        return timeAttributes.isTimeHHMMSS();
    }

    @Override
    public boolean isTimeHHMMSSmmm() {
        return timeAttributes.isTimeHHMMSSmmm();
    }

    @Override
    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }
}
