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
package org.ehrbase.aql.sql.binding;

public class PredicatesBinderTest {
    //
    //    @Test
    //    public void bind() {
    //
    //        // Single containment
    //        {
    //            //represents contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]
    //            ContainmentSet containmentSet = new ContainmentSet(1, null);
    //            containmentSet.add(ContainmentTest.buildContainment(null, "a",
    // "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null));
    //
    //            PredicatesBinder cut = new PredicatesBinder();
    //            Predicates actual = cut.bind(containmentSet);
    //            assertThat(actual.getAtomicPredicates()).size().isEqualTo(1);
    //
    //            Predicates.Details details1 = actual.getAtomicPredicates().get(0);
    //            assertThat(details1.getExpression()).isEqualTo("openEHR_EHR_COMPOSITION_health_summary_v1");
    //            assertThat(details1.getContainedIn()).isNull();
    //            assertThat(details1.getInSet()).isNull();
    //        }
    //
    //        // boolean operators
    //        {
    //
    //            //represents CONTAINS COMPOSITION a [openEHR-EHR-COMPOSITION.health_summary.v1] AND COMPOSITION d
    // [openEHR-EHR-COMPOSITION.referral.v1]
    //            ContainmentSet containmentSet = new ContainmentSet(1, null);
    //            containmentSet.add(ContainmentTest.buildContainment(null, "a",
    // "openEHR-EHR-COMPOSITION.health_summary.v1", "COMPOSITION", null));
    //            containmentSet.add(new ContainOperator("AND").getOperator());
    //            containmentSet.add(ContainmentTest.buildContainment(null, "d", "openEHR-EHR-COMPOSITION.referral.v1",
    // "COMPOSITION", null));
    //
    //            PredicatesBinder cut = new PredicatesBinder();
    //            Predicates actual = cut.bind(containmentSet);
    //            assertThat(actual.getAtomicPredicates()).size().isEqualTo(1);
    //
    //            Predicates.Details details1 = actual.getAtomicPredicates().get(0);
    //            assertThat(details1.getExpression()).isEqualTo("openEHR_EHR_COMPOSITION_referral_v1");
    //            assertThat(details1.getContainedIn()).isNull();
    //            assertThat(details1.getInSet()).isNull();
    //
    //            assertThat(actual.getIntersectPredicates()).size().isEqualTo(1);
    //            Predicates.Details details2 = actual.getIntersectPredicates().get(0);
    //            assertThat(details2.getExpression()).isEqualTo("openEHR_EHR_COMPOSITION_health_summary_v1");
    //            assertThat(details2.getContainedIn()).isNull();
    //            assertThat(details2.getInSet()).isNull();
    //        }
    //    }
}
