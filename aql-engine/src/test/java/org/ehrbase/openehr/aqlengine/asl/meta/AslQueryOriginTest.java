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
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.junit.jupiter.api.Test;

public class AslQueryOriginTest {

    private static ContainmentClassExpression containmentClassExpression(String type, String alias) {
        ContainmentClassExpression containment = new ContainmentClassExpression();
        containment.setType(type);
        containment.setIdentifier(alias);
        return containment;
    }

    @Test
    void aslQueryOriginWithSingleType() {

        AslTypeOrigin typeOrigin = AslTypeOrigin.ofContainsWrapper(
                new RmContainsWrapper(containmentClassExpression(RmConstants.COMPOSITION, "c")));

        AslQueryOrigin origin = AslQueryOrigin.ofType(typeOrigin);
        assertThat(origin.typeOrigins()).singleElement().isSameAs(typeOrigin);
        assertThat(origin)
                .hasToString("AslQueryOrigin[typeOrigins=[AslRmTypeOrigin[alias=c, rmType=COMPOSITION, fieldPaths=]]]");
    }

    @Test
    void aslQueryOriginWithMultipleTypes() {

        AslTypeOrigin typeOrigin1 = AslTypeOrigin.ofContainsWrapper(
                new RmContainsWrapper(containmentClassExpression(RmConstants.COMPOSITION, "c1")));
        AslTypeOrigin typeOrigin2 = AslTypeOrigin.ofContainsWrapper(
                new RmContainsWrapper(containmentClassExpression(RmConstants.COMPOSITION, "c2")));

        AslQueryOrigin origin = AslQueryOrigin.ofType(typeOrigin1);
        origin.addTypeOrigins(List.of(typeOrigin2));

        assertThat(origin.typeOrigins()).hasSize(2).containsExactly(typeOrigin1, typeOrigin2);
        assertThat(origin)
                .hasToString(
                        "AslQueryOrigin[typeOrigins=[AslRmTypeOrigin[alias=c1, rmType=COMPOSITION, fieldPaths=], AslRmTypeOrigin[alias=c2, rmType=COMPOSITION, fieldPaths=]]]");
    }

    private static List<IdentifiedPath> identifiedPath(String rmType, String alias, String identifier, String path) {
        IdentifiedPath identifiedPath = new IdentifiedPath();
        identifiedPath.setPath(AqlObjectPath.parse(path));
        identifiedPath.setRoot(containmentClassExpression(rmType, alias));
        identifiedPath.setRootPredicate(List.of(new AndOperatorPredicate(List.of(new ComparisonOperatorPredicate(
                AqlObjectPath.parse("archetype_node_id"),
                ComparisonOperatorPredicate.PredicateComparisonOperator.EQ,
                new StringPrimitive(identifier))))));
        return List.of(identifiedPath);
    }

    @Test
    void addPath() {

        AslTypeOrigin typeOrigin = AslTypeOrigin.ofContainsWrapper(
                new RmContainsWrapper(containmentClassExpression(RmConstants.ACTION, "a")));
        AslQueryOrigin origin = AslQueryOrigin.ofType(typeOrigin);
        assertThat(origin.typeOrigins()).singleElement().isSameAs(typeOrigin);

        // adds a new AslTypeOrigin
        origin.addPaths(
                identifiedPath(
                        RmConstants.COMPOSITION,
                        "c",
                        "openEHR-EHR-COMPOSITION.registereintrag.v1",
                        "content[openEHR-EHR-OBSERVATION.blood_pressure.v2]/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value"));

        assertThat(origin.typeOrigins())
                .hasSize(2)
                .last()
                .satisfies(
                        newTypeOrigin -> assertThat(newTypeOrigin)
                                .hasToString(
                                        "AslRmTypeOrigin[alias=c, rmType=COMPOSITION, fieldPaths=c[openEHR-EHR-COMPOSITION.registereintrag.v1]/content[openEHR-EHR-OBSERVATION.blood_pressure.v2]/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value]"));
    }

    @Test
    void addPathToSameTypeAlias() {

        AslTypeOrigin typeOrigin = AslTypeOrigin.ofContainsWrapper(
                new RmContainsWrapper(containmentClassExpression(RmConstants.ACTION, "a")));
        AslQueryOrigin origin = AslQueryOrigin.ofType(typeOrigin);

        // adds a new AslTypeOrigin
        origin.addPaths(identifiedPath(
                RmConstants.COMPOSITION,
                "c",
                "openEHR-EHR-COMPOSITION.registereintrag.v1",
                "content/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value"));

        origin.addPaths(identifiedPath(
                RmConstants.COMPOSITION,
                "c",
                "openEHR-EHR-COMPOSITION.registereintrag.v1",
                "content/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value"));

        assertThat(origin.typeOrigins())
                .hasSize(2)
                .last()
                .satisfies(
                        newTypeOrigin -> assertThat(newTypeOrigin)
                                .hasToString(
                                        "AslRmTypeOrigin[alias=c, rmType=COMPOSITION, fieldPaths=c[openEHR-EHR-COMPOSITION.registereintrag.v1]/content/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value,c[openEHR-EHR-COMPOSITION.registereintrag.v1]/content/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value]"));
    }
}
