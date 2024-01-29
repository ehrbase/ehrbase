/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.io.IOException;
import org.ehrbase.openehr.sdk.validation.webtemplate.DvCodedTextValidator;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplateNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 */
@SuppressWarnings("NewClassNamingConvention")
@Disabled
class DvCodedTextIT {

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected WebTemplateNode parseNode(String file) throws IOException {
        return objectMapper.readValue(getClass().getResourceAsStream(file), WebTemplateNode.class);
    }

    private final FhirTerminologyValidation fhirTerminologyValidator =
            new FhirTerminologyValidation("https://r4.ontoserver.csiro.au/fhir");

    private final DvCodedTextValidator validator = new DvCodedTextValidator(fhirTerminologyValidator);

    @Test
    void testValidate_UnsupportedExternalTerminology() throws Exception {
        var node = parseNode("/webtemplate_nodes/dv_codedtext_unsupported.json");
        var dvCodedText = new DvCodedText(
                "Iodine-deficiency related thyroid disorders and allied conditions",
                new CodePhrase(new TerminologyId("ICD10"), "E01"));

        var result = validator.validate(dvCodedText, node);
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidate_FhirCodeSystem() throws Exception {
        var codePhrase = new CodePhrase(new TerminologyId("http://hl7.org/fhir/observation-status"), "final");

        var node = parseNode("/webtemplate_nodes/dv_codedtext_fhir_codesystem.json");

        var result = validator.validate(new DvCodedText("Final", codePhrase), node);
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidate_FhirCodeSystem_WrongTerminologyId() throws Exception {
        var codePhrase = new CodePhrase(new TerminologyId("http://hl7.org/fhir/name-use"), "usual");

        var node = parseNode("/webtemplate_nodes/dv_codedtext_fhir_codesystem.json");

        var result = validator.validate(new DvCodedText("Usual", codePhrase), node);
        assertTrue(result.size() > 0);
    }

    @Test
    void testValidate_FhirCodeSystem_WrongCode() throws Exception {
        var codePhrase = new CodePhrase(new TerminologyId("http://hl7.org/fhir/observation-status"), "casual");

        var node = parseNode("/webtemplate_nodes/dv_codedtext_fhir_codesystem.json");

        var result = validator.validate(new DvCodedText("Casual", codePhrase), node);
        assertTrue(result.size() > 0);
    }

    @Test
    void testValidate_FhirValueSet() throws Exception {
        var codePhrase =
                new CodePhrase(new TerminologyId("http://terminology.hl7.org/CodeSystem/v3-EntityNameUseR2"), "ANON");

        var node = parseNode("/webtemplate_nodes/dv_codedtext_fhir_valueset.json");

        var result = validator.validate(new DvCodedText("Anonymous", codePhrase), node);
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidate_FhirValueSet_WrongTerminologyId() throws Exception {
        var codePhrase = new CodePhrase(new TerminologyId("http://snomed.info/sct"), "ANON");

        var node = parseNode("/webtemplate_nodes/dv_codedtext_fhir_valueset.json");

        var result = validator.validate(new DvCodedText("Anonymous", codePhrase), node);
        assertTrue(result.size() > 0);
    }

    @Test
    void testValidate_FhirValueSet_WrongCode() throws Exception {
        var codePhrase =
                new CodePhrase(new TerminologyId("http://terminology.hl7.org/CodeSystem/v3-EntityNameUseR2"), "UKN");

        var node = parseNode("/webtemplate_nodes/dv_codedtext_fhir_valueset.json");

        var result = validator.validate(new DvCodedText("Anonymous", codePhrase), node);
        assertTrue(result.size() > 0);
    }
}
