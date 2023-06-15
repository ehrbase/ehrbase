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
package org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.datetime;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.I_DateAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.I_DateTimeAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.I_TimeAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.TemporalAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.date.DvDateAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.time.DvTimeAttributes;

/**
 * decorator for DvDateTime.
 * Add attributes and handling for partial date/time (f.e. 2019-12)
 * Provide a defaulted representation (f.e. 2019-12-01:00:00:00Z for the above)
 * Calculate a timestamp based on the defaulted representation
 */
public class DvDateTimeAttributes extends TemporalAttributes implements I_DateTimeAttributes {

    protected DvDateTime dvDateTime;
    protected I_DateAttributes datePart;
    protected I_TimeAttributes timePart;
    protected ZoneOffset zoneOffset;

    private DvDateTimeAttributes(DvDateTime dvDateTime, I_DateAttributes datePart, I_TimeAttributes timePart) {
        this.dvDateTime = dvDateTime;
        this.datePart = datePart;
        this.timePart = timePart;
        this.zoneOffset = timePart == null ? null : timePart.getZoneOffset();
    }

    public static DvDateTimeAttributes instanceFromValue(DvDateTime dvDateTime) {

        TemporalAccessor localDate;
        TemporalAccessor actual = dvDateTime.getValue();

        if (actual.isSupported(ChronoField.YEAR)
                && actual.isSupported(ChronoField.MONTH_OF_YEAR)
                && actual.isSupported(ChronoField.DAY_OF_MONTH))
            localDate = LocalDate.of(
                    actual.get(ChronoField.YEAR),
                    actual.get(ChronoField.MONTH_OF_YEAR),
                    actual.get(ChronoField.DAY_OF_MONTH));
        else if (actual.isSupported(ChronoField.YEAR) && actual.isSupported(ChronoField.MONTH_OF_YEAR))
            localDate = YearMonth.of(actual.get(ChronoField.YEAR), actual.get(ChronoField.MONTH_OF_YEAR));
        else if (actual.isSupported(ChronoField.YEAR)) localDate = Year.of(actual.get(ChronoField.YEAR));
        else throw new IllegalArgumentException("DvDateTime supplied is not valid:" + actual);

        DvTimeAttributes dvTimeAttributes = DvTimeAttributes.instanceFromValue(dvDateTime.getValue());
        DvDateAttributes dvDateAttributes = DvDateAttributes.instanceFromValue(new DvDate((Temporal) localDate));

        if (dvTimeAttributes == null) {
            dvDateTime = new DvDateTime(dvDateAttributes.getValueAsProvided());
        }

        return new DvDateTimeAttributes(dvDateTime, dvDateAttributes, dvTimeAttributes);
    }

    @Override
    public Long getMagnitude() {
        return LocalDate.from(getValueExtended()).toEpochDay();
    }

    @Override
    public Temporal getValueAsProvided() {
        return (Temporal) dvDateTime.getValue();
    }

    @Override
    public Temporal getValueExtended() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(datePart.getValueExtended());
        stringBuilder.append("T");
        stringBuilder.append(timePart == null ? "00:00:00" : timePart.getValueExtended());
        if (zoneOffset != null) return OffsetDateTime.parse(stringBuilder.toString());
        else return LocalDateTime.parse(stringBuilder.toString());
    }

    @Override
    public Integer getSupportedChronoFields() {
        return TemporalAttributes.DV_DATE_TIME
                | (datePart.getSupportedChronoFields() & ~TemporalAttributes.DV_DATE)
                | (timePart == null ? 0 : timePart.getSupportedChronoFields() & ~TemporalAttributes.DV_TIME);
    }

    @Override
    public Long getTimeStamp() {
        if (zoneOffset == null)
            return LocalDateTime.parse(getValueExtended().toString()).toEpochSecond(ZoneOffset.UTC);
        else return OffsetDateTime.parse(getValueExtended().toString()).toEpochSecond();
    }

    @Override
    public boolean isRmDvDateTime() {
        if (timePart == null) return false;
        return datePart.isDateYYYYMMDD() && (timePart.isTimeHHMMSSmmm() || timePart.isTimeHHMMSS());
    }

    @Override
    public boolean isDateTimeYYYY() {
        return datePart.isDateYYYY();
    }

    @Override
    public boolean isDateTimeYYYYMM() {
        return datePart.isDateYYYYMM();
    }

    @Override
    public boolean isDateTimeYYYYMMDD() {
        return datePart.isDateYYYYMMDD();
    }

    @Override
    public boolean isDateTimeYYYYMMDDHH() {
        if (timePart == null) return false;
        return datePart.isDateYYYYMMDD() && timePart.isTimeHH();
    }

    @Override
    public boolean isDateTimeYYYYMMDDHHMM() {
        if (timePart == null) return false;
        return datePart.isDateYYYYMMDD() && timePart.isTimeHHMM();
    }

    @Override
    public boolean isDateTimeYYYYMMDDHHMMSS() {
        if (timePart == null) return false;
        return datePart.isDateYYYYMMDD() && timePart.isTimeHHMMSS();
    }

    @Override
    public boolean isDateTimeYYYYMMDDHHMMSSmmm() {
        if (timePart == null) return false;
        return datePart.isDateYYYYMMDD() && timePart.isTimeHHMMSSmmm();
    }

    @Override
    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }
}
