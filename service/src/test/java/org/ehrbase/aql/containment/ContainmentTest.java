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

package org.ehrbase.aql.containment;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainmentTest {


    @Test
    public void setArchetypeId() {
        Containment cut = new Containment(null);

        cut.setArchetypeId(null);
        assertThat(cut.getArchetypeId()).isNull();

        cut.setArchetypeId("[openEHR-EHR-OBSERVATION.laboratory-hba1c.v1]");
        assertThat(cut.getArchetypeId()).isEqualTo("openEHR-EHR-OBSERVATION.laboratory-hba1c.v1");

        cut.setArchetypeId("openEHR-EHR-OBSERVATION.laboratory-hba1c.v1");
        assertThat(cut.getArchetypeId()).isEqualTo("openEHR-EHR-OBSERVATION.laboratory-hba1c.v1");
    }


    public static Containment buildContainment(Containment enclosingContainment, String symbol, String archetypeId, String composition, String path) {
        Containment expected = new Containment(enclosingContainment);
        expected.setSymbol(symbol);
        expected.setArchetypeId(archetypeId);
        expected.setClassName(composition);
        expected.setPath(path);
        return expected;
    }

    public static void checkEqual(Containment containment, Containment expected) {
        assertThat(containment.getSymbol()).isEqualTo(expected.getSymbol());
        assertThat(containment.getArchetypeId()).isEqualTo(expected.getArchetypeId());
        assertThat(containment.getClassName()).isEqualTo(expected.getClassName());
        assertThat(containment.getPath()).isEqualTo(expected.getPath());
        assertThat(containment.getEnclosingContainment()).isEqualTo(expected.getEnclosingContainment());
    }

}