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

package org.ehrbase.aql.sql;

import org.ehrbase.aql.containment.Containment;
import org.ehrbase.aql.containment.ContainmentTest;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.tools.jdbc.MockResult;
import org.junit.Test;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.CONTAINMENT;
import static org.ehrbase.jooq.pg.Tables.ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

public class PathResolverTest {

    @Test
    public void testBuildLquery() {


        {
            //represents contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v]
            Containment containment = ContainmentTest.buildContainment(null, "a", "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
            String expected = PathResolver.buildLquery(containment);
            assertThat(expected).isEqualTo("openEHR_EHR_COMPOSITION_health_summary_v1");
        }

        {
            //represents contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v]  CONTAINS ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]
            Containment containment1 = ContainmentTest.buildContainment(null, "a", "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
            Containment containment2 = ContainmentTest.buildContainment(containment1, "d", "openEHR-EHR-ACTION.immunisation_procedure.v1", "ACTION", null);

            String actual1 = PathResolver.buildLquery(containment1);
            assertThat(actual1).isEqualTo("openEHR_EHR_COMPOSITION_health_summary_v1");

            String actual2 = PathResolver.buildLquery(containment2);
            assertThat(actual2).isEqualTo("*.openEHR_EHR_ACTION_immunisation_procedure_v1");
        }
    }

    @Test
    public void testResolvePaths() {
        //represents contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v]  CONTAINS ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]
        Containment containment1 = ContainmentTest.buildContainment(null, "a", "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null);
        Containment containment2 = ContainmentTest.buildContainment(containment1, "d", "openEHR-EHR-ACTION.immunisation_procedure.v1", "ACTION", null);
        IdentifierMapper identifierMapper = new IdentifierMapper();
        identifierMapper.add(containment1);
        identifierMapper.add(containment2);


        /** mocks the ehr.containment as
         *   comp_id    |   label                                                                               |   path
         *   ?          | openEHR_EHR_COMPOSITION_health_summary_v1                                             |  /composition[openEHR-EHR-COMPOSITION.health_summary.v1]
         *   ?          | openEHR_EHR_COMPOSITION_health_summary_v1.openEHR_EHR_ACTION_immunisation_procedure_v1| /content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']
         */
        DSLContext context = DSLContextHelper.buildContext(ctx -> {
            DSLContext create = DSLContextHelper.buildContext();
            MockResult[] mock = new MockResult[1];
            Result<Record2<String, String>> result = create.newResult(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID);
            //search for leafs
            if (ctx.sql().contains("\"ehr\".\"containment\".\"label\"~ '*.openEHR_EHR_ACTION_immunisation_procedure_v1")) {
                result.add(create
                        .newRecord(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID)
                        .values("/content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']", "openEHR-EHR-COMPOSITION.health_summary.v1"));
                // search for root entry
            } else if (ctx.sql().contains("\"ehr\".\"containment\".\"label\"~ 'openEHR_EHR_COMPOSITION_health_summary_v1")) {
                result.add(create
                        .newRecord(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID)
                        .values("/composition[openEHR-EHR-COMPOSITION.health_summary.v1]", "openEHR-EHR-COMPOSITION.health_summary.v1"));
            }

            mock[0] = new MockResult(1, result);
            return mock;
        });

        PathResolver cut = new PathResolver(context, identifierMapper);
        cut.resolvePaths("openEHR-EHR-COMPOSITION.health_summary.v1", UUID.randomUUID());


        assertThat(cut.pathOf("d")).isEqualTo("/content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']");
        assertThat(cut.pathOf("a")).isEqualTo("/composition[openEHR-EHR-COMPOSITION.health_summary.v1]");

    }
}