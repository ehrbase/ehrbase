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

import com.nedap.archie.rm.datavalues.DataValue;
import com.nedap.archie.rm.datavalues.quantity.DvInterval;

public class CompositeClassName {

    private DataValue dataValue;

    public CompositeClassName(DataValue dataValue) {
        this.dataValue = dataValue;
    }

    /**
     * extrapolate composite class name such as DvInterval<DvCount>
     *
     * @return
     */
    public String toString() {
        String classname = new SimpleClassName(dataValue).toString();

        if ("DvInterval".equals(classname)
                && !(((DvInterval) dataValue).getLower() == null
                        && ((DvInterval) dataValue).getUpper() == null)) { // get the classname of lower/upper
            DvInterval<?> interval = (DvInterval<?>) dataValue;
            String lowerClassName = null;
            String upperClassName = null;

            // either lower or upper or both are set value
            if (interval.getLower() != null) lowerClassName = new SimpleClassName(interval.getLower()).toString();

            if (interval.getUpper() != null) upperClassName = new SimpleClassName(interval.getUpper()).toString();

            if (lowerClassName != null && upperClassName != null && (!lowerClassName.equals(upperClassName)))
                throw new IllegalArgumentException(
                        "Lower and Upper classnames do not match:" + lowerClassName + " vs." + upperClassName);

            return classname + "<" + (lowerClassName != null ? lowerClassName : upperClassName) + ">";
        }
        return classname;
    }
}
