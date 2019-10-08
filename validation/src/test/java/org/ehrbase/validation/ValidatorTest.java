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

package org.ehrbase.validation;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.xml.JAXBUtil;
import org.apache.xmlbeans.XmlException;
import org.junit.Test;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.fail;

public class ValidatorTest {


    @Test
    public void testValidateComposition1() throws JAXBException, IOException, XmlException {
        Unmarshaller unmarshaller = JAXBUtil.createRMContext().createUnmarshaller();
        Composition composition = (Composition) unmarshaller.unmarshal(new FileInputStream(new File("./src/test/resources/composition/IDCR-LabReportRAW1.xml")));
        OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(new FileInputStream("./src/test/resources/operational_templates/IDCR-LaboratoryTestReport.opt")).getTemplate();

        try {
            new Validator(template).check(composition);
        }catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateComposition2() throws JAXBException, IOException, XmlException {
        Unmarshaller unmarshaller = JAXBUtil.createRMContext().createUnmarshaller();
        Composition composition = (Composition) unmarshaller.unmarshal(new FileInputStream(new File("./src/test/resources/composition/IDCR Problem List.v1.xml")));
        OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(new FileInputStream("./src/test/resources/operational_templates/IDCR Problem List.v1.opt")).getTemplate();

        try {
            new Validator(template).check(composition);
        }catch (Exception e){
            fail("fail");
        }
    }

    @Test
    public void testValidateCompositionWithBadCodePhrase() throws JAXBException, IOException, XmlException {
        Unmarshaller unmarshaller = JAXBUtil.createRMContext().createUnmarshaller();
        Composition composition = (Composition) unmarshaller.unmarshal(new FileInputStream(new File("./src/test/resources/composition/IDCR - Adverse Reaction List  Bad CodePhrase at0021.v1.xml")));
        OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(new FileInputStream("./src/test/resources/operational_templates/IDCR - Adverse Reaction List.v1.opt")).getTemplate();

        try {
            new Validator(template).check(composition);
            fail("Undetected bad code phrase");
        }catch (Exception e){
            System.out.println(e);
        }
    }

    @Test
    public void testValidateCompositionBadValueCodedText() throws JAXBException, IOException, XmlException {
        Unmarshaller unmarshaller = JAXBUtil.createRMContext().createUnmarshaller();
        Composition composition = (Composition) unmarshaller.unmarshal(new FileInputStream(new File("./src/test/resources/composition/IDCR - Adverse Reaction List Bad Coded Value.v1.xml")));
        OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(new FileInputStream("./src/test/resources/operational_templates/IDCR - Adverse Reaction List.v1.opt")).getTemplate();

        try {
            new Validator(template).check(composition);
            fail("Undetected bad value");
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testValidateCompositionOKCodedText() throws JAXBException, IOException, XmlException {
        Unmarshaller unmarshaller = JAXBUtil.createRMContext().createUnmarshaller();
        Composition composition = (Composition) unmarshaller.unmarshal(new FileInputStream(new File("./src/test/resources/composition/IDCR - Adverse Reaction List.v1.xml")));
        OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(new FileInputStream("./src/test/resources/operational_templates/IDCR - Adverse Reaction List.v1.opt")).getTemplate();

        try {
            new Validator(template).check(composition);
        }catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateRippleConformance() throws JAXBException, IOException, XmlException {
        Unmarshaller unmarshaller = JAXBUtil.createRMContext().createUnmarshaller();
        Composition composition = (Composition) unmarshaller.unmarshal(new FileInputStream(new File("./src/test/resources/composition/RIPPLE-ConformanceTest.xml")));
        OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(new FileInputStream("./src/test/resources/operational_templates/RIPPLE-ConformanceTest.opt")).getTemplate();

        try {
            new Validator(template).check(composition);
        }catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateTestAllTypes() throws JAXBException, IOException, XmlException {
        Unmarshaller unmarshaller = JAXBUtil.createRMContext().createUnmarshaller();
        Composition composition = (Composition) unmarshaller.unmarshal(new FileInputStream(new File("./src/test/resources/composition/test_all_types.fixed.v1.xml")));
        OPERATIONALTEMPLATE template = TemplateDocument.Factory.parse(new FileInputStream("./src/test/resources/operational_templates/Test all types.opt")).getTemplate();

        try {
            new Validator(template).check(composition);
        }catch (Exception e){
            fail(e.getMessage());
        }
    }
}
