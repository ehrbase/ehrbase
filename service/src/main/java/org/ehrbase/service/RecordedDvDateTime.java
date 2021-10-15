package org.ehrbase.service;

import com.nedap.archie.datetime.DateTimeFormatters;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;

public class RecordedDvDateTime {

    private DvDateTime dateTime;

    public RecordedDvDateTime() {
    }

    public RecordedDvDateTime(DvDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Timestamp toTimestamp() {
        TemporalAccessor temporal = dateTime.getValue();

        if (temporal instanceof OffsetDateTime) {
            return Timestamp.from(((OffsetDateTime) temporal).toInstant());
        } else {
            return Timestamp.valueOf(LocalDateTime.from(temporal));
        }
    }

    public String zoneId() {
        TemporalAccessor accessor = dateTime.getValue();
        if (accessor instanceof OffsetDateTime) {
            return ((OffsetDateTime) accessor).getOffset().getId();
        } else {
            return ZoneId.systemDefault().getId();
        }
    }


    /**
     * Decodes given timestamp and timezone into a openEHR {@link DvDateTime} object.
     *
     * @param timestamp date-time as {@link Timestamp}
     * @param timezone  time zone for the given timestamp
     * @return DvDateTime instance
     */
    public DvDateTime decodeDvDateTime(Timestamp timestamp, String timezone) {
        if (timestamp == null) {
            return null;
        }
        ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = timestamp.toInstant().atZone(zoneId);
        return new DvDateTime(DateTimeFormatters.ISO_8601_DATE_TIME.format(zonedDateTime));
    }
}
