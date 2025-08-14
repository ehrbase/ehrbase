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
package org.ehrbase.openehr.aqlengine.asl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.aqlengine.AqlConfigurationProperties;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AqlSqlLayerTest {

    private final KnowledgeCacheService mockKnowledgeCacheService = mock();

    @BeforeEach
    void setUp() {
        Mockito.reset(mockKnowledgeCacheService);
        Mockito.when(mockKnowledgeCacheService.findUuidByTemplateId(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(UUID.randomUUID()));
    }

    @Disabled
    @Test
    void printAslGraph() {
        AslRootQuery aslQuery = buildSqlQuery(
                """
        SELECT
        c/feeder_audit,
        c/uid/value,
        c/context/other_context[at0004]/items[at0014]/value
        FROM EHR e CONTAINS COMPOSITION c
        WHERE e/ehr_id/value = 'e6fad8ba-fb4f-46a2-bf82-66edb43f142f'
        """,
                false);
        System.out.println(AslGraph.createAslGraph(aslQuery));
    }

    @Test
    void testDataQueryPlacedLast() {
        AslRootQuery aslQuery = buildSqlQuery(
                """
            SELECT
            c/content,
            c/content[at0001],
            c[openEHR-EHR-COMPOSITION.test.v0]/content[at0002],
            c/uid/value,
            c/context/other_context[at0004]/items[at0014]/value
            FROM EHR e CONTAINS COMPOSITION c
            WHERE e/ehr_id/value = 'e6fad8ba-fb4f-46a2-bf82-66edb43f142f'
        """,
                false);
        List<AslQuery> queries =
                aslQuery.getChildren().stream().map(Pair::getLeft).toList();

        assertThat(queries).hasSize(4);

        assertThat(queries.get(0)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(1)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(2)).isInstanceOf(AslEncapsulatingQuery.class);
        assertThat(queries.get(3)).isInstanceOf(AslEncapsulatingQuery.class);

        // feeder_audit
        AslField contentField1 = aslQuery.getSelect().get(0);
        AslField contentField2 = aslQuery.getSelect().get(1);
        AslField contentField3 = aslQuery.getSelect().get(2);

        // check select
        assertThat(contentField1).isInstanceOf(AslSubqueryField.class);
        assertThat(((AslSubqueryField) contentField1).getFilterConditions()).isEmpty();
        assertThat(contentField2).isInstanceOf(AslSubqueryField.class);
        assertThat(((AslSubqueryField) contentField2).getFilterConditions()).hasSize(1);
        assertThat(contentField3).isInstanceOf(AslSubqueryField.class);
        assertThat(((AslSubqueryField) contentField3).getFilterConditions()).hasSize(2);

        assertThat(aslQuery.getSelect().getLast()).isInstanceOf(AslRmPathField.class);
    }

    @Test
    void clusterDataSingleSelection() {

        AslRootQuery aslQuery = buildSqlQuery(
                """
        SELECT
            cluster/items[at0001]/value/data
        FROM COMPOSITION CONTAINS CLUSTER cluster[openEHR-EHR-CLUSTER.media_file.v1]
        """,
                false);
        List<AslQuery> queries =
                aslQuery.getChildren().stream().map(Pair::getLeft).toList();

        assertThat(queries).hasSize(3);

        assertThat(queries.get(0)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(1)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(2)).isInstanceOf(AslEncapsulatingQuery.class);
        assertThat(aslQuery.getSelect().getFirst()).isInstanceOf(AslRmPathField.class);
    }

    @Test
    void testPathNodeSkipping() {
        AslRootQuery aslQuery = buildSqlQuery(
                """
        SELECT
            o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[openEHR-EHR-CLUSTER.cl.v0]/items[at0005]/items[at0006]/value,
            o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[openEHR-EHR-CLUSTER.cl.v0]/items[at0005]/items[at0009]/value,
            o/data[at0001]/events[at0002]/state[at0006]/items[at0008]/value,
            o/data[at0001]/events[at0002]/state[at0006]/items[at0007]/value
        FROM OBSERVATION o[openEHR-EHR-OBSERVATION.ooo.v1]
        """,
                true);

        String expected =
                """
                AslRootQuery
                  SELECT
                    p_eq_0.p_items__2_data -> value
                    p_eq_0.p_items__3_data -> value
                    p_eq_0.p_items__4_data -> value
                    p_eq_0.p_items__5_data -> value
                  FROM
                    sOB_o_0: StructureQuery
                      SELECT
                        sOB_o_0.sOB_o_0_vo_id
                        sOB_o_0.sOB_o_0_num
                      WHERE
                        sOB_o_0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=OB, concept=.ooo.v1]]
                      FROM COMPOSITION
                      STRUCTURE CONDITIONS
                        sOB_o_0.sOB_o_0_rm_entity IN [OB]
                    p_eq_0: EncapsulatingQuery
                      SELECT
                        p_events__0.p_events__0_vo_id
                        p_events__0.p_events__0_num
                        p_events__0.p_events__0_num_cap
                        p_events__0.p_events__0_citem_num
                        p_eq_1.p_items__0_vo_id
                        p_eq_1.p_items__0_num
                        p_eq_1.p_items__0_citem_num
                        p_eq_1.p_items__1_vo_id
                        p_eq_1.p_items__1_num
                        p_eq_1.p_items__1_parent_num
                        p_eq_1.p_items__2_vo_id
                        p_eq_1.p_items__2_parent_num
                        p_eq_1.p_items__2_citem_num
                        p_eq_1.p_items__2_data
                        p_eq_1.p_items__3_vo_id
                        p_eq_1.p_items__3_parent_num
                        p_eq_1.p_items__3_citem_num
                        p_eq_1.p_items__3_data
                        p_eq_2.p_items__4_vo_id
                        p_eq_2.p_items__4_num
                        p_eq_2.p_items__4_parent_num
                        p_eq_2.p_items__4_citem_num
                        p_eq_2.p_items__4_data
                        p_eq_3.p_items__5_vo_id
                        p_eq_3.p_items__5_num
                        p_eq_3.p_items__5_parent_num
                        p_eq_3.p_items__5_citem_num
                        p_eq_3.p_items__5_data
                      WHERE
                        OR
                          NOT_NULL p_items__0.p_items__0_vo_id
                          NOT_NULL p_items__4.p_items__4_vo_id
                          NOT_NULL p_items__5.p_items__5_vo_id
                        FROM
                          p_events__0: StructureQuery
                              SELECT
                                p_events__0.p_events__0_vo_id
                                p_events__0.p_events__0_num
                                p_events__0.p_events__0_num_cap
                                p_events__0.p_events__0_citem_num
                              WHERE
                                p_events__0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0002]]
                              FROM COMPOSITION
                              STRUCTURE CONDITIONS
                                p_events__0.p_events__0_entity_attribute EQ [e]

                          p_eq_1: EncapsulatingQuery
                              SELECT
                                p_items__0.p_items__0_vo_id
                                p_items__0.p_items__0_num
                                p_items__0.p_items__0_citem_num
                                p_items__1.p_items__1_vo_id
                                p_items__1.p_items__1_num
                                p_items__1.p_items__1_parent_num
                                p_items__2.p_items__2_vo_id
                                p_items__2.p_items__2_parent_num
                                p_items__2.p_items__2_citem_num
                                p_items__2.p_items__2_data
                                p_items__3.p_items__3_vo_id
                                p_items__3.p_items__3_parent_num
                                p_items__3.p_items__3_citem_num
                                p_items__3.p_items__3_data
                                FROM
                                  p_items__0: StructureQuery
                                      SELECT
                                        p_items__0.p_items__0_vo_id
                                        p_items__0.p_items__0_num
                                        p_items__0.p_items__0_citem_num
                                      WHERE
                                        p_items__0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0004]]
                                      FROM COMPOSITION
                                      STRUCTURE CONDITIONS
                                        p_items__0.p_items__0_entity_attribute EQ [i]

                                  p_items__1: StructureQuery
                                      SELECT
                                        p_items__1.p_items__1_vo_id
                                        p_items__1.p_items__1_num
                                        p_items__1.p_items__1_parent_num
                                      WHERE
                                        p_items__1.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=CL, concept=.cl.v0]]
                                      FROM COMPOSITION
                                      STRUCTURE CONDITIONS
                                        p_items__1.p_items__1_entity_attribute EQ [i]
                                      JOIN p_items__0 -> p_items__1
                                        on
                                          DelegatingJoinCondition ->
                                              p_items__0.p_items__0_vo_id EQ p_items__1.p_items__1_vo_id

                                          DelegatingJoinCondition ->
                                              p_items__0.p_items__0_num EQ p_items__1.p_items__1_parent_num


                                  p_items__2: StructureQuery
                                      SELECT
                                        p_items__2.p_items__2_vo_id
                                        p_items__2.p_items__2_parent_num
                                        p_items__2.p_items__2_citem_num
                                        p_items__2.p_items__2_data
                                      WHERE
                                        p_items__2.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0006]]
                                      FROM COMPOSITION
                                      STRUCTURE CONDITIONS
                                        p_items__2.p_items__2_entity_attribute EQ [i]
                                      JOIN p_items__1 -> p_items__2
                                        on
                                          DelegatingJoinCondition ->
                                              p_items__1.p_items__1_vo_id EQ p_items__2.p_items__2_vo_id

                                          DelegatingJoinCondition ->
                                              p_items__1.p_items__1_num EQ p_items__2.p_items__2_citem_num


                                  p_items__3: StructureQuery
                                      SELECT
                                        p_items__3.p_items__3_vo_id
                                        p_items__3.p_items__3_parent_num
                                        p_items__3.p_items__3_citem_num
                                        p_items__3.p_items__3_data
                                      WHERE
                                        p_items__3.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0009]]
                                      FROM COMPOSITION
                                      STRUCTURE CONDITIONS
                                        p_items__3.p_items__3_entity_attribute EQ [i]
                                      JOIN p_items__1 -> p_items__3
                                        on
                                          DelegatingJoinCondition ->
                                              p_items__1.p_items__1_vo_id EQ p_items__3.p_items__3_vo_id

                                          DelegatingJoinCondition ->
                                              p_items__1.p_items__1_num EQ p_items__3.p_items__3_citem_num

                                          DelegatingJoinCondition ->
                                              COALESCE(p_items__3.p_items__3_parent_num EQ p_items__2.p_items__2_parent_num, true)


                              LEFT_OUTER_JOIN p_events__0 -> p_eq_1
                                on
                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_vo_id EQ p_items__0.p_items__0_vo_id

                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_citem_num EQ p_items__0.p_items__0_citem_num

                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_num LT p_items__0.p_items__0_num

                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_num_cap GT_EQ p_items__0.p_items__0_num


                          p_eq_2: EncapsulatingQuery
                              SELECT
                                p_items__4.p_items__4_vo_id
                                p_items__4.p_items__4_num
                                p_items__4.p_items__4_parent_num
                                p_items__4.p_items__4_citem_num
                                p_items__4.p_items__4_data
                                FROM
                                  p_items__4: StructureQuery
                                      SELECT
                                        p_items__4.p_items__4_vo_id
                                        p_items__4.p_items__4_num
                                        p_items__4.p_items__4_parent_num
                                        p_items__4.p_items__4_citem_num
                                        p_items__4.p_items__4_data
                                      WHERE
                                        p_items__4.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0008]]
                                      FROM COMPOSITION
                                      STRUCTURE CONDITIONS
                                        p_items__4.p_items__4_entity_attribute EQ [i]

                              LEFT_OUTER_JOIN p_events__0 -> p_eq_2
                                on
                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_vo_id EQ p_items__4.p_items__4_vo_id

                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_citem_num EQ p_items__4.p_items__4_citem_num

                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_num LT p_items__4.p_items__4_num

                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_num_cap GT_EQ p_items__4.p_items__4_num


                          p_eq_3: EncapsulatingQuery
                              SELECT
                                p_items__5.p_items__5_vo_id
                                p_items__5.p_items__5_num
                                p_items__5.p_items__5_parent_num
                                p_items__5.p_items__5_citem_num
                                p_items__5.p_items__5_data
                                FROM
                                  p_items__5: StructureQuery
                                      SELECT
                                        p_items__5.p_items__5_vo_id
                                        p_items__5.p_items__5_num
                                        p_items__5.p_items__5_parent_num
                                        p_items__5.p_items__5_citem_num
                                        p_items__5.p_items__5_data
                                      WHERE
                                        p_items__5.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0007]]
                                      FROM COMPOSITION
                                      STRUCTURE CONDITIONS
                                        p_items__5.p_items__5_entity_attribute EQ [i]

                              LEFT_OUTER_JOIN p_events__0 -> p_eq_3
                                on
                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_vo_id EQ p_items__5.p_items__5_vo_id

                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_citem_num EQ p_items__5.p_items__5_citem_num

                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_num LT p_items__5.p_items__5_num

                                  DelegatingJoinCondition ->
                                      p_events__0.p_events__0_num_cap GT_EQ p_items__5.p_items__5_num

                                  DelegatingJoinCondition ->
                                      COALESCE(p_items__5.p_items__5_parent_num EQ p_items__4.p_items__4_parent_num, true)

                      LEFT_OUTER_JOIN sOB_o_0 -> p_eq_0
                        on
                          DelegatingJoinCondition ->
                              sOB_o_0.sOB_o_0_vo_id EQ p_events__0.p_events__0_vo_id

                          DelegatingJoinCondition ->
                              sOB_o_0.sOB_o_0_num EQ p_events__0.p_events__0_citem_num
                """;

        assertThat(AslGraph.createAslGraph(aslQuery)).isEqualToIgnoringNewLines(expected);
    }

    private AslRootQuery buildSqlQuery(String query, final boolean pathSkipping) {

        AqlQuery aqlQuery = AqlQueryParser.parse(query);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery, pathSkipping);

        AqlSqlLayer aqlSqlLayer =
                new AqlSqlLayer(mockKnowledgeCacheService, () -> "node", new AqlConfigurationProperties());
        AslRootQuery aslRootQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
        new AslCleanupPostProcessor().afterBuildAsl(aslRootQuery, aqlQuery, queryWrapper, null);
        return aslRootQuery;
    }
}
