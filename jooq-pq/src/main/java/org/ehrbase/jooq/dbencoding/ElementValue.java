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
package org.ehrbase.jooq.dbencoding;

import com.nedap.archie.base.Interval;
import com.nedap.archie.rm.datavalues.DataValue;
import com.nedap.archie.rm.datavalues.quantity.DvInterval;

/**
 * this class handles special DataValues for DB serialization
 */
public class ElementValue {
    DataValue value;

    public ElementValue(DataValue value) {
        this.value = value;
    }

    /**
     * a normalize value is either a DATA_VALUE or rm.base type (at the moment Interval)
     * this is required to normalize AQL path accesses to the data attributes
     *
     * @return the actualized object following the DB encoding convention
     */
    public Object normalize() {
        if (value instanceof DvInterval) {
            DvInterval<?> dvInterval = (DvInterval<?>) value;
            Interval<?> interval = new Interval<>(dvInterval.getLower(), dvInterval.getUpper());
            interval.setLowerIncluded(dvInterval.isLowerIncluded());
            interval.setUpperIncluded(dvInterval.isUpperIncluded());
            interval.setLowerUnbounded(dvInterval.isLowerUnbounded());
            interval.setUpperUnbounded(dvInterval.isUpperUnbounded());
            return interval;
        }
        return value;
    }
}
