/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.test_data.composition;

import java.io.InputStream;

public enum CompositionTestDataCanonicalXML {
    ALL_TYPES("Valid Contains all node types ", "test_all_types.v1.xml"),
    DIADEM("Valid Contains all node types ", "diadem.xml"),
    DIADEM_DEFAULT_SCHEMA("Valid Contains default schema ", "diadem_default_schema.xml"),
    RIPPLE_CONFORMANCE_FULL("Full Ripple comformance test sample", "RIPPLE_conformanceTesting.xml"),
    RIPPLE_COMFORMANCE_ACTION("Action section from Ripple comformance", "RIPPLE_conformanceTesting_ACTION.procedure.v1.xml"),
    RIPPLE_COMFORMANCE_ADMIN_ENTRY("Admin entry section from Ripple comformance", "RIPPLE_conformanceTesting_ADMIN_ENTRY.xml"),
    RIPPLE_COMFORMANCE_EVALUATION("Evaluation section from Ripple comformance", "RIPPLE_conformanceTesting_EVALUATION.cpr_decision_uk.xml"),
    RIPPLE_COMFORMANCE_OBSERVATION_DEMO("Observation demo section from Ripple comformance", "RIPPLE_conformanceTesting_OBSERVATION.demo.v1.xml"),
    RIPPLE_COMFORMANCE_OBSERVATION_PULSE("Observation UI demo from Ripple comformance", "RIPPLE_conformanceTesting_OBSERVATION.pulse.v1.xml"),
    RIPPLE_COMFORMANCE_INSTRUCTION("Instruction section from Ripple comformance", "RIPPLE_conformanceTesting_INSTRUCTION.request-procedure.v1.xml"),
    REGISTRO_DE_ATENDIMENTO("Duplicate section headings with different name", "Registro_de_Atendimento_Clinico.xml"),
    ALL_TYPES_FIXED("test all node types without archetype details at the moment", "test_all_types.fixed.v1.xml"),
    ALL_TYPES_INVALID_PARTICIPATIONS("simple test with participations holding a wrong value", "test_all_types_participations_invalid.xml"),
    ALL_TYPES_NO_CONTENT("a composition with null content", "test_all_no_content.xml");



    private final String filename;
    private final String description;

    CompositionTestDataCanonicalXML(String description, String filename) {
        this.filename = filename;
        this.description = description;
    }


    public InputStream getStream() {
        return getClass().getResourceAsStream("/composition/canonical_xml/" + filename);
    }
}
