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
package org.ehrbase.openehr.aqlengine.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.aqlengine.AqlEhrPathPostProcessor;
import org.ehrbase.openehr.aqlengine.AqlFromEhrOptimisationPostProcessor;
import org.ehrbase.openehr.aqlengine.asl.AqlSqlLayer;
import org.ehrbase.openehr.aqlengine.asl.AslCleanupPostProcessor;
import org.ehrbase.openehr.aqlengine.asl.AslGraph;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathCohesionAnalysis;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathInfo;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.select.SelectWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.util.TestConfig;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.SelectQuery;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class AqlSqlQueryBuilderTest {

    private final KnowledgeCacheService mockKnowledgeCacheService = mock();

    private AqlSqlQueryBuilder aqlSqlQueryBuilder() {
        TemplateService templateService = mock();
        Mockito.when(templateService.findAllTemplateIds())
                .thenReturn(Map.of(UUID.randomUUID(), "template1.v1", UUID.randomUUID(), "template2.v3"));

        return new AqlSqlQueryBuilder(
                TestConfig.aqlConfigurationProperties(),
                new DefaultDSLContext(SQLDialect.POSTGRES),
                templateService,
                Optional.empty());
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(mockKnowledgeCacheService);
        Mockito.when(mockKnowledgeCacheService.findUuidByTemplateId(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(UUID.randomUUID()));
    }

    @Disabled
    @Test
    void printSqlQuery() {
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
            SELECT c/feeder_audit/original_content FROM COMPOSITION c
        """);

        System.out.println("/*");
        System.out.println(aqlQuery.render());
        System.out.println("*/");

        new AqlEhrPathPostProcessor().afterParseAql(aqlQuery, null, null);
        new AqlFromEhrOptimisationPostProcessor().afterParseAql(aqlQuery, null, null);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(mockKnowledgeCacheService, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
        new AslCleanupPostProcessor().afterBuildAsl(aslQuery, aqlQuery, queryWrapper, null);

        System.out.println("/*");
        System.out.println(AslGraph.createAslGraph(aslQuery));
        System.out.println("*/");
        System.out.println();

        AqlSqlQueryBuilder sqlQueryBuilder = aqlSqlQueryBuilder();

        SelectQuery<Record> sqlQuery = sqlQueryBuilder.buildSqlQuery(aslQuery);
        System.out.println(sqlQuery);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                """
                    SELECT o/data/events/data/items/value/magnitude
                    FROM OBSERVATION o [openEHR-EHR-OBSERVATION.conformance_observation.v0]
                    WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0008]/value = 82.0
                    """,
                """
                    SELECT e/ehr_id/value, cv0/commit_audit/time_committed/value, c0
                    FROM EHR e
                    CONTAINS FOLDER f0[name/value = '1439656']
                    CONTAINS VERSION cv0[LATEST_VERSION]
                    CONTAINS COMPOSITION c0
                    """
            })
    void canBuildSqlQuery(String aql) {

        AqlQuery aqlQuery = AqlQueryParser.parse(aql);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(mockKnowledgeCacheService, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
        AqlSqlQueryBuilder sqlQueryBuilder = aqlSqlQueryBuilder();

        assertDoesNotThrow(() -> sqlQueryBuilder.buildSqlQuery(aslQuery));
    }

    @Test
    void queryOnData() {
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
        SELECT
        c/content,
        c/content[at0001],
        c/content[at0002],
        c/uid/value,
        c/context/other_context[at0004]/items[at0014]/value
        FROM EHR e CONTAINS COMPOSITION c
        WHERE e/ehr_id/value = 'e6fad8ba-fb4f-46a2-bf82-66edb43f142f'
        """);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        assertDoesNotThrow(() -> buildSqlQuery(queryWrapper));
    }

    @Test
    void queryOnFolder() {
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
        SELECT
        f/uid/value
        FROM EHR
        CONTAINS FOLDER f
        """);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        assertThat(queryWrapper.pathInfos()).hasSize(1);
        assertThat(queryWrapper.selects()).singleElement().satisfies(select -> {
            assertThat(select.type()).isEqualTo(SelectWrapper.SelectType.PATH);
            assertThat(select.getSelectPath()).hasValueSatisfying(path -> {
                assertThat(path).isEqualTo("f/uid/value");
            });
            assertThat(select.root()).satisfies(root -> {
                assertThat(root.getRmType()).isEqualTo("FOLDER");
                assertThat(root.alias()).isEqualTo("f");
            });
        });

        assertDoesNotThrow(() -> buildSqlQuery(queryWrapper));
    }

    @Test
    void queryOnFolderWithComposition() {
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
            SELECT
              c/uid/value
            FROM FOLDER CONTAINS COMPOSITION c
        """);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        assertThat(queryWrapper.pathInfos()).hasSize(1);
        assertThat(queryWrapper.selects()).singleElement().satisfies(select -> {
            assertThat(select.type()).isEqualTo(SelectWrapper.SelectType.PATH);
            assertThat(select.getSelectPath()).hasValueSatisfying(path -> {
                assertThat(path).isEqualTo("c/uid/value");
            });
            assertThat(select.root()).satisfies(root -> {
                assertThat(root.getRmType()).isEqualTo("COMPOSITION");
                assertThat(root.alias()).isEqualTo("c");
            });
        });

        SelectQuery<Record> selectQuery = buildSqlQuery(queryWrapper);
        assertThat(selectQuery.toString())
                // items_id_value are selected from folder
                .contains("\"sF_0sq\".\"fi_uuids\" as \"sF_0_item_id_value\"")
                .contains("and \"descendant\".\"num\" between \"base\".\"num\" and \"base\".\"num_cap\"")
                // compositions are joined on item_id_value
                .contains("on \"sCO_c_0\".\"sCO_c_0_vo_id\" = \"sF_0\".\"sF_0_item_id_value\"");
    }

    @Test
    void clusterWithDataMultiplicitySelectSingle() {
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
        SELECT
            cluster/items[at0001]/value/data
        FROM COMPOSITION CONTAINS CLUSTER cluster[openEHR-EHR-CLUSTER.media_file.v1]
        """);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        assertThat(queryWrapper.pathInfos()).hasSize(1);
        PathInfo pathInfo = queryWrapper.pathInfos().entrySet().stream()
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow();

        PathCohesionAnalysis.PathCohesionTreeNode cohesionTreeRoot = pathInfo.getCohesionTreeRoot();
        assertThat(pathInfo.isMultipleValued(cohesionTreeRoot)).isFalse();

        // Ensure generated query does not try to perform jsonb array selection
        SelectQuery<Record> selectQuery = buildSqlQuery(queryWrapper);
        assertThat(selectQuery.toString()).doesNotContain("select jsonb_array_elements(");
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource
    void aslGraphRegression(String name, String aql, String aslGraph) {
        AqlQuery aqlQuery = AqlQueryParser.parse(aql);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);
        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(mockKnowledgeCacheService, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
        assertThat(AslGraph.createAslGraph(aslQuery)).isEqualToIgnoringWhitespace(aslGraph);
    }

    public static Stream<Arguments> aslGraphRegression() throws IOException {
        var res = new PathMatchingResourcePatternResolver(AqlSqlQueryBuilderTest.class.getClassLoader());
        Resource[] resources =
                res.getResources("classpath*:/org/ehrbase/openehr/aqlengine/testdata/aslGraphRegression-*.txt");
        return Arrays.stream(resources).map(r -> {
            try {
                String filename = r.getFilename();
                String testName = filename.substring(filename.indexOf('-') + 1, filename.length() - 4);
                String[] parts = r.getContentAsString(StandardCharsets.UTF_8).split("\\R+##.+\\R+");
                return Arguments.of(testName, parts[1].trim(), parts.length < 3 ? "" : parts[2].trim());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private SelectQuery<Record> buildSqlQuery(AqlQueryWrapper queryWrapper) {
        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(mockKnowledgeCacheService, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
        AqlSqlQueryBuilder sqlQueryBuilder = aqlSqlQueryBuilder();

        return sqlQueryBuilder.buildSqlQuery(aslQuery);
    }
}
