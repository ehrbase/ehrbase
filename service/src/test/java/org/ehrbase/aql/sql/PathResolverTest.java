/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql;

import static org.assertj.core.api.Assertions.assertThat;

import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.Contains;
import org.junit.Test;

public class PathResolverTest extends TestAqlBase {

    @Test
    public void testResolvePaths() {
        String query = "select\n" + "a, d\n"
                + "from EHR e\n"
                + "contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]"
                + "  CONTAINS ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        /** mocks the ehr.containment as
         *   comp_id    |   label                                                                               |   path
         *   ?          | openEHR_EHR_COMPOSITION_health_summary_v1                                             |  /composition[openEHR-EHR-COMPOSITION.health_summary.v1]
         *   ?          | openEHR_EHR_COMPOSITION_health_summary_v1.openEHR_EHR_ACTION_immunisation_procedure_v1| /content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']
         */
        PathResolver cut = new PathResolver(knowledge, contains.getIdentifierMapper());

        assertThat(cut.pathOf("IDCR - Immunisation summary.v0", "d").toArray()[0])
                .isEqualTo("/content[openEHR-EHR-ACTION.immunisation_procedure.v1]");
        assertThat(cut.pathOf("IDCR - Immunisation summary.v0", "a").toArray()[0])
                .isEqualTo("/composition[openEHR-EHR-COMPOSITION.health_summary.v1]");
    }
}
