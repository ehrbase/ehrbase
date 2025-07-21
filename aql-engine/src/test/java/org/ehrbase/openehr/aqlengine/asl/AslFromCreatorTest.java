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
package org.ehrbase.openehr.aqlengine.asl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoin;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.jooq.JoinType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ThrowingConsumer;

class AslFromCreatorTest {

    private final KnowledgeCacheService mockKnowledgeCacheService = mock();

    @BeforeEach
    void setUp() {
        Mockito.reset(mockKnowledgeCacheService);
    }

    private AslRootQuery addFromClause(String aql) {
        var aliasProvider = new AslUtils.AliasProvider();
        var aslQuery = new AslRootQuery();

        var aqlQuery = AqlQueryParser.parse(aql);
        var queryWrapper = AqlQueryWrapper.create(aqlQuery, false);

        var aslFromCreator = new AslFromCreator(aliasProvider, mockKnowledgeCacheService);
        aslFromCreator.addFromClause(aslQuery, queryWrapper);

        return aslQuery;
    }

    @Nested
    class Folder {

        @Test
        void simple() {

            AslRootQuery aslQuery = addFromClause(
                    """
                            SELECT f/uid/value
                            FROM FOLDER f
                            """);

            // FROM FOLDER f
            assertThat(aslQuery.getChildren()).singleElement().satisfies(isStructureQueryRootWithVersionOnFolder());
        }

        @Test
        void containsFolder() {

            AslRootQuery aslQuery = addFromClause(
                    """
                            SELECT f1/uid/value, f2/uid/value
                            FROM FOLDER f1 CONTAINS FOLDER f2
                            """);

            assertThat(aslQuery.getChildren()).hasSize(2);

            // FROM FOLDER f
            assertThat(aslQuery.getChildren()).element(0).satisfies(isStructureQueryRootWithVersionOnFolder());
            // CONTAINS FOLDER f2
            assertThat(aslQuery.getChildren())
                    .element(1)
                    .satisfies(isStructureQueryWithDataContains(
                            AslStructureQuery.AslSourceRelation.FOLDER,
                            AslStructureQuery.AslSourceRelation.FOLDER,
                            false));
        }

        @Test
        void containsComposition() {

            AslRootQuery aslQuery = addFromClause(
                    """
                            SELECT c/uid/value
                            FROM FOLDER CONTAINS COMPOSITION c
                            """);

            assertThat(aslQuery.getChildren()).hasSize(2);

            // FROM FOLDER
            assertThat(aslQuery.getChildren()).element(0).satisfies(isStructureQueryRootWithVersionOnFolder());
            // CONTAINS COMPOSITION c
            assertThat(aslQuery.getChildren())
                    .element(1)
                    .satisfies(isStructureQueryWithDataContains(
                            AslStructureQuery.AslSourceRelation.FOLDER,
                            AslStructureQuery.AslSourceRelation.COMPOSITION,
                            true));
        }

        @Test
        void containsFolderContainsComposition() {

            AslRootQuery aslQuery = addFromClause(
                    """
                            SELECT c/uid/value
                            FROM FOLDER CONTAINS FOLDER f2[openEHR-EHR-FOLDER.episode_of_care.v1] CONTAINS COMPOSITION c
                            """);

            assertThat(aslQuery.getChildren()).hasSize(3);

            // FROM FOLDER
            assertThat(aslQuery.getChildren()).element(0).satisfies(isStructureQueryRootWithVersionOnFolder());
            // CONTAINS FOLDER f2
            assertThat(aslQuery.getChildren())
                    .element(1)
                    .satisfies(isStructureQueryWithDataContains(
                            AslStructureQuery.AslSourceRelation.FOLDER,
                            AslStructureQuery.AslSourceRelation.FOLDER,
                            false));
            // FOLDER f2 CONTAINS COMPOSITION c
            assertThat(aslQuery.getChildren())
                    .element(2)
                    .satisfies(isStructureQueryWithDataContains(
                            AslStructureQuery.AslSourceRelation.FOLDER,
                            AslStructureQuery.AslSourceRelation.COMPOSITION,
                            true));
        }

        private static ThrowingConsumer<Pair<AslQuery, AslJoin>> isStructureQueryRootWithVersionOnFolder() {
            return isStructureQueryRootWithVersion(AslStructureQuery.AslSourceRelation.FOLDER);
        }

        private static ThrowingConsumer<Pair<AslQuery, AslJoin>> isStructureQueryWithDataContains(
                AslStructureQuery.AslSourceRelation leftType,
                AslStructureQuery.AslSourceRelation rightType,
                boolean requiresVersionJoin) {
            return pair -> {
                assertThat(pair.getValue()).isNotNull().satisfies(join -> {
                    assertThat(join.getJoinType()).isSameAs(JoinType.JOIN);
                    assertThat(join.getLeft())
                            .isInstanceOfSatisfying(AslStructureQuery.class, left -> assertThat(left.getType())
                                    .isSameAs(leftType));
                    assertThat(join.getRight())
                            .isInstanceOfSatisfying(AslStructureQuery.class, right -> assertThat(right.getType())
                                    .isSameAs(rightType));
                });
                assertThat(pair.getKey()).isInstanceOfSatisfying(AslStructureQuery.class, sq -> {

                    // Source relation FOLDER with version table join and no condition
                    assertThat(sq.getType()).isSameAs(rightType);
                    assertThat(sq.isRequiresVersionTableJoin()).isEqualTo(requiresVersionJoin);
                });
            };
        }
    }

    private static ThrowingConsumer<Pair<AslQuery, AslJoin>> isStructureQueryRootWithVersion(
            AslStructureQuery.AslSourceRelation type) {
        return pair -> {
            assertThat(pair.getKey()).isInstanceOfSatisfying(AslStructureQuery.class, sq -> {

                // Source relation FOLDER with version table join and no condition
                assertThat(sq.getType()).isSameAs(type);
                assertThat(sq.isRequiresVersionTableJoin()).isTrue();
                assertThat(sq.getCondition()).isNull();
            });
            assertThat(pair.getValue()).isNull();
        };
    }
}
