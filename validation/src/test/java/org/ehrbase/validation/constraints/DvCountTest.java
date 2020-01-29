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
import com.nedap.archie.rm.datavalues.quantity.DvCount;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public class DvCountTest extends ConstraintTestBase {

    @Before
    public void setUp() {
        try {
            setUpContext("./src/test/resources/constraints/dvcount.xml");
        } catch (Exception e) {
            fail();
        }
    }


    @Test
    public void testConstraintValidation() {
        DvCount count = new DvCount(10L);

        try {
            new CArchetypeConstraint(null).validate("test", count, archetypeconstraint);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }


    @Test
    public void testConstraintValidationNullMagnitude() {
        DvCount count = new DvCount(null);

        try {
            new CArchetypeConstraint(null).validate("test", count, archetypeconstraint);
            fail("mandatory element not detected");
        } catch (Exception e) {

        }
    }
}
