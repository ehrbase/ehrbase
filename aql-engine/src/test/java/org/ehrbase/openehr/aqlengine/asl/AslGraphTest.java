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

import org.ehrbase.openehr.aqlengine.AqlConfigurationProperties;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AslGraphTest {

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

        AslRootQuery rootQuery =
                new AqlSqlLayer(null, () -> "node", new AqlConfigurationProperties()).buildAslRootQuery(queryWrapper);

        System.out.println(AslGraph.createAslGraph(rootQuery));
    }
}
