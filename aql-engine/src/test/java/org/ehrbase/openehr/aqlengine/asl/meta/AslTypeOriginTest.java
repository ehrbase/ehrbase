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
import org.ehrbase.openehr.aqlengine.querywrapper.contains.RmContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.VersionContainsWrapper;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.junit.jupiter.api.Test;

public class AslTypeOriginTest {

    @Test
    void ofAslRmType() {

        ContainmentClassExpression containment = new ContainmentClassExpression();
        containment.setType(RmConstants.COMPOSITION);
        containment.setIdentifier("compo");

        AslTypeOrigin origin = AslTypeOrigin.ofContainsWrapper(new RmContainsWrapper(containment));
        assertThat(origin.getAlias()).isEqualTo("compo");
        assertThat(origin.getRmType()).isEqualTo("COMPOSITION");
        assertThat(origin.getFieldPaths()).isEmpty();
        assertThat(origin).hasToString("AslRmTypeOrigin[alias=compo, rmType=COMPOSITION, fieldPaths=]");
    }

    @Test
    void ofAslRmTypeWithPredicate() {

        ContainmentClassExpression containment = new ContainmentClassExpression();
        containment.setType(RmConstants.COMPOSITION);
        containment.setIdentifier("c");
        containment.setPredicates(List.of(new AndOperatorPredicate(List.of(new ComparisonOperatorPredicate(
                AqlObjectPath.parse("archetype_node_id"),
                ComparisonOperatorPredicate.PredicateComparisonOperator.EQ,
                new StringPrimitive("openEHR-EHR-COMPOSITION.some.identifier.v0"))))));

        AslTypeOrigin origin = AslTypeOrigin.ofContainsWrapper(new RmContainsWrapper(containment));
        assertThat(origin.getAlias()).isEqualTo("c");
        assertThat(origin.getRmType()).isEqualTo("COMPOSITION");
        assertThat(origin.getFieldPaths()).singleElement().satisfies(identifiedPath -> assertThat(
                        identifiedPath.render())
                .isEqualTo("c[openEHR-EHR-COMPOSITION.some.identifier.v0]/archetype_node_id"));
        assertThat(origin)
                .hasToString(
                        "AslRmTypeOrigin[alias=c, rmType=COMPOSITION, fieldPaths=c[openEHR-EHR-COMPOSITION.some.identifier.v0]/archetype_node_id]");
    }

    @Test
    void ofAslVersion() {

        ContainmentClassExpression containment = new ContainmentClassExpression();
        containment.setType(RmConstants.COMPOSITION);
        containment.setIdentifier("compo");

        AslTypeOrigin origin =
                AslTypeOrigin.ofContainsWrapper(new VersionContainsWrapper("vc", new RmContainsWrapper(containment)));
        assertThat(origin.getAlias()).isEqualTo("vc");
        assertThat(origin.getRmType()).isEqualTo("ORIGINAL_VERSION");
        assertThat(origin.getFieldPaths()).isEmpty();
        assertThat(origin).hasToString("AslVersionTypeOrigin[alias=vc, rmType=ORIGINAL_VERSION, fieldPaths=]");
    }

    @Test
    void ofAslVersionWithPredicate() {

        ContainmentClassExpression containment = new ContainmentClassExpression();
        containment.setType(RmConstants.COMPOSITION);
        containment.setIdentifier("c");
        containment.setPredicates(List.of(new AndOperatorPredicate(List.of(new ComparisonOperatorPredicate(
                AqlObjectPath.parse("archetype_node_id"),
                ComparisonOperatorPredicate.PredicateComparisonOperator.EQ,
                new StringPrimitive("openEHR-EHR-COMPOSITION.other.identifier.v0"))))));

        AslTypeOrigin origin =
                AslTypeOrigin.ofContainsWrapper(new VersionContainsWrapper("vc", new RmContainsWrapper(containment)));

        assertThat(origin).isInstanceOf(AslTypeOrigin.AslVersionTypeOrigin.class);
        assertThat(origin.getAlias()).isEqualTo("vc");
        assertThat(origin.getRmType()).isEqualTo("ORIGINAL_VERSION");
        assertThat(origin.getFieldPaths()).singleElement().satisfies(identifiedPath -> assertThat(
                        identifiedPath.render())
                .isEqualTo("c[openEHR-EHR-COMPOSITION.other.identifier.v0]/archetype_node_id"));
        assertThat(origin)
                .hasToString(
                        "AslVersionTypeOrigin[alias=vc, rmType=ORIGINAL_VERSION, fieldPaths=c[openEHR-EHR-COMPOSITION.other.identifier.v0]/archetype_node_id]");
    }
}
