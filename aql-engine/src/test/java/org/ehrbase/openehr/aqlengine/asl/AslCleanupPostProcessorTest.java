/*
 * Copyright (c) 2025 vitasystems GmbH.
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.aqlengine.TestAqlQueryContext;
import org.ehrbase.openehr.aqlengine.aql.AqlEhrPathPostProcessor;
import org.ehrbase.openehr.aqlengine.aql.AqlFromEhrOptimisationPostProcessor;
import org.ehrbase.openehr.aqlengine.aql.AqlQueryParsingPostProcessor;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.aqlengine.sql.AqlSqlQueryBuilder;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.util.TestConfig;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AslCleanupPostProcessorTest {

    private final KnowledgeCacheService mockKnowledgeCacheService = mock();
    private final AqlSqlQueryBuilder sqlBuilder = new AqlSqlQueryBuilder(
            TestConfig.aqlConfigurationProperties(),
            new DefaultDSLContext(SQLDialect.POSTGRES),
            mock(),
            Optional.empty());

    @Disabled
    @ParameterizedTest
    @ValueSource(strings = {"SELECT c/uid/value FROM COMPOSITION c"})
    void showAsl(String aqlStr) {
        AslResult result = parseAql(aqlStr);
        String originalAslGraph = AslGraph.createAslGraph(result.aslQuery());
        new AslCleanupPostProcessor().afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());
        System.out.println(modifiedAslGraph);
        assertThat(modifiedAslGraph).isNotEqualTo(originalAslGraph);
    }

    @Test
    void simpleCleanupRegression() {
        AslResult result = parseAql("""
            SELECT c/uid/value FROM COMPOSITION c
        """);
        new AslCleanupPostProcessor().afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());

        assertThat(modifiedAslGraph)
                .isEqualToIgnoringWhitespace(
                        """
        AslRootQuery
          SELECT
            sCO_c_0.?? -- COMPLEX VO_ID uid/value
          FROM
            sCO_c_0: StructureQuery
              SELECT
                sCO_c_0.sCO_c_0_vo_id
                sCO_c_0.sCO_c_0_sys_version
              FROM COMPOSITION
        """);
    }

    @Test
    @Disabled
    void nestedSingleNodePathSubqueryCleanup() {
        AslResult result =
                parseAql("""
            SELECT c/feeder_audit/original_content FROM COMPOSITION c
        """);
        new AslCleanupPostProcessor().afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());

        assertThat(modifiedAslGraph)
                .isEqualToIgnoringWhitespace(
                        """
        AslRootQuery
          SELECT
            p_feeder_audit__0.p_feeder_audit__0_data -> original_content
          FROM
            sCO_c_0: StructureQuery
              SELECT
                sCO_c_0.sCO_c_0_vo_id
                sCO_c_0.sCO_c_0_num
                sCO_c_0.sCO_c_0_ehr_id
              FROM COMPOSITION

            p_feeder_audit__0: StructureQuery
                SELECT
                  p_feeder_audit__0.p_feeder_audit__0_vo_id
                  p_feeder_audit__0.p_feeder_audit__0_parent_num
                  p_feeder_audit__0.p_feeder_audit__0_data
                FROM COMPOSITION
                STRUCTURE CONDITIONS
                  p_feeder_audit__0.p_feeder_audit__0_entity_attribute EQ [f]
                LEFT_OUTER_JOIN sCO_c_0 -> p_feeder_audit__0
                  on
                    DelegatingJoinCondition ->
                        PathChildCondition COMPOSITION sCO_c_0 -> COMPOSITION p_feeder_audit__0
        """);
    }

    @Test
    void cleanupRegression() {
        AslResult result = parseAql(
                """
            SELECT c/uid/value,
                o/data[at0001]/events[at0002]/data[at0003, 'name1']/items[at0004]/value/magnitude,
                o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude,
                el/value/units
            FROM EHR[ehr_id/value='860f5c5b-f121-41bb-ad66-7ac6c285526c']
            CONTAINS EHR_STATUS s
            AND COMPOSITION c
             CONTAINS OBSERVATION o[openEHR-EHR-OBSERVATION.test.v0]
                CONTAINS ELEMENT el[at0120]
            WHERE el/name/value='Result'
            AND el/value/value=1.3
            ORDER BY o/name/value


        """);
        new AslCleanupPostProcessor().afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());

        assertThat(modifiedAslGraph)
                .isEqualToIgnoringWhitespace(
                        """
        AslRootQuery
          SELECT
            sCO_c_0.?? -- COMPLEX VO_ID uid/value
            p_items__0_f_0.p_items__0_f_0_data
            p_items__0_f_1.p_items__0_f_1_data
            sE_el_0.sE_el_0_data -> value -> units
          FROM
            sEHR_0: StructureQuery
              SELECT
                sEHR_0.sEHR_0_id /* ehr_id/value */
              WHERE
                sEHR_0.sEHR_0_id /* ehr_id/value */ EQ [860f5c5b-f121-41bb-ad66-7ac6c285526c]
              FROM EHR
            sES_s_0: StructureQuery
              SELECT
                sES_s_0.sES_s_0_vo_id
                sES_s_0.sES_s_0_ehr_id
              FROM EHR_STATUS
              JOIN sEHR_0 -> sES_s_0
                on
                  DelegatingJoinCondition ->
                      sEHR_0.sEHR_0_id /* ehr_id/value */ EQ sES_s_0.sES_s_0_ehr_id

            sCO_c_0: StructureQuery
              SELECT
                sCO_c_0.sCO_c_0_vo_id
                sCO_c_0.sCO_c_0_ehr_id
                sCO_c_0.sCO_c_0_sys_version
              FROM COMPOSITION
              JOIN sEHR_0 -> sCO_c_0
                on
                  DelegatingJoinCondition ->
                      sEHR_0.sEHR_0_id /* ehr_id/value */ EQ sCO_c_0.sCO_c_0_ehr_id

            sOB_o_0: StructureQuery
              SELECT
                sOB_o_0.sOB_o_0_vo_id
                sOB_o_0.sOB_o_0_num
                sOB_o_0.sOB_o_0_entity_name /* name/value */
              WHERE
                sOB_o_0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=OB, concept=.test.v0]]
              FROM COMPOSITION
              STRUCTURE CONDITIONS
                sOB_o_0.sOB_o_0_rm_entity IN [OB]
              JOIN sCO_c_0 -> sOB_o_0
                on
                  DelegatingJoinCondition ->
                      sCO_c_0.sCO_c_0_vo_id EQ sOB_o_0.sOB_o_0_vo_id

            sE_el_0: StructureQuery
              SELECT
                sE_el_0.sE_el_0_vo_id
                sE_el_0.sE_el_0_citem_num
                sE_el_0.sE_el_0_entity_name /* name/value */
                sE_el_0.sE_el_0_data
              WHERE
                sE_el_0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0120]]
              FROM COMPOSITION
              STRUCTURE CONDITIONS
                sE_el_0.sE_el_0_rm_entity IN [E]
              JOIN sOB_o_0 -> sE_el_0
                on
                  DelegatingJoinCondition ->
                      sOB_o_0.sOB_o_0_vo_id EQ sE_el_0.sE_el_0_vo_id

                  DelegatingJoinCondition ->
                      sOB_o_0.sOB_o_0_num EQ sE_el_0.sE_el_0_citem_num

            p_eq_0: EncapsulatingQuery
              SELECT
                p_data__0.p_data__0_vo_id
                p_data__0.p_data__0_num
                p_data__0.p_data__0_parent_num
                p_events__0.p_events__0_vo_id
                p_events__0.p_events__0_num
                p_events__0.p_events__0_parent_num
                p_data__1.p_data__1_vo_id
                p_data__1.p_data__1_num
                p_data__1.p_data__1_parent_num
                p_data__1.p_data__1_entity_concept
                p_data__1.p_data__1_entity_name /* name/value */
                p_data__1.p_data__1_rm_entity
                p_items__0.p_items__0_vo_id
                p_items__0.p_items__0_parent_num
                p_items__0.p_items__0_data
                FROM
                  p_data__0: StructureQuery
                      SELECT
                        p_data__0.p_data__0_vo_id
                        p_data__0.p_data__0_num
                        p_data__0.p_data__0_parent_num
                      WHERE
                        p_data__0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0001]]
                      FROM COMPOSITION
                      STRUCTURE CONDITIONS
                        p_data__0.p_data__0_entity_attribute EQ [d]

                  p_events__0: StructureQuery
                      SELECT
                        p_events__0.p_events__0_vo_id
                        p_events__0.p_events__0_num
                        p_events__0.p_events__0_parent_num
                      WHERE
                        p_events__0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0002]]
                      FROM COMPOSITION
                      STRUCTURE CONDITIONS
                        p_events__0.p_events__0_entity_attribute EQ [e]
                      JOIN p_data__0 -> p_events__0
                        on
                          DelegatingJoinCondition ->
                              p_data__0.p_data__0_vo_id EQ p_events__0.p_events__0_vo_id

                          DelegatingJoinCondition ->
                              p_data__0.p_data__0_num EQ p_events__0.p_events__0_parent_num


                  p_data__1: StructureQuery
                      SELECT
                        p_data__1.p_data__1_vo_id
                        p_data__1.p_data__1_num
                        p_data__1.p_data__1_parent_num
                        p_data__1.p_data__1_entity_concept
                        p_data__1.p_data__1_entity_name /* name/value */
                        p_data__1.p_data__1_rm_entity
                      WHERE
                        p_data__1.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0003]]
                      FROM COMPOSITION
                      STRUCTURE CONDITIONS
                        p_data__1.p_data__1_entity_attribute EQ [d]
                      JOIN p_events__0 -> p_data__1
                        on
                          DelegatingJoinCondition ->
                              p_events__0.p_events__0_vo_id EQ p_data__1.p_data__1_vo_id

                          DelegatingJoinCondition ->
                              p_events__0.p_events__0_num EQ p_data__1.p_data__1_parent_num


                  p_items__0: StructureQuery
                      SELECT
                        p_items__0.p_items__0_vo_id
                        p_items__0.p_items__0_parent_num
                        p_items__0.p_items__0_data
                      WHERE
                        p_items__0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0004]]
                      FROM COMPOSITION
                      STRUCTURE CONDITIONS
                        p_items__0.p_items__0_entity_attribute EQ [i]
                      JOIN p_data__1 -> p_items__0
                        on
                          PathFilterJoinCondition p_data__1 ->
                              OR
                                AND
                                  p_data__1.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0003]]
                                  p_data__1.p_data__1_entity_name /* name/value */ EQ [name1]
                                p_data__1.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0003]]

                          DelegatingJoinCondition ->
                              p_data__1.p_data__1_vo_id EQ p_items__0.p_items__0_vo_id

                          DelegatingJoinCondition ->
                              p_data__1.p_data__1_num EQ p_items__0.p_items__0_parent_num


              LEFT_OUTER_JOIN sOB_o_0 -> p_eq_0
                on
                  DelegatingJoinCondition ->
                      sOB_o_0.sOB_o_0_vo_id EQ p_data__0.p_data__0_vo_id

                  DelegatingJoinCondition ->
                      sOB_o_0.sOB_o_0_num EQ p_data__0.p_data__0_parent_num

            p_items__0_f_0: FilteringQuery
              SELECT
                p_items__0_f_0.p_items__0_f_0_data
              LEFT_OUTER_JOIN p_eq_0 -> p_items__0_f_0
                on
                  PathFilterJoinCondition p_data__1 ->
                      AND
                        p_eq_0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0003]]
                        p_eq_0.p_data__1_entity_name /* name/value */ EQ [name1]

            p_items__0_f_1: FilteringQuery
              SELECT
                p_items__0_f_1.p_items__0_f_1_data
              LEFT_OUTER_JOIN p_eq_0 -> p_items__0_f_1
                on
                  PathFilterJoinCondition p_data__1 ->
                      p_eq_0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0003]]

          WHERE
            AND
              sE_el_0.sE_el_0_entity_name /* name/value */ EQ [Result]
              sE_el_0.sE_el_0_data -> value -> value EQ [1.3]
          ORDER BY
        sOB_o_0.sOB_o_0_entity_name /* name/value */ ASC
        """);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT c/uid/value FROM COMPOSITION c"})
    void changedAsl(String aqlStr) {
        AslResult result = parseAql(aqlStr);
        String originalAslGraph = AslGraph.createAslGraph(result.aslQuery());
        new AslCleanupPostProcessor().afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());
        assertThat(modifiedAslGraph).isNotEqualTo(originalAslGraph);
        assertThatNoException().isThrownBy(() -> sqlBuilder.buildSqlQuery(result.aslQuery()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    @Disabled
    void aslGraphRegression(String aql, String aslGraph) {
        AslResult result = parseAql(aql);
        new AslCleanupPostProcessor().afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());
        assertThat(modifiedAslGraph).isEqualToIgnoringWhitespace(aslGraph);
    }

    private AslResult parseAql(String aqlStr) {
        AqlQuery aqlQuery = AqlQueryParser.parse(aqlStr);

        for (AqlQueryParsingPostProcessor processor : new AqlQueryParsingPostProcessor[] {
            new AqlEhrPathPostProcessor(), new AqlFromEhrOptimisationPostProcessor()
        }) {
            processor.afterParseAql(aqlQuery, null, null);
        }

        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery, false);
        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(mockKnowledgeCacheService, () -> "node", new TestAqlQueryContext());
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
        return new AslResult(aqlQuery, queryWrapper, aslQuery);
    }

    private record AslResult(AqlQuery aqlQuery, AqlQueryWrapper queryWrapper, AslRootQuery aslQuery) {}

    private static Stream<Arguments> aslGraphRegression() {
        try (InputStream is = AslCleanupPostProcessor.class
                .getClassLoader()
                .getResourceAsStream("aslCleanupProcessor/test_data.txt")) {
            Stream.Builder<Pair<String, String>> sb = Stream.builder();
            StringBuilder currentAqlQuery = new StringBuilder();
            StringBuilder currentAslGraph = new StringBuilder();

            Runnable nextQuery = () -> {
                if (!currentAqlQuery.isEmpty()) {
                    sb.accept(Pair.of(
                            currentAqlQuery.toString().trim(),
                            currentAslGraph.toString().trim()));
                    currentAqlQuery.setLength(0);
                    currentAslGraph.setLength(0);
                }
            };

            boolean inAqlQuery = false;
            for (String l : IOUtils.readLines(is, StandardCharsets.UTF_8)) {
                if (l.startsWith("#")) {
                    nextQuery.run();
                    currentAqlQuery.append(l.substring(1));
                    inAqlQuery = true;
                } else if (l.startsWith("AslRootQuery")) {
                    inAqlQuery = false;
                    currentAslGraph.append(l).append("\n");
                } else if (inAqlQuery) {
                    currentAqlQuery.append(l).append("\n");
                } else {
                    currentAslGraph.append(l).append("\n");
                }
            }

            nextQuery.run();

            return sb.build().map(t -> Arguments.of(t.getLeft(), t.getRight()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
