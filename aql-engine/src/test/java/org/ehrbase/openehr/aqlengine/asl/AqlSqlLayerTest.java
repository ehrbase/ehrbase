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
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslPathDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.jooq.JSONB;
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
        """);
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
        """);
        List<AslQuery> queries =
                aslQuery.getChildren().stream().map(Pair::getLeft).toList();

        assertThat(queries).hasSize(5);

        assertThat(queries.get(0)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(1)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(2)).isInstanceOf(AslEncapsulatingQuery.class);
        assertThat(queries.get(3)).isInstanceOf(AslEncapsulatingQuery.class);
        assertThat(queries.get(4)).isInstanceOf(AslPathDataQuery.class);

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

        // assertThat(queries.get(5)).isInstanceOf(AslRmObjectDataQuery.class);
    }

    @Test
    void clusterDataSingleSelection() {

        AslRootQuery aslQuery = buildSqlQuery(
                """
        SELECT
            cluster/items[at0001]/value/data
        FROM COMPOSITION CONTAINS CLUSTER cluster[openEHR-EHR-CLUSTER.media_file.v1]
        """);
        List<AslQuery> queries =
                aslQuery.getChildren().stream().map(Pair::getLeft).toList();

        assertThat(queries).hasSize(4);

        assertThat(queries.get(0)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(1)).isInstanceOf(AslStructureQuery.class);
        assertThat(queries.get(2)).isInstanceOf(AslEncapsulatingQuery.class);
        assertThat(queries.get(3)).isInstanceOfSatisfying(AslPathDataQuery.class, q -> {
            assertThat(q.isMultipleValued()).isFalse();
            assertThat(q.getDataField().getColumnName()).isEqualTo("data");
            assertThat(q.getDataField().getType()).isSameAs(JSONB.class);
        });
    }

    private AslRootQuery buildSqlQuery(String query) {

        AqlQuery aqlQuery = AqlQueryParser.parse(query);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(mockKnowledgeCacheService, () -> "node");
        return aqlSqlLayer.buildAslRootQuery(queryWrapper);
    }
}
