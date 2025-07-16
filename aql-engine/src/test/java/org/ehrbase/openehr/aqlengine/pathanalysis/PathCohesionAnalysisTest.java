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

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractStringAssert;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathCohesionAnalysis.PathCohesionTreeNode;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.util.TreeUtils;
import org.junit.jupiter.api.Test;

class PathCohesionAnalysisTest {

    @Test
    void simplePath() {
        var map = byIdentifier(analyzePathCohesion("SELECT c/uid/value FROM COMPOSITION c"));
        assertThat(map).containsOnlyKeys("c");

        PathCohesionTreeNode n = map.get("c");

        assertTreeMatches(n, """
        COMPOSITION
          uid
            value""");
    }

    @Test
    void multiContains() {

        var map = byIdentifier(
                analyzePathCohesion(
                        """
                SELECT c/uid/value, ev/name/value
                FROM EHR e contains COMPOSITION c CONTAINS ( (OBSERVATION o CONTAINS CLUSTER cl) OR EVALUATION ev )
                WHERE cl/name/value = 'Values'
                ORDER BY ev/name/value
                """));

        assertThat(map).containsOnlyKeys("c", "ev", "cl");

        PathCohesionTreeNode n = map.values().stream().iterator().next();

        assertTreeMatches(map.get("c"), """
        COMPOSITION
          uid
            value""");

        assertTreeMatches(map.get("ev"), """
        EVALUATION
          name
            value""");

        assertTreeMatches(map.get("cl"), """
        CLUSTER
          name
            value""");
    }

    @Test
    void simpleWithPredicates() {
        var map = byIdentifier(
                analyzePathCohesion(
                        """

        SELECT
        c/content[at0001]/data/events[at0002, 'Irrelevant']/items[name/value='All Items']/items[openEHR-EHR-CLUSTER.myCluster.v1]/items[openEHR-EHR-ELEMENT.myElement.v1, 'Data']/value
        FROM COMPOSITION c"""));
        assertThat(map).containsOnlyKeys("c");

        PathCohesionTreeNode n = map.get("c");

        assertTreeMatches(
                n,
                """
        COMPOSITION
          content[at0001]
            data
              events[at0002]
                items[name/value='All Items']
                  items[openEHR-EHR-CLUSTER.myCluster.v1]
                    items[openEHR-EHR-ELEMENT.myElement.v1, 'Data']
                      value""");
    }

    @Test
    void containsPredicate() {
        var map = byIdentifier(analyzePathCohesion("SELECT c/uid FROM COMPOSITION c[openEHR-EHR-CLUSTER.myComp.v1]"));
        assertThat(map).containsOnlyKeys("c");

        PathCohesionTreeNode n = map.get("c");

        assertTreeMatches(n, """
        COMPOSITION[openEHR-EHR-CLUSTER.myComp.v1]
          uid""");
    }

    @Test
    void ignoreRootPredicate() {
        var map = byIdentifier(analyzePathCohesion("SELECT c[openEHR-EHR-CLUSTER.myComp.v1]/uid FROM COMPOSITION c"));
        assertThat(map).containsOnlyKeys("c");

        PathCohesionTreeNode n = map.get("c");

        // c[openEHR-EHR-CLUSTER.myComp.v1] is ignored, because it only actas as filter
        assertTreeMatches(n, """
        COMPOSITION
          uid""");
    }

    @Test
    void notMergingRootPredicate() {
        var map = byIdentifier(analyzePathCohesion(
                "SELECT c[name/value='My Comp']/uid FROM COMPOSITION c[openEHR-EHR-CLUSTER.myComp.v1]"));
        assertThat(map).containsOnlyKeys("c");

        PathCohesionTreeNode n = map.get("c");

        // c[name/value='My Comp'] is ignored, because it only acts as filter
        assertTreeMatches(n, """
        COMPOSITION[openEHR-EHR-CLUSTER.myComp.v1]
          uid""");
    }

    @Test
    void simpleAttributes() {
        var map = byIdentifier(
                analyzePathCohesion(
                        """
        SELECT
          t/items[at0004]/name/value AS SystolicName,
          t/items[at0004]/value/magnitude AS SystolicValue
        FROM OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
        CONTAINS ITEM_TREE t"""));

        assertTreeMatches(
                map.get("t"),
                """
        ITEM_TREE
          items[at0004]
            name
              value
            value
              magnitude""");
    }

    @Test
    void simpleNodeAttributes() {
        var map = byIdentifier(
                analyzePathCohesion(
                        """
        SELECT
          t/items[at0004]/name/value AS SystolicName,
          t/items[at0004]/value/magnitude AS SystolicValue,
          t/items[at0004]/value/units AS SystolicUnit,
          t/items[at0005]/name/value AS DiastolicName,
          t/items[at0005]/value/magnitude AS DiastolicValue
        FROM OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
        CONTAINS ITEM_TREE t"""));

        assertTreeMatches(
                map.get("t"),
                """
        ITEM_TREE
          items[at0004]
            name
              value
            value
              magnitude
              units
          items[at0005]
            name
              value
            value
              magnitude""");
    }

    @Test
    void baseAttributes() {
        var map = byIdentifier(
                analyzePathCohesion(
                        """
        SELECT
          t/items/name/value AS Name,
          t/items/value/magnitude AS Value,
          t/items[at0005]/name/value AS DiastolicName,
          t/items[at0005]/value/magnitude AS DiastolicValue,
          t/items[at0005]/value/units AS DiastolicUnit
        FROM OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
        CONTAINS ITEM_TREE t"""));

        assertTreeMatches(
                map.get("t"),
                """
        ITEM_TREE
          items
            name
              value
            value
              magnitude
              units""");
    }

    @Test
    void archetypeAttributes() {
        var map = byIdentifier(
                analyzePathCohesion(
                        """
        SELECT
          t/items[openEHR-EHR-ELEMENT.blood_pressure.v2]/name/value AS Name,
          t/items[openEHR-EHR-ELEMENT.blood_pressure.v2, 'Systolic']/value/magnitude AS SystolicValue,
          t/items[at0005, 'Diastolic']/name/value AS DiastolicName,
          t/items[at0005]/value/magnitude AS DiastolicValue,
          t/items[at0005]/value/units AS DiastolicUnit
        FROM OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
        CONTAINS ITEM_TREE t"""));

        assertTreeMatches(
                map.get("t"),
                """
        ITEM_TREE
          items[at0005]
            name
              value
            value
              magnitude
              units
          items[openEHR-EHR-ELEMENT.blood_pressure.v2]
            name
              value
            value
              magnitude""");
    }

    @Test
    void nameAttributes() {
        var map = byIdentifier(
                analyzePathCohesion(
                        """
        SELECT
          t/items[name/value='Systolic']/name/value AS Name,
          t/items[name/value='Systolic']/value/magnitude AS SystolicValue,
          t/items[name/value='Diastolic']/name/value AS DiastolicName,
          t/items[name/value='Diastolic']/value/magnitude AS DiastolicValue,
          t/items[name/value='Diastolic']/value/units AS DiastolicUnit
        FROM OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
        CONTAINS ITEM_TREE t"""));

        assertTreeMatches(
                map.get("t"),
                """
        ITEM_TREE
          items[name/value='Diastolic']
            name
              value
            value
              magnitude
              units
          items[name/value='Systolic']
            name
              value
            value
              magnitude""");
    }

    @Test
    void mixedAttributes() {
        var map = byIdentifier(
                analyzePathCohesion(
                        """
        SELECT
          t/items[openEHR-EHR-ELEMENT.blood_pressure.v2]/name/value AS Name,
          t/items[openEHR-EHR-ELEMENT.blood_pressure.v2, 'Systolic']/value/magnitude AS SystolicValue,
          t/items[name/value='Diastolic']/name/value AS DiastolicName,
          t/items[at0005]/value/magnitude AS DiastolicValue,
          t/items[at0005]/value/units AS DiastolicUnit
        FROM OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
        CONTAINS ITEM_TREE t"""));

        assertTreeMatches(
                map.get("t"),
                """
        ITEM_TREE
          items
            name
              value
            value
              magnitude
              units""");
    }

    @Test
    void irrelevantPredicates() {
        var map = byIdentifier(
                analyzePathCohesion(
                        """
        SELECT
          t/items[archetype_node_id=at0004 and value/magnitude > 3 and name/value='Systolic']/name/value AS SystolicName,
          t/items[at0004]/value/magnitude AS SystolicValue
        FROM OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
        CONTAINS ITEM_TREE t"""));

        assertTreeMatches(
                map.get("t"),
                """
        ITEM_TREE
          items[at0004]
            name
              value
            value
              magnitude""");
    }

    private static Map<AbstractContainmentExpression, PathCohesionTreeNode> analyzePathCohesion(String aqlStr) {
        return PathCohesionAnalysis.analyzePathCohesion(AqlQuery.parse(aqlStr));
    }

    private static Map<String, PathCohesionTreeNode> byIdentifier(
            Map<AbstractContainmentExpression, PathCohesionTreeNode> map) {
        return map.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getIdentifier(), Map.Entry::getValue));
    }

    private static String renderTree(PathCohesionTreeNode node) {
        return TreeUtils.renderTree(
                node,
                Comparator.comparing(n -> new AqlObjectPath(n.getAttribute()).render()),
                n -> new AqlObjectPath(n.getAttribute()).render());
    }

    private static AbstractStringAssert<?> assertTreeMatches(PathCohesionTreeNode root, String expected) {
        return assertThat(renderTree(root)).isEqualTo(expected);
    }
}
