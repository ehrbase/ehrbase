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
package org.ehrbase.openehr.aqlengine.pathanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.ehrbase.openehr.dbformat.RmAttribute;
import org.ehrbase.openehr.sdk.aql.dto.operand.LongPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPathUtil;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.junit.jupiter.api.Test;

class PathAnalysisTest {

    @Test
    void compositionTypes() {
        assertThat(PathAnalysis.AttributeInfos.rmTypes).isNotEmpty();
    }

    @Test
    void baseTypesByAttribute() {

        Map<String, Set<String>> cut = PathAnalysis.AttributeInfos.baseTypesByAttribute;

        assertThat(cut).isNotEmpty();
        assertThat(cut).containsKey("other_participations");

        assertThat(cut)
                .containsEntry(
                        "other_participations",
                        Set.of(
                                "CARE_ENTRY",
                                "ADMIN_ENTRY",
                                "INSTRUCTION",
                                "OBSERVATION",
                                "ENTRY",
                                "ACTION",
                                "EVALUATION"));
    }

    @Test
    void analyzeAqlPathInvalid() {
        assertThatThrownBy(() -> {
                    ANode node = PathAnalysis.analyzeAqlPathTypes(
                            RmConstants.COMPOSITION,
                            null,
                            null,
                            AqlObjectPath.parse("path/links/non/existent/attributes"),
                            null);

                    // Map<ANode, Map<String, PathAnalysisUtil.AttInfo>> attributeInfos =
                    // PathAnalysisUtil.createAttributeInfos(node);

                })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(": non");
    }

    @Test
    void analyzeAqlPath() {

        // simple composition
        {
            ANode node = PathAnalysis.analyzeAqlPathTypes(RmConstants.COMPOSITION, null, null, null, null);
            assertThat(node.candidateTypes).containsExactly(RmConstants.COMPOSITION);
            assertThat(node.attributes).isEmpty();
        }

        // simple composition
        {
            ANode node = PathAnalysis.analyzeAqlPathTypes(
                    "CARE_ENTRY",
                    archetypeNodeIdCondition("openEHR-EHR-OBSERVATION.my-observation.v3"),
                    null,
                    null,
                    null);
            assertThat(node.candidateTypes).containsExactly(RmConstants.OBSERVATION);
            assertThat(node.attributes).containsOnlyKeys("archetype_node_id");
        }

        // CARE_ENTRY with state
        {
            ANode node = PathAnalysis.analyzeAqlPathTypes("CARE_ENTRY", null, null, AqlObjectPath.parse("state"), null);
            assertThat(node.candidateTypes).containsExactly(RmConstants.OBSERVATION);
            assertThat(node.attributes).containsOnlyKeys("state");
        }

        // CARE_ENTRY with data
        {
            ANode node = PathAnalysis.analyzeAqlPathTypes("CARE_ENTRY", null, null, AqlObjectPath.parse("data"), null);
            assertThat(node.candidateTypes).containsExactly(RmConstants.OBSERVATION, RmConstants.EVALUATION);
            assertThat(node.attributes).containsOnlyKeys("data");
        }

        // CARE_ENTRY with data with items; SELECT c/data/events/state FROM CARE_ENTRY c
        {
            ANode node = PathAnalysis.analyzeAqlPathTypes(
                    "CARE_ENTRY", null, null, AqlObjectPath.parse("data/events/state"), null);
            assertThat(node.candidateTypes).containsExactly(RmConstants.OBSERVATION);
            assertThat(node.attributes).containsOnlyKeys("data");
        }

        // ITEM_SINGLE with one element; SELECT s/item/value FROM ITEM_STRUCTURE s
        {
            ANode node = PathAnalysis.analyzeAqlPathTypes(
                    "ITEM_STRUCTURE", null, null, AqlObjectPath.parse("item/value"), null);
            assertThat(node.candidateTypes).containsExactly(RmConstants.ITEM_SINGLE);
            assertThat(node.attributes).containsOnlyKeys("item");

            ANode item = node.attributes.get("item");
            assertThat(item.candidateTypes).containsExactly(RmConstants.ELEMENT);

            assertThat(item.attributes).containsOnlyKeys("value");
            ANode elementValue = item.attributes.get("value");
            assertThat(elementValue.candidateTypes).isNotEmpty().allMatch(v -> v.startsWith("DV_"));
        }

        // ITEM_SINGLE with one element with DvCodedText value
        {
            ANode node = PathAnalysis.analyzeAqlPathTypes(
                    "ITEM_STRUCTURE",
                    null,
                    null,
                    AqlObjectPath.parse("item/value[defining_code/terminology_id/value='openehr']/value"),
                    null);
            assertThat(node.candidateTypes).containsExactly(RmConstants.ITEM_SINGLE);
            assertThat(node.attributes).containsOnlyKeys("item");

            ANode item = node.attributes.get("item");
            assertThat(item.candidateTypes).containsExactly(RmConstants.ELEMENT);

            assertThat(item.attributes).containsOnlyKeys("value");
            ANode elementValue = item.attributes.get("value");
            assertThat(elementValue.candidateTypes).allMatch(v -> v.startsWith("DV_"));

            assertThat(elementValue.attributes).containsOnlyKeys("value", "defining_code");
            ANode valueValue = elementValue.attributes.get("value");
            assertThat(valueValue.candidateTypes).containsExactly(FoundationType.STRING.name());
        }

        // ITEM_SINGLE with one element, type constrained via predicate value
        {
            ANode node = PathAnalysis.analyzeAqlPathTypes(
                    "ITEM_STRUCTURE", null, null, AqlObjectPath.parse("item/value[value=10.0]/value"), null);
            assertThat(node.candidateTypes).containsExactly(RmConstants.ITEM_SINGLE);
            assertThat(node.attributes).containsOnlyKeys("item");

            ANode item = node.attributes.get("item");
            assertThat(item.candidateTypes).containsExactly(RmConstants.ELEMENT);

            assertThat(item.attributes).containsOnlyKeys("value");
            ANode elementValue = item.attributes.get("value");
            assertThat(elementValue.candidateTypes).containsExactly(RmConstants.DV_SCALE, RmConstants.DV_ORDINAL);
        }

        // ITEM_SINGLE with one element, type constrained via value
        {
            Set<String> candidateTypes = PathAnalysis.getCandidateTypes(new LongPrimitive(10L));

            ANode node = PathAnalysis.analyzeAqlPathTypes(
                    "ITEM_STRUCTURE", null, null, AqlObjectPath.parse("item/value/value"), candidateTypes);
            assertThat(node.candidateTypes).containsExactly(RmConstants.ITEM_SINGLE);
            assertThat(node.attributes).containsOnlyKeys("item");

            ANode item = node.attributes.get("item");
            assertThat(item.candidateTypes).containsExactly(RmConstants.ELEMENT);

            assertThat(item.attributes).containsOnlyKeys("value");
            ANode elementValue = item.attributes.get("value");
            assertThat(elementValue.candidateTypes).containsExactly(RmConstants.DV_SCALE, RmConstants.DV_ORDINAL);
        }
    }

    private static List<AndOperatorPredicate> archetypeNodeIdCondition(String archetypeNodeId) {
        if (archetypeNodeId == null) {
            return null;
        }

        return new ArrayList<>(List.of(new AndOperatorPredicate(List.of(new ComparisonOperatorPredicate(
                AqlObjectPathUtil.ARCHETYPE_NODE_ID,
                ComparisonOperatorPredicate.PredicateComparisonOperator.EQ,
                new StringPrimitive(archetypeNodeId))))));
    }

    @Test
    void createAttributeInfos() {
        {
            ANode rootNode = PathAnalysis.analyzeAqlPathTypes(
                    "ITEM_STRUCTURE",
                    null,
                    null,
                    AqlObjectPath.parse("item/value/value"),
                    PathAnalysis.getCandidateTypes(new LongPrimitive(10L)));

            Map<ANode, Map<String, PathAnalysis.AttInfo>> attributeInfos = PathAnalysis.createAttributeInfos(rootNode);
            assertThat(attributeInfos).hasSize(3);
            assertThat(attributeInfos.values()).map(Map::size).allMatch(i -> i == 1);
        }

        {
            ANode rootNode = PathAnalysis.analyzeAqlPathTypes(
                    "ITEM_STRUCTURE",
                    null,
                    null,
                    AqlObjectPath.parse("item[value/value>3]/value[value < 100]/value"),
                    PathAnalysis.getCandidateTypes(new LongPrimitive(10L)));

            Map<ANode, Map<String, PathAnalysis.AttInfo>> attributeInfos = PathAnalysis.createAttributeInfos(rootNode);
            assertThat(attributeInfos).hasSize(3);
            assertThat(attributeInfos.values()).map(Map::size).allMatch(i -> i == 1);
        }

        {
            ANode rootNode = PathAnalysis.analyzeAqlPathTypes(
                    "ITEM_STRUCTURE",
                    null,
                    null,
                    AqlObjectPath.parse("item[name/value='My Item']/value[value < 100]/value"),
                    PathAnalysis.getCandidateTypes(new LongPrimitive(10L)));

            Map<ANode, Map<String, PathAnalysis.AttInfo>> attributeInfos = PathAnalysis.createAttributeInfos(rootNode);
            assertThat(attributeInfos).hasSize(4);
            assertThat(attributeInfos.values()).flatExtracting(Map::values).hasSize(5);
        }
    }

    @Test
    void testRmAttributeAlias() {

        List<String> synthetic = List.of("_magnitude", "_type", "_index");
        List<String> rmAttributes = RmAttribute.VALUES.stream()
                .map(RmAttribute::attribute)
                .filter(s -> !synthetic.contains(s))
                .collect(Collectors.toList());
        // EHR-only
        rmAttributes.addAll(List.of("timeCreated", "ehrId", "ehrStatus", "compositions"));

        assertThat(PathAnalysis.AttributeInfos.attributeInfos.keySet()).containsAll(rmAttributes);

        assertThat(rmAttributes).containsAll(PathAnalysis.AttributeInfos.attributeInfos.keySet());
    }
}
