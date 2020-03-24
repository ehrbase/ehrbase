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

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.containment.ContainmentSet;
import org.ehrbase.aql.containment.ContainmentTest;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainBinderTest {
    private DSLContext context = DSLContextHelper.buildContext();

    @Test
    public void bind() {
        // Single containment
        {
            //represents contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]
            ContainmentSet containmentSet = new ContainmentSet(1, null);
            containmentSet.add(ContainmentTest.buildContainment(null, "a", "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null));

            ContainBinder cut = new ContainBinder(Arrays.asList(new ContainmentSet[]{containmentSet}));
            String actualString = cut.bind();
            assertThat(actualString).isEqualTo("SELECT DISTINCT comp_id FROM ehr.containment WHERE label ~'openEHR_EHR_COMPOSITION_health_summary_v1'");
            SelectQuery actualQuery = cut.bind(context);
            assertThat(actualQuery.getSQL())
                    .contains("select", ".\"comp_id\" from (select distinct on (\"ehr\".\"containment\".\"comp_id\") \"ehr\".\"containment\".\"comp_id\" from \"ehr\".\"containment\" where (label ~'openEHR_EHR_COMPOSITION_health_summary_v1')) as");
        }
    }

    @Test
    public void labelize() {
        assertThat(ContainBinder.labelize("openEHR-EHR-COMPOSITION.health_summary.v1")).isEqualTo("openEHR_EHR_COMPOSITION_health_summary_v1");
    }


}