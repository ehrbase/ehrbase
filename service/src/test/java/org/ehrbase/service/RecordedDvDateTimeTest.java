/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.Test;

public class RecordedDvDateTimeTest {

    @Test
    public void testToTimestamp() {
        OffsetDateTime now = OffsetDateTime.now();
        Timestamp timestamp = new RecordedDvDateTime(new DvDateTime(now)).toTimestamp();

        assertEquals(now.toInstant(), timestamp.toInstant());
    }

    @Test
    public void testDecodeDvDateTime() {
        ZoneOffset zoneId = ZoneOffset.of("-06:00");
        Instant now = Instant.now();

        DvDateTime dateTime;

        dateTime = new RecordedDvDateTime()
                .decodeDvDateTime(Timestamp.valueOf(LocalDateTime.ofInstant(now, zoneId)), zoneId.getId());
        assertEquals(OffsetDateTime.ofInstant(now, zoneId), dateTime.getValue());

        dateTime = new RecordedDvDateTime().decodeDvDateTime(Timestamp.from(now), null);
        assertEquals(LocalDateTime.ofInstant(now, ZoneId.systemDefault()), dateTime.getValue());

        dateTime = new RecordedDvDateTime().decodeDvDateTime(Timestamp.from(now), zoneId.getId());
        assertNotEquals(OffsetDateTime.ofInstant(now, ZoneOffset.UTC), dateTime.getValue());
    }
}
