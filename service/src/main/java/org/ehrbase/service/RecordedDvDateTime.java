package org.ehrbase.service;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import org.ehrbase.api.exception.InternalServerException;

import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import static java.time.LocalTime.now;

public class RecordedDvDateTime {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";
    private DvDateTime dateTime;

    public RecordedDvDateTime(DvDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public RecordedDvDateTime() {
    }

    public Timestamp toTimestamp() {
        TemporalAccessor accessor = dateTime.getValue();
        long millis;

        if (!accessor.isSupported(ChronoField.OFFSET_SECONDS)){
            //set timezone at default locale
            ZonedDateTime zonedDateTime = LocalDateTime.from(accessor).atZone(ZoneId.systemDefault());
            accessor = zonedDateTime.toOffsetDateTime();
            millis = accessor.getLong(ChronoField.INSTANT_SECONDS) * 1000 + accessor.getLong(ChronoField.MILLI_OF_SECOND);
        }
        else {
            LocalDateTime localDateTime = LocalDateTime.from(accessor);
            millis = localDateTime.toInstant(ZoneOffset.from(ZonedDateTime.now())).toEpochMilli();
        }


        return new Timestamp(millis);
    }

    public String zoneId() {
        TemporalAccessor accessor = dateTime.getValue();
        if (accessor instanceof OffsetDateTime)
            //set timezone at default locale
            return ((OffsetDateTime)accessor).toZonedDateTime().getZone().getId();
        else
            return ZoneId.systemDefault().getId();
    }

    /**
     * Decodes and creates RM object instance from given {@link Timestamp} representation
     *
     * @param timestamp input as {@link Timestamp}
     * @param timezone  TODO doc! what format is the timezone in?
     * @return RM object generated from input
     * @throws InternalServerException on failure
     */
    // TODO unit test - until this is done please note this method was refactored from joda to java time classes
    public DvDateTime decodeDvDateTime(Timestamp timestamp, String timezone) {
        if (timestamp == null) return null;

        Optional<LocalDateTime> codedLocalDateTime = Optional.empty();
        Optional<ZonedDateTime> zonedDateTime = Optional.empty();

        if (timezone != null)
            zonedDateTime = Optional.of(timestamp.toInstant().atZone(ZoneId.of(timezone)));
        else
            codedLocalDateTime = Optional.of(timestamp.toLocalDateTime());

        Optional<String> convertedDateTime = codedLocalDateTime.map(i -> i.format(java.time.format.DateTimeFormatter.ofPattern(DATE_FORMAT)));
        if (convertedDateTime.isEmpty())
            convertedDateTime = zonedDateTime.map(i -> i.format(java.time.format.DateTimeFormatter.ofPattern(DATE_FORMAT)));

        return new DvDateTime(convertedDateTime.orElseThrow(() -> new InternalServerException("Decoding DvDateTime failed")));
    }
}
