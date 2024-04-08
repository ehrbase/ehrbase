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

import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.junit.jupiter.api.Test;

class ANodeTest {
    @Test
    void testNodeCategories() {

        // POINT_EVENT with ITEM_SINGLE data with ELEMENT item
        // with DV_SCALE or DV_ORDINAL value (as its value is a number)
        {
            ANode rootNode = PathAnalysis.analyzeAqlPathTypes(
                    "POINT_EVENT", null, null, AqlObjectPath.parse("data/item/value[value>=0]/value"), null);

            ANode node = rootNode;
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("data");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("item");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("value");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.RM_TYPE);
            node = node.attributes.get("value");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.FOUNDATION);
        }

        // POINT_EVENT with ITEM_STRUCTURE data with ELEMENT or CLUSTER items
        {
            ANode rootNode = PathAnalysis.analyzeAqlPathTypes(
                    "POINT_EVENT", null, null, AqlObjectPath.parse("data/items/name[value!='foo']/value"), null);

            ANode node = rootNode;
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("data");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("items");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("name");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.RM_TYPE);
            node = node.attributes.get("value");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.FOUNDATION);
        }

        // POINT_EVENT with ITEM_STRUCTURE data with CLUSTER with ELEMENT
        {
            ANode rootNode = PathAnalysis.analyzeAqlPathTypes(
                    "POINT_EVENT", null, null, AqlObjectPath.parse("data/items/items/name[value!='foo']/value"), null);

            ANode node = rootNode;
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("data");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("items");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("items");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
        }

        // ACTION with INSTRUCTION_DETAILS instruction_details with ITEM_STRUCTURE wf_details
        {
            ANode rootNode = PathAnalysis.analyzeAqlPathTypes(
                    "CARE_ENTRY", null, null, AqlObjectPath.parse("instruction_details/wf_details"), null);

            ANode node = rootNode;
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("instruction_details");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE_INTERMEDIATE);
            node = node.attributes.get("wf_details");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
        }

        //  with ITEM with
        {
            ANode rootNode = PathAnalysis.analyzeAqlPathTypes(
                    "CARE_ENTRY", null, null, AqlObjectPath.parse("instruction_details/wf_details"), null);

            ANode node = rootNode;
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
            node = node.attributes.get("instruction_details");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE_INTERMEDIATE);
            node = node.attributes.get("wf_details");
            assertThat(node.getCategories()).containsExactly(ANode.NodeCategory.STRUCTURE);
        }
    }
}
