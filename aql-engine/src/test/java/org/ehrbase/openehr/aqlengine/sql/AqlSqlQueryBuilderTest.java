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

import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.aqlengine.asl.AqlSqlLayer;
import org.ehrbase.openehr.aqlengine.asl.AslGraph;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.SelectQuery;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AqlSqlQueryBuilderTest {

    public static class TestServerConfig implements ServerConfig {
        @Override
        public int getPort() {
            return 0;
        }

        @Override
        public void setPort(int port) {}

        @Override
        public boolean isDisableStrictValidation() {
            return false;
        }
    }

    @Disabled
    @Test
    void printSqlQuery() {
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
        SELECT
        c/feeder_audit,
        c/uid/value,
        c/context/other_context[at0004]/items[at0014]/value
        FROM EHR e CONTAINS COMPOSITION c
        WHERE e/ehr_id/value = 'e6fad8ba-fb4f-46a2-bf82-66edb43f142f'
        """);

        System.out.println("/*");
        System.out.println(aqlQuery.render());
        System.out.println("*/");

        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        KnowledgeCacheService kcs = Mockito.mock(KnowledgeCacheService.class);
        Mockito.when(kcs.findUuidByTemplateId(ArgumentMatchers.anyString())).thenReturn(Optional.of(UUID.randomUUID()));

        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(kcs, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);

        System.out.println("/*");
        System.out.println(AslGraph.createAslGraph(aslQuery));
        System.out.println("*/");
        System.out.println();

        AqlSqlQueryBuilder sqlQueryBuilder =
                new AqlSqlQueryBuilder(new DefaultDSLContext(SQLDialect.YUGABYTEDB), kcs, Optional.empty());

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
                """
            })
    void canBuildSqlQuery(String aql) {

        AqlQuery aqlQuery = AqlQueryParser.parse(aql);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);
        KnowledgeCacheService kcs = Mockito.mock(KnowledgeCacheService.class);
        Mockito.when(kcs.findUuidByTemplateId(ArgumentMatchers.anyString())).thenReturn(Optional.of(UUID.randomUUID()));

        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(kcs, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
        AqlSqlQueryBuilder sqlQueryBuilder =
                new AqlSqlQueryBuilder(new DefaultDSLContext(SQLDialect.YUGABYTEDB), kcs, Optional.empty());

        Assertions.assertDoesNotThrow(() -> sqlQueryBuilder.buildSqlQuery(aslQuery));
    }
}
