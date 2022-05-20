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

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

public class RecordedDvDateTime {

    private DvDateTime dateTime;

    public RecordedDvDateTime() {}

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
