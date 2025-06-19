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
