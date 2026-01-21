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

import java.util.stream.IntStream;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.aqlengine.TestAqlQueryContext;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AslGraphTest {

    private final KnowledgeCacheService mockKnowledgeCacheService = mock();

    @Test
    @Disabled
    void printDataQueryGraph() {

        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
                SELECT
                  -- c1/content[openEHR-EHR-SECTION.adhoc.v1],
                  -- c1/content[openEHR-EHR-SECTION.adhoc.v1]/name,
                  c1/content[openEHR-EHR-SECTION.adhoc.v1]/name/value
                  -- ,c1/content[openEHR-EHR-SECTION.adhoc.v1,'Diagnostic Results']/name/value
                FROM EHR e
                  CONTAINS COMPOSITION c1
                """);

        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery, false);

        AslRootQuery rootQuery = new AqlSqlLayer(mockKnowledgeCacheService, () -> "node", new TestAqlQueryContext())
                .buildAslRootQuery(queryWrapper);

        System.out.println(AslGraph.createAslGraph(rootQuery));
    }

    @Test
    void stableAslGraphTest() {
        var variants = IntStream.range(0, 100)
                .parallel()
                .mapToObj(i -> createSampleGraph())
                .distinct()
                .toList();

        assertThat(variants).hasSize(1);
    }

    private String createSampleGraph() {
        AqlQuery sampleAqlQuery = AqlQueryParser.parse(
                """
             SELECT
             c/uid/value AS compositionID,
             c/context/start_time AS startTime,
             sample/items[at0002]/value/value,
             c/feeder_audit,
             xds/items[at0003]/value,
             c/content[openEHR-EHR-SECTION.problem_list.v0]/items[openEHR-EHR-EVALUATION.problem_diagnosis.v1]/data[at0001]/items[at0002]/value,
             sample/items[at0007]/items[at0016]/value,
             xds/items[at0010]/value/value,
             sample/items[at0023]/value/value,
             sample/items[at0007]/items[at0014]/value,
             sample/items[at0007]/items[at0015]/value,
             c/content[openEHR-EHR-SECTION.conclusion.v0]/items[openEHR-EHR-EVALUATION.clinical_synopsis.v1]/data[at0001]/items[at0002]/value/value,
             o/data[at0001]/events[at0002]/data[at0003]/items[at0101]/value/value,
             o/data[at0001]/events[at0002]/data[at0003]/items[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1]/items[at0024]/value,
             o/data[at0001]/events[at0002]/data[at0003]/items[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1]/items[at0001]/value,
             o/data[at0001]/events[at0002]/data[at0003]/items[openEHR-EHR-CLUSTER.specimen.v1]/items[at0015]/value/value
             FROM EHR e[ehr_id/value='87a474c0-ce8d-4f6f-b999-20b2f1842e47']
             CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.report-result.v1]
             CONTAINS (
             CLUSTER sample[openEHR-EHR-CLUSTER.sample.v0]
             AND
             CLUSTER xds[openEHR-EHR-CLUSTER.xds_metadata.v0]
             AND
             OBSERVATION o[openEHR-EHR-OBSERVATION.laboratory_test_result.v1]
             )
             WHERE
             o/data[at0001]/events[at0002]/data[at0003]/items[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1]/items[at0024]/value/defining_code/code_string='123'
             ORDER BY o/data[at0001]/events[at0002]/data[at0003]/items[openEHR-EHR-CLUSTER.specimen.v1]/items[at0015]/value DESC
             """);

        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(sampleAqlQuery, false);

        AslRootQuery rootQuery = new AqlSqlLayer(mockKnowledgeCacheService, () -> "node", new TestAqlQueryContext())
                .buildAslRootQuery(queryWrapper);

        new AslCleanupPostProcessor().afterBuildAsl(rootQuery, sampleAqlQuery, queryWrapper, null);

        return AslGraph.createAslGraph(rootQuery);
    }
}
