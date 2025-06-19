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
import org.ehrbase.openehr.aqlengine.AqlEhrPathPostProcessor;
import org.ehrbase.openehr.aqlengine.AqlFromEhrOptimisationPostProcessor;
import org.ehrbase.openehr.aqlengine.AqlQueryParsingPostProcessor;
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
    @ValueSource(
            strings = {
                    "SELECT c/uid/value FROM COMPOSITION c"
            })
    void showAsl(String aqlStr) {
        AslResult result = parseAql(aqlStr);
        String originalAslGraph = AslGraph.createAslGraph(result.aslQuery());
        new AslCleanupPostProcessor()
                .afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());
        System.out.println(modifiedAslGraph);
        assertThat(modifiedAslGraph).isNotEqualTo(originalAslGraph);
    }

    @Test
    void simpleCleanupRegression() {
        AslResult result = parseAql("""
            SELECT c/uid/value FROM COMPOSITION c
        """);
        new AslCleanupPostProcessor()
                .afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());

        assertThat(modifiedAslGraph).isEqualToIgnoringWhitespace("""
        AslRootQuery
          SELECT
            sCO_c_0.?? -- COMPLEX VO_ID uid/value
          FROM
            sCO_c_0: StructureQuery
              SELECT
                sCO_c_0.sCO_c_0_vo_id
                sCO_c_0.sCO_c_0_num
                sCO_c_0.sCO_c_0_sys_version
              WHERE
                sCO_c_0.sCO_c_0_num EQ [0]
              FROM COMPOSITION
        """);
    }

    @Test
    void cleanupRegression() {
        AslResult result = parseAql("""
            SELECT c/uid/value,
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
        new AslCleanupPostProcessor()
                .afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());

        assertThat(modifiedAslGraph).isEqualToIgnoringWhitespace("""
AslRootQuery
  SELECT
    sCO_c_0.?? -- COMPLEX VO_ID uid/value
    p_eq_0.p_items__0_data -> value -> magnitude
    sE_el_0.sE_el_0_data -> value -> units
  FROM
    sEHR_0: StructureQuery
      SELECT
        sEHR_0.sEHR_0_id -- ehr_id/value
        sEHR_0.sEHR_0_creation_date
      WHERE
        sEHR_0.sEHR_0_id -- ehr_id/value EQ [860f5c5b-f121-41bb-ad66-7ac6c285526c]
      FROM EHR
    sES_s_0: StructureQuery
      SELECT
        sES_s_0.sES_s_0_vo_id
        sES_s_0.sES_s_0_num
        sES_s_0.sES_s_0_num_cap
        sES_s_0.sES_s_0_parent_num
        sES_s_0.sES_s_0_ehr_id
        sES_s_0.sES_s_0_entity_idx
        sES_s_0.sES_s_0_entity_idx_len
        sES_s_0.sES_s_0_entity_concept
        sES_s_0.sES_s_0_entity_name -- name/value
        sES_s_0.sES_s_0_rm_entity
        sES_s_0.sES_s_0_data
        sES_s_0.sES_s_0_sys_version
        sES_s_0.sES_s_0_audit_id
        sES_s_0.sES_s_0_contribution_id
        sES_s_0.sES_s_0_sys_period_lower
      WHERE
        sES_s_0.sES_s_0_num EQ [0]
      FROM EHR_STATUS
      JOIN sEHR_0 -> sES_s_0
        on
          DelegatingJoinCondition sEHR_0 ->
              DescendantCondition EHR sEHR_0 -> EHR_STATUS sES_s_0

    sCO_c_0: StructureQuery
      SELECT
        sCO_c_0.sCO_c_0_vo_id
        sCO_c_0.sCO_c_0_num
        sCO_c_0.sCO_c_0_num_cap
        sCO_c_0.sCO_c_0_ehr_id
        sCO_c_0.sCO_c_0_entity_concept
        sCO_c_0.sCO_c_0_rm_entity
        sCO_c_0.sCO_c_0_sys_version
      WHERE
        sCO_c_0.sCO_c_0_num EQ [0]
      FROM COMPOSITION
      JOIN sEHR_0 -> sCO_c_0
        on
          DelegatingJoinCondition sEHR_0 ->
              DescendantCondition EHR sEHR_0 -> COMPOSITION sCO_c_0

    sOB_o_0: StructureQuery
      SELECT
        sOB_o_0.sOB_o_0_vo_id
        sOB_o_0.sOB_o_0_num
        sOB_o_0.sOB_o_0_num_cap
        sOB_o_0.sOB_o_0_parent_num
        sOB_o_0.sOB_o_0_entity_idx
        sOB_o_0.sOB_o_0_entity_idx_len
        sOB_o_0.sOB_o_0_entity_concept
        sOB_o_0.sOB_o_0_entity_name -- name/value
        sOB_o_0.sOB_o_0_rm_entity
        sOB_o_0.sOB_o_0_data
      WHERE
        sOB_o_0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=OB, concept=.test.v0]]
      FROM COMPOSITION
      STRUCTURE CONDITIONS
        sOB_o_0.sOB_o_0_rm_entity IN [OB]
      JOIN sCO_c_0 -> sOB_o_0
        on
          DelegatingJoinCondition sCO_c_0 ->
              DescendantCondition COMPOSITION sCO_c_0 -> COMPOSITION sOB_o_0

    sE_el_0: StructureQuery
      SELECT
        sE_el_0.sE_el_0_vo_id
        sE_el_0.sE_el_0_num
        sE_el_0.sE_el_0_num_cap
        sE_el_0.sE_el_0_parent_num
        sE_el_0.sE_el_0_entity_idx
        sE_el_0.sE_el_0_entity_idx_len
        sE_el_0.sE_el_0_entity_concept
        sE_el_0.sE_el_0_entity_name -- name/value
        sE_el_0.sE_el_0_rm_entity
        sE_el_0.sE_el_0_data
      WHERE
        sE_el_0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0120]]
      FROM COMPOSITION
      STRUCTURE CONDITIONS
        sE_el_0.sE_el_0_rm_entity IN [E]
      JOIN sOB_o_0 -> sE_el_0
        on
          DelegatingJoinCondition sOB_o_0 ->
              DescendantCondition COMPOSITION sOB_o_0 -> COMPOSITION sE_el_0

    p_eq_0: EncapsulatingQuery
      SELECT
        p_data__0.p_data__0_vo_id
        p_data__0.p_data__0_num
        p_data__0.p_data__0_num_cap
        p_data__0.p_data__0_parent_num
        p_data__0.p_data__0_entity_idx
        p_data__0.p_data__0_entity_idx_len
        p_data__0.p_data__0_entity_concept
        p_data__0.p_data__0_entity_name -- name/value
        p_data__0.p_data__0_rm_entity
        p_data__0.p_data__0_data
        p_data__0.p_data__0_entity_attribute
        p_events__0.p_events__0_vo_id
        p_events__0.p_events__0_num
        p_events__0.p_events__0_num_cap
        p_events__0.p_events__0_parent_num
        p_events__0.p_events__0_entity_idx
        p_events__0.p_events__0_entity_idx_len
        p_events__0.p_events__0_entity_concept
        p_events__0.p_events__0_entity_name -- name/value
        p_events__0.p_events__0_rm_entity
        p_events__0.p_events__0_data
        p_events__0.p_events__0_entity_attribute
        p_data__1.p_data__1_vo_id
        p_data__1.p_data__1_num
        p_data__1.p_data__1_num_cap
        p_data__1.p_data__1_parent_num
        p_data__1.p_data__1_entity_idx
        p_data__1.p_data__1_entity_idx_len
        p_data__1.p_data__1_entity_concept
        p_data__1.p_data__1_entity_name -- name/value
        p_data__1.p_data__1_rm_entity
        p_data__1.p_data__1_data
        p_data__1.p_data__1_entity_attribute
        p_items__0.p_items__0_vo_id
        p_items__0.p_items__0_num
        p_items__0.p_items__0_num_cap
        p_items__0.p_items__0_parent_num
        p_items__0.p_items__0_entity_idx
        p_items__0.p_items__0_entity_idx_len
        p_items__0.p_items__0_entity_concept
        p_items__0.p_items__0_entity_name -- name/value
        p_items__0.p_items__0_rm_entity
        p_items__0.p_items__0_data
        p_items__0.p_items__0_entity_attribute
        FROM
          p_data__0: StructureQuery
              SELECT
                p_data__0.p_data__0_vo_id
                p_data__0.p_data__0_num
                p_data__0.p_data__0_num_cap
                p_data__0.p_data__0_parent_num
                p_data__0.p_data__0_entity_idx
                p_data__0.p_data__0_entity_idx_len
                p_data__0.p_data__0_entity_concept
                p_data__0.p_data__0_entity_name -- name/value
                p_data__0.p_data__0_rm_entity
                p_data__0.p_data__0_data
                p_data__0.p_data__0_entity_attribute
              WHERE
                p_data__0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0001]]
              FROM COMPOSITION
              STRUCTURE CONDITIONS
                p_data__0.p_data__0_entity_attribute EQ [d]

          p_events__0: StructureQuery
              SELECT
                p_events__0.p_events__0_vo_id
                p_events__0.p_events__0_num
                p_events__0.p_events__0_num_cap
                p_events__0.p_events__0_parent_num
                p_events__0.p_events__0_entity_idx
                p_events__0.p_events__0_entity_idx_len
                p_events__0.p_events__0_entity_concept
                p_events__0.p_events__0_entity_name -- name/value
                p_events__0.p_events__0_rm_entity
                p_events__0.p_events__0_data
                p_events__0.p_events__0_entity_attribute
              WHERE
                p_events__0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0002]]
              FROM COMPOSITION
              STRUCTURE CONDITIONS
                p_events__0.p_events__0_entity_attribute EQ [e]
              JOIN p_data__0 -> p_events__0
                on
                  DelegatingJoinCondition p_data__0 ->
                      PathChildCondition COMPOSITION p_data__0 -> COMPOSITION p_events__0


          p_data__1: StructureQuery
              SELECT
                p_data__1.p_data__1_vo_id
                p_data__1.p_data__1_num
                p_data__1.p_data__1_num_cap
                p_data__1.p_data__1_parent_num
                p_data__1.p_data__1_entity_idx
                p_data__1.p_data__1_entity_idx_len
                p_data__1.p_data__1_entity_concept
                p_data__1.p_data__1_entity_name -- name/value
                p_data__1.p_data__1_rm_entity
                p_data__1.p_data__1_data
                p_data__1.p_data__1_entity_attribute
              WHERE
                p_data__1.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0003]]
              FROM COMPOSITION
              STRUCTURE CONDITIONS
                p_data__1.p_data__1_entity_attribute EQ [d]
              JOIN p_events__0 -> p_data__1
                on
                  DelegatingJoinCondition p_events__0 ->
                      PathChildCondition COMPOSITION p_events__0 -> COMPOSITION p_data__1


          p_items__0: StructureQuery
              SELECT
                p_items__0.p_items__0_vo_id
                p_items__0.p_items__0_num
                p_items__0.p_items__0_num_cap
                p_items__0.p_items__0_parent_num
                p_items__0.p_items__0_entity_idx
                p_items__0.p_items__0_entity_idx_len
                p_items__0.p_items__0_entity_concept
                p_items__0.p_items__0_entity_name -- name/value
                p_items__0.p_items__0_rm_entity
                p_items__0.p_items__0_data
                p_items__0.p_items__0_entity_attribute
              WHERE
                p_items__0.?? -- COMPLEX ARCHETYPE_NODE_ID archetype_node_id EQ [AslRmTypeAndConcept[aliasedRmType=null, concept=at0004]]
              FROM COMPOSITION
              STRUCTURE CONDITIONS
                p_items__0.p_items__0_entity_attribute EQ [i]
              JOIN p_data__1 -> p_items__0
                on
                  DelegatingJoinCondition p_data__1 ->
                      PathChildCondition COMPOSITION p_data__1 -> COMPOSITION p_items__0


      LEFT_OUTER_JOIN sOB_o_0 -> p_eq_0
        on
          DelegatingJoinCondition sOB_o_0 ->
              PathChildCondition COMPOSITION sOB_o_0 -> COMPOSITION p_data__0

  WHERE
    AND
      sE_el_0.sE_el_0_entity_name -- name/value EQ [Result]
      sE_el_0.sE_el_0_data -> value -> value EQ [1.3]
  ORDER BY
sOB_o_0.sOB_o_0_entity_name -- name/value ASC
        """);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "SELECT c/uid/value, c/name FROM COMPOSITION c"
            })
    void unchangedAsl(String aqlStr) {
        AslResult result = parseAql(aqlStr);
        String originalAslGraph = AslGraph.createAslGraph(result.aslQuery());
        new AslCleanupPostProcessor()
                .afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        assertThat(AslGraph.createAslGraph(result.aslQuery())).isEqualTo(originalAslGraph);

    }

    @ParameterizedTest
    @ValueSource(
            strings = {

                    "SELECT c/uid/value FROM COMPOSITION c"
            })
    void changedAsl(String aqlStr) {
        AslResult result = parseAql(aqlStr);
        String originalAslGraph = AslGraph.createAslGraph(result.aslQuery());
        new AslCleanupPostProcessor()
                .afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
        String modifiedAslGraph = AslGraph.createAslGraph(result.aslQuery());
        assertThat(modifiedAslGraph).isNotEqualTo(originalAslGraph);
        assertThatNoException().isThrownBy(() -> sqlBuilder.buildSqlQuery(result.aslQuery()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    @Disabled
    void aslGraphRegression(String aql, String aslGraph) {
        AslResult result = parseAql(aql);
        new AslCleanupPostProcessor()
                .afterBuildAsl(result.aslQuery(), result.aqlQuery(), result.queryWrapper(), null);
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

        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);
        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(mockKnowledgeCacheService, () -> "node");
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
