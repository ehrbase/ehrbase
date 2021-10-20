package org.ehrbase.service;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.Instant;
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
        ZoneId zoneId;
        DvDateTime dateTime;
        Instant now = Instant.now();

        zoneId = ZoneId.systemDefault();
        dateTime = new RecordedDvDateTime().decodeDvDateTime(Timestamp.from(now), zoneId.getId());
        assertEquals(OffsetDateTime.ofInstant(now, zoneId), dateTime.getValue());

        zoneId = ZoneId.of("Europe/Paris");
        dateTime = new RecordedDvDateTime().decodeDvDateTime(Timestamp.from(now), zoneId.getId());
        assertEquals(OffsetDateTime.ofInstant(now, zoneId), dateTime.getValue());

        dateTime = new RecordedDvDateTime().decodeDvDateTime(Timestamp.from(now), zoneId.getId());
        assertNotEquals(OffsetDateTime.ofInstant(now, ZoneOffset.UTC), dateTime.getValue());
    }
}
