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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.I_DateAttributes;
import org.ehrbase.jooq.dbencoding.attributes.datavalues.datetime.TemporalAttributes;

public abstract class DvDateAttributesImp extends TemporalAttributes implements I_DateAttributes {

    protected final DvDate dvDate;

    public DvDateAttributesImp(DvDate dvDate) {
        this.dvDate = dvDate;
    }

    @Override
    public Temporal getValueAsProvided() {
        return dvDate.getValue();
    }

    @Override
    public Long getMagnitude() {
        return LocalDate.from(getValueExtended()).toEpochDay();
    }

    @Override
    public Long getTimeStamp() {
        return LocalDateTime.from(LocalDate.parse(getValueExtended().toString() + "T00:00:00"))
                .toEpochSecond(ZoneOffset.UTC);
    }

    public Integer supportedChronoFields(Integer chronoFieldBitmask) {
        return TemporalAttributes.DV_DATE | chronoFieldBitmask;
    }

    @Override
    public boolean isRmDvDate() {
        return (getSupportedChronoFields() & DV_DATE) == DV_DATE;
    }

    @Override
    public boolean isDateYYYY() {
        return getSupportedChronoFields() == (DV_DATE | YEAR);
    }

    @Override
    public boolean isDateYYYYMM() {
        return getSupportedChronoFields() == (DV_DATE | YEAR | MONTH_OF_YEAR);
    }

    @Override
    public boolean isDateYYYYMMDD() {
        return getSupportedChronoFields() == (DV_DATE | YEAR | MONTH_OF_YEAR | DAY_OF_MONTH);
    }
}
