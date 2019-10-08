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

import org.apache.xmlbeans.XmlException;
import org.junit.Before;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.CCOMPLEXOBJECT;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public abstract class ConstraintTestBase {

    ARCHETYPECONSTRAINT archetypeconstraint;

    protected void setUpContext(String constraintPath) throws IOException, XmlException {
        archetypeconstraint = CCOMPLEXOBJECT.Factory.parse(new FileInputStream(constraintPath));
        assertNotNull(archetypeconstraint);
    }
}
