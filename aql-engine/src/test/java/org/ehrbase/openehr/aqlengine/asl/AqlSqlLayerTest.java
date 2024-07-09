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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AqlSqlLayerTest {

    @Disabled
    @Test
    void printAslGraph() {
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
        SELECT
        c/feeder_audit,
        c/uid/value,
        c/context/other_context[at0004]/items[at0014]/value
        FROM EHR e CONTAINS COMPOSITION c
        WHERE e/ehr_id/value = 'e6fad8ba-fb4f-46a2-bf82-66edb43f142f'
        """);

        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        KnowledgeCacheService kcs = Mockito.mock(KnowledgeCacheService.class);
        Mockito.when(kcs.findUuidByTemplateId(ArgumentMatchers.anyString())).thenReturn(Optional.of(UUID.randomUUID()));

        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(kcs, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);

        System.out.println(AslGraph.createAslGraph(aslQuery));
    }

    @Test
    void testDataQueryPlacedLast() {
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
        SELECT
        c/feeder_audit,
        c/uid/value,
        c/context/other_context[at0004]/items[at0014]/value
        FROM EHR e CONTAINS COMPOSITION c
        WHERE e/ehr_id/value = 'e6fad8ba-fb4f-46a2-bf82-66edb43f142f'
        """);

        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        KnowledgeCacheService kcs = Mockito.mock(KnowledgeCacheService.class);
        Mockito.when(kcs.findUuidByTemplateId(ArgumentMatchers.anyString())).thenReturn(Optional.of(UUID.randomUUID()));

        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(kcs, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);

        List<AslQuery> queries =
                aslQuery.getChildren().stream().map(Pair::getLeft).toList();

        assertThat(queries).hasSize(6);

        assertThat(queries.get(0)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(1)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(2)).isInstanceOf(AslEncapsulatingQuery.class);
        assertThat(queries.get(3)).isInstanceOf(AslEncapsulatingQuery.class);

        assertThat(queries.get(4)).isInstanceOf(AslDataQuery.class);
        assertThat(queries.get(5)).isInstanceOf(AslDataQuery.class);
    }
}
