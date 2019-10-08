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

import org.ehrbase.validation.constraints.wrappers.CArchetypeConstraint;
import com.nedap.archie.rm.datavalues.quantity.DvInterval;
import com.nedap.archie.rm.datavalues.quantity.DvQuantity;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public class DvIntervalDvQuantityTest extends ConstraintTestBase {

    @Before
    public void setUp() {
        try {
            setUpContext("./src/test/resources/constraints/interval_dvquantity.xml");
        } catch (Exception e) {
            fail();
        }
    }


    @Test
    public void testConstraintValidation() {
        DvInterval<DvQuantity> dvQuantityDvInterval = new DvInterval<>(new DvQuantity("in", 100D, 2L), new DvQuantity("in", 200D, 2L));

        try {
            new CArchetypeConstraint(null).validate("test", dvQuantityDvInterval, archetypeconstraint);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
