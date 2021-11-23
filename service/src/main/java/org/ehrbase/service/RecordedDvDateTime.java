package org.ehrbase.service;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

public class RecordedDvDateTime {

    private DvDateTime dateTime;

    public RecordedDvDateTime() {
    }

    public RecordedDvDateTime(DvDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Timestamp toTimestamp() {
        TemporalAccessor temporal = dateTime.getValue();
        return Timestamp.valueOf(LocalDateTime.from(temporal));
    }

    public Optional<String> zoneId() {
        TemporalAccessor accessor = dateTime.getValue();
        if (accessor instanceof OffsetDateTime) {
            ZoneOffset offset = ((OffsetDateTime) accessor).getOffset();
            return Optional.of(offset.getId());
        } else {
            return Optional.empty();
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

        TemporalAccessor temporal;
        if (timezone != null) {
            temporal = timestamp.toLocalDateTime().atOffset(ZoneOffset.of(timezone));
        } else {
            temporal = timestamp.toLocalDateTime();
        }
        return new DvDateTime(temporal);
    }
}
