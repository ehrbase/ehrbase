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

import org.junit.Test;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;

import java.io.FileInputStream;

import static org.junit.Assert.*;

public class OptConstraintTest {

    @Test
    public void testLoadOperationalTemplate() throws Exception {
        OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(new FileInputStream("./src/test/resources/operational_templates/IDCR-LaboratoryTestReport.opt")).getTemplate();

        //load the template and create the corresponding constraints map
        OptConstraintMapper optConstraint = new OptConstraint().map(template);

        assertNotNull(optConstraint);
    }

}