/*
 * Copyright (c) 2019-2025 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine.asl.meta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.junit.jupiter.api.Test;

public class AslFieldOriginTest {

    @Test
    void fieldOrigin() {

        ContainmentClassExpression containmentClassExpression = new ContainmentClassExpression();
        containmentClassExpression.setType(RmConstants.COMPOSITION);
        containmentClassExpression.setIdentifier("compo");

        IdentifiedPath identifiedPath = new IdentifiedPath();
        identifiedPath.setPath(
                AqlObjectPath.parse(
                        "content[openEHR-EHR-OBSERVATION.blood_pressure.v2]/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value"));
        identifiedPath.setRoot(containmentClassExpression);
        identifiedPath.setRootPredicate(List.of(new AndOperatorPredicate(List.of(new ComparisonOperatorPredicate(
                AqlObjectPath.parse("archetype_node_id"),
                ComparisonOperatorPredicate.PredicateComparisonOperator.EQ,
                new StringPrimitive("openEHR-EHR-COMPOSITION.registereintrag.v1"))))));

        AslFieldOrigin origin = AslFieldOrigin.of(identifiedPath);
        System.out.println(origin);
        assertThat(origin)
                .hasToString(
                        "AslFieldOrigin[compo[openEHR-EHR-COMPOSITION.registereintrag.v1]/content[openEHR-EHR-OBSERVATION.blood_pressure.v2]/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value]");
    }
}
