/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.nedap.archie.rm.datavalues.DvCodedText;
import java.util.List;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ExternalTerminologyValidationServerTest {

    @Ignore(
            "This test runs against ontoserver sample instance. It is deactivated until we have a test FHIR terminology server and the architecture allows to run Spring integration tests.")
    @Test
    public void shouldRetrieveValueSet() {
        FhirTerminologyValidation tsserver = null;
        try {
            tsserver = new FhirTerminologyValidation("https://r4.ontoserver.csiro.au/fhir/", true);
        } catch (Exception e) {
            Assert.fail();
        }

        TerminologyParam tp = TerminologyParam.ofServiceApi("//hl7.org/fhir/R4");
        tp.setOperation("expand");
        tp.setParameter("http://hl7.org/fhir/ValueSet/surface");

        List<DvCodedText> result = tsserver.expand(tp);
        // 1: Buccal
        assertThat(result.get(0).getDefiningCode().getCodeString()).isEqualTo("B");
        assertThat(result.get(0).getValue()).isEqualTo("Buccal");
        // 2: Distal
        assertThat(result.get(1).getDefiningCode().getCodeString()).isEqualTo("D");
        assertThat(result.get(1).getValue()).isEqualTo("Distal");
        // 3: Distoclusal
        assertThat(result.get(2).getDefiningCode().getCodeString()).isEqualTo("DO");
        assertThat(result.get(2).getValue()).isEqualTo("Distoclusal");
        // 4: Distoincisal
        assertThat(result.get(3).getDefiningCode().getCodeString()).isEqualTo("DI");
        assertThat(result.get(3).getValue()).isEqualTo("Distoincisal");

        assertThat(result.size()).isEqualTo(11);
    }
}
