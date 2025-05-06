/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.test.ServiceIntegrationTest;
import org.ehrbase.util.SemVer;
import org.ehrbase.util.StoredQueryQualifiedName;
import org.jooq.impl.ParserException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ServiceIntegrationTest
class StoredQueryRepositoryIT {

    @Autowired
    StoredQueryRepository storedQueryRepository;

    @Test
    void retrieveAllLatest() {

        storedQueryRepository.store(
                StoredQueryQualifiedName.create("dom.2::QueryA", new SemVer(1, 2, 3, null)), "SELECT h FROM EHR h");

        storedQueryRepository.store(
                StoredQueryQualifiedName.create("dom.1::QueryB", new SemVer(1, 2, 3, null)), "SELECT e FROM EHR e");
        // SNAPSHOT ignored
        storedQueryRepository.store(
                StoredQueryQualifiedName.create("dom.1::QueryB", new SemVer(3, 2, 3, "SNAPSHOT")),
                "SELECT g FROM EHR g");
        storedQueryRepository.store(
                StoredQueryQualifiedName.create("dom.1::QueryB", new SemVer(2, 2, 3, null)), "SELECT f FROM EHR f");

        storedQueryRepository.store(
                StoredQueryQualifiedName.create("dom.1::QueryX", new SemVer(2, 2, 3, null)), "SELECT x FROM EHR x");

        // SNAPSHOT ignored
        storedQueryRepository.store(
                StoredQueryQualifiedName.create("dom.3::QueryC", new SemVer(3, 2, 3, "SNAPSHOT")),
                "SELECT i FROM EHR i");

        List<QueryDefinitionResultDto> results = storedQueryRepository.retrieveAllLatest();
        assertThat(results.stream().map(QueryDefinitionResultDto::getQueryText))
                .containsExactly("SELECT f FROM EHR f", "SELECT x FROM EHR x", "SELECT h FROM EHR h");
        results.stream().forEach(System.out::println);
    }

    @Test
    void parseSelectQuery() {
        String query = "SELECT e.id FROM ehr.ehr e";

        assertThatNoException().isThrownBy(() -> storedQueryRepository.parseQuery(query));
    }

    @Test
    void parseUnsupportedStatements() {
        String insert = "INSERT INTO ehr.ehr VALUES ('bb1006cb-c6b3-4715-a6bc-0bc629e37702', '2025-05-06 00:00:00.000')";
        String update = "UPDATE ehr.ehr SET id = NULL WHERE id = 'bb1006cb-c6b3-4715-a6bc-0bc629e37702'";
        String delete = "DELETE FROM ehr.ehr e";

        assertThatThrownBy(() -> storedQueryRepository.parseQuery(insert))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Unsupported query type: [1:1] [*]INSERT INTO ehr.ehr");
        assertThatThrownBy(() -> storedQueryRepository.parseQuery(update))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Unsupported query type: [1:1] [*]UPDATE ehr.ehr");
        assertThatThrownBy(() -> storedQueryRepository.parseQuery(delete))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Unsupported query type: [1:1] [*]DELETE FROM ehr.ehr");
    }
}
