package org.ehrbase.service;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import org.apache.tomcat.jni.Local;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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

        dateTime = new RecordedDvDateTime().decodeDvDateTime(Timestamp.valueOf(LocalDateTime.ofInstant(now, zoneId)), zoneId.getId());
        assertEquals(OffsetDateTime.ofInstant(now, zoneId), dateTime.getValue());

        dateTime = new RecordedDvDateTime().decodeDvDateTime(Timestamp.from(now), null);
        assertEquals(LocalDateTime.ofInstant(now, ZoneId.systemDefault()), dateTime.getValue());

        dateTime = new RecordedDvDateTime().decodeDvDateTime(Timestamp.from(now), zoneId.getId());
        assertNotEquals(OffsetDateTime.ofInstant(now, ZoneOffset.UTC), dateTime.getValue());
    }
}
