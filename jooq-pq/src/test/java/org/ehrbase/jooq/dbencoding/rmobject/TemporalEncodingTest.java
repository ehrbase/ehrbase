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
package org.ehrbase.jooq.dbencoding.rmobject;

import static org.junit.jupiter.api.Assertions.*;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.TemporalAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.date.DvDateAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.datetime.DvDateTimeAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.time.DvTimeAttributes;
import org.junit.jupiter.api.Test;

public class TemporalEncodingTest {

    @Test
    public void testChronoFieldSupport2() {
        YearMonth yearMonth = YearMonth.parse("2020-08");

        DvDate dvDate = new DvDate(yearMonth);

        // check supported ChronoField

        assertTrue(dvDate.getValue().isSupported(ChronoField.YEAR));
        assertTrue(dvDate.getValue().isSupported(ChronoField.MONTH_OF_YEAR));
        assertFalse(dvDate.getValue().isSupported(ChronoField.DAY_OF_MONTH));

        DvDateAttributes dvDateAttributes = DvDateAttributes.instanceFromValue(dvDate);

        assertTrue(dvDateAttributes.isRmDvDate());

        assertTrue(dvDateAttributes.isDateYYYYMM());
        assertFalse(dvDateAttributes.isDateYYYY());
        assertFalse(dvDateAttributes.isDateYYYYMMDD());
    }

    @Test
    public void testChronoFieldSupport3() {

        DvDateTime dvDateTime = new DvDateTime("2020-08-19T12:12:12.123Z");

        DvDateTimeAttributes dvDateTimeAttributes = DvDateTimeAttributes.instanceFromValue(dvDateTime);

        assertTrue(dvDateTimeAttributes.isRmDvDateTime());
    }

    @Test
    public void testChronoFieldSupportTime() {

        DvTime dvTime = new DvTime("10:10:10Z");

        DvTimeAttributes timeAttributes = DvTimeAttributes.instanceFromValue(dvTime);

        assertTrue(timeAttributes.isRmDvTime());

        assertEquals(Long.valueOf(36610), timeAttributes.getTimeStamp());

        assertFalse(timeAttributes.isTimeHH());
        assertFalse(timeAttributes.isTimeHHMM());
        assertTrue(timeAttributes.isTimeHHMMSS());
        assertFalse(timeAttributes.isTimeHHMMSSmmm());
        //        String encoded = new TemporalEncoding().toDB(dvDate);
        assertEquals("10:10:10Z", timeAttributes.getValueAsProvided().toString());

        assertEquals(
                Integer.valueOf(TemporalAttributes.DV_TIME
                        | TemporalAttributes.HOUR
                        | TemporalAttributes.MINUTE_OF_HOUR
                        | TemporalAttributes.SECOND_OF_MINUTE),
                timeAttributes.getSupportedChronoFields());
    }

    @Test
    public void testChronoFieldSupportTimeHHMM() {

        DvTime dvTime = new DvTime("10:10");

        DvTimeAttributes timeAttributes = DvTimeAttributes.instanceFromValue(dvTime);

        assertTrue(timeAttributes.isRmDvTime());

        assertFalse(timeAttributes.isTimeHH());
        assertTrue(timeAttributes.isTimeHHMM());
        assertFalse(timeAttributes.isTimeHHMMSS());
        assertFalse(timeAttributes.isTimeHHMMSSmmm());
        assertEquals("10:10", timeAttributes.getValueAsProvided().toString());

        assertEquals(
                Integer.valueOf(
                        TemporalAttributes.DV_TIME | TemporalAttributes.HOUR | TemporalAttributes.MINUTE_OF_HOUR),
                timeAttributes.getSupportedChronoFields());
    }

    @Test
    public void testChronoFieldSupportTimeHH() {

        DvTime dvTime = new DvTime("10");

        DvTimeAttributes timeAttributes = DvTimeAttributes.instanceFromValue(dvTime);

        // Archie encode this time as 10:00, should we enforce this minimalist encoding?
        assertFalse(timeAttributes.isTimeHH());

        assertTrue(timeAttributes.isTimeHHMM());
        assertFalse(timeAttributes.isTimeHHMMSS());
        assertFalse(timeAttributes.isTimeHHMMSSmmm());
        assertEquals("10:00", timeAttributes.getValueAsProvided().toString());

        assertEquals(
                Integer.valueOf(
                        TemporalAttributes.DV_TIME | TemporalAttributes.HOUR | TemporalAttributes.MINUTE_OF_HOUR),
                timeAttributes.getSupportedChronoFields());

        assertEquals(
                "YYYY-MM-DD\"T\"HH24:MI",
                timeAttributes.getISOdateTimeSQLFormatter(timeAttributes.getSupportedChronoFields()));
    }

    @Test
    public void testChronoFieldSupportDateTimeYYYYMMDDHHMMSSTZ() {

        DvDateTime dvDateTime = new DvDateTime("2020-12-12T10:10:10Z");

        DvDateTimeAttributes dateTimeAttributes = DvDateTimeAttributes.instanceFromValue(dvDateTime);

        assertTrue(dateTimeAttributes.isRmDvDateTime());

        assertTrue(dateTimeAttributes.isDateTimeYYYYMMDDHHMMSS());
        assertFalse(dateTimeAttributes.isDateTimeYYYYMM());

        assertEquals(
                "2020-12-12T10:10:10Z", dateTimeAttributes.getValueAsProvided().toString());
        assertEquals(
                "2020-12-12T10:10:10Z", dateTimeAttributes.getValueExtended().toString());

        // GMT: Saturday, December 12, 2020 10:10:10 AM == 1607767810 (https://www.epochconverter.com/)
        assertEquals(Long.valueOf(1607767810), dateTimeAttributes.getTimeStamp());

        assertEquals(
                Integer.valueOf(TemporalAttributes.DV_DATE_TIME
                        | TemporalAttributes.YEAR
                        | TemporalAttributes.MONTH_OF_YEAR
                        | TemporalAttributes.DAY_OF_MONTH
                        | TemporalAttributes.HOUR
                        | TemporalAttributes.MINUTE_OF_HOUR
                        | TemporalAttributes.SECOND_OF_MINUTE),
                dateTimeAttributes.getSupportedChronoFields());
    }

    @Test
    public void testChronoFieldSupportDateTimeYYYYMMDD() {

        DvDateTime dvDateTime = new DvDateTime("2020-12-12");

        DvDateTimeAttributes dateTimeAttributes = DvDateTimeAttributes.instanceFromValue(dvDateTime);

        assertTrue(dateTimeAttributes.isDateTimeYYYYMMDD());

        assertFalse(dateTimeAttributes.isDateTimeYYYYMMDDHHMMSS());
        assertFalse(dateTimeAttributes.isDateTimeYYYYMM());
        assertEquals("2020-12-12", dateTimeAttributes.getValueAsProvided().toString());

        // GMT: Saturday, December 12, 2020 12:00:00 AM == 1607731200 (https://www.epochconverter.com/)
        assertEquals(Long.valueOf(1607731200), dateTimeAttributes.getTimeStamp());

        assertEquals(
                Integer.valueOf(TemporalAttributes.DV_DATE_TIME
                        | TemporalAttributes.YEAR
                        | TemporalAttributes.MONTH_OF_YEAR
                        | TemporalAttributes.DAY_OF_MONTH),
                dateTimeAttributes.getSupportedChronoFields());
    }
}
