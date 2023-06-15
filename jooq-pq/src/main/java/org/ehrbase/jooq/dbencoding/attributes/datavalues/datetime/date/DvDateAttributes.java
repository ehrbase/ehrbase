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
package org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.date;

import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.I_DateAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.TemporalAttributes;

/**
 * decorator for DvDate.
 * Add attributes and handling for partial date (f.e. 2019-12)
 * Provide a defaulted representation (f.e. 2019-12-01 for the above)
 * Calculate a timestamp based on the defaulted representation
 */
public class DvDateAttributes extends TemporalAttributes implements I_DateAttributes {

    private I_DateAttributes dateAttributes;

    private DvDateAttributes(I_DateAttributes dateAttributes) {
        this.dateAttributes = dateAttributes;
    }

    public static DvDateAttributes instanceFromValue(DvDate dvDate) {
        I_DateAttributes dateAttributes;

        if (dvDate.getValue().isSupported(ChronoField.YEAR)
                && dvDate.getValue().isSupported(ChronoField.MONTH_OF_YEAR)
                && dvDate.getValue().isSupported(ChronoField.DAY_OF_MONTH)) {
            dateAttributes = new DvDateYYYYMMDDImp(dvDate);
        } else if (dvDate.getValue().isSupported(ChronoField.YEAR)
                && dvDate.getValue().isSupported(ChronoField.MONTH_OF_YEAR)) {
            dateAttributes = new DvDateYYYYMMImp(dvDate);
        } else if (dvDate.getValue().isSupported(ChronoField.YEAR)) {
            dateAttributes = new DvDateYYYYImp(dvDate);
        } else
            throw new IllegalArgumentException(
                    "Invalid date:" + dvDate.getValue().toString());

        return new DvDateAttributes(dateAttributes);
    }

    @Override
    public Long getMagnitude() {
        return dateAttributes.getMagnitude();
    }

    @Override
    public Temporal getValueAsProvided() {
        return dateAttributes.getValueAsProvided();
    }

    @Override
    public Temporal getValueExtended() {
        return dateAttributes.getValueExtended();
    }

    @Override
    public Integer getSupportedChronoFields() {
        return dateAttributes.getSupportedChronoFields();
    }

    @Override
    public Long getTimeStamp() {
        return null;
    }

    @Override
    public boolean isRmDvDate() {
        return dateAttributes.isRmDvDate();
    }

    @Override
    public boolean isDateYYYY() {
        return dateAttributes.isDateYYYY();
    }

    @Override
    public boolean isDateYYYYMM() {
        return dateAttributes.isDateYYYYMM();
    }

    @Override
    public boolean isDateYYYYMMDD() {
        return dateAttributes.isDateYYYYMMDD();
    }
}
