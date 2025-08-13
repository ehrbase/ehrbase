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
package org.ehrbase.openehr.aqlengine.pathanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathCohesionAnalysis.PathCohesionTreeNode;
import org.ehrbase.openehr.aqlengine.pathanalysis.PathInfo.PathJoinConditionType;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.util.TreeUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class PathInfoTest {

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource
    void pathSkipping(String name, String aql, String cohesionTrees) {
        AqlQuery aqlQuery = AqlQueryParser.parse(aql);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery, true);
        String actualTrees = queryWrapper.pathInfos().entrySet().stream()
                .map(e -> e.getKey().alias() + ": "
                        + renderTree(
                                e.getValue().getCohesionTreeRoot(), e.getValue().getJoinConditionTypes()))
                .collect(Collectors.joining("\n"));
        try {

            assertThat(actualTrees).isEqualToNormalizingNewlines(cohesionTrees);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public static Stream<Arguments> pathSkipping() throws IOException {
        var res = new PathMatchingResourcePatternResolver(PathInfoTest.class.getClassLoader());
        Resource r = res.getResource("classpath:/org/ehrbase/openehr/aqlengine/testdata/pathskipping.txt");
        return Arrays.stream(r.getContentAsString(StandardCharsets.UTF_8).split("\\R+#+\\R+"))
                .skip(1)
                .map(tc -> {
                    String[] split = tc.split(">>>");
                    return Arguments.of(split[0], split[1], split[2].trim().replaceAll("^\\s*\\R", ""));
                });
    }

    private static String renderTree(
            PathCohesionTreeNode node, Map<PathCohesionTreeNode, Set<PathJoinConditionType>> joinTypeMap) {
        return TreeUtils.renderTree(
                node,
                Comparator.comparing(n -> new AqlObjectPath(n.getAttribute()).render()),
                n -> new AqlObjectPath(n.getAttribute()).render() + ": " + joinTypeMap.get(n));
    }
}
