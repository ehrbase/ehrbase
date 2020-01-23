/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.validation.constraints;

import com.nedap.archie.rm.datavalues.quantity.DvInterval;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import org.ehrbase.validation.constraints.wrappers.CArchetypeConstraint;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.fail;

public class DvIntervalDvDateTimeTest extends ConstraintTestBase {

    @Before
    public void setUp() {
        try {
            setUpContext("./src/test/resources/constraints/interval_dvdatetime.xml");
        } catch (Exception e) {
            fail();
        }
    }


    @Test
    public void testConstraintValidation() {
        DvInterval<DvDateTime> dvDateTimeDvInterval = new DvInterval<>(new DvDateTime(LocalDateTime.now()), null);

        try {
            new CArchetypeConstraint(null).validate("test", dvDateTimeDvInterval, archetypeconstraint);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
