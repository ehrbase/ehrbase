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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ComparisonOperatorConditionWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.ConditionWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.where.LogicalOperatorConditionWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AslGraphTest {

    private KnowledgeCacheService knowledgeCacheService = mock();

    @BeforeEach
    void setUp() {
        doReturn(Optional.empty()).when(knowledgeCacheService).findUuidByTemplateId(any());
    }

    @Test
    @Disabled
    void printDataQueryGraph() {

        //        AqlQuery aqlQuery = AqlQueryParser.parse(
        //                """
        //                SELECT
        //                  c1/content[openEHR-EHR-SECTION.adhoc.v1],
        //                  c1/content[openEHR-EHR-SECTION.adhoc.v1]/name,
        //                  c1/content[openEHR-EHR-SECTION.adhoc.v1]/name/value
        //                  -- , c1/content[openEHR-EHR-SECTION.adhoc.v1,'Diagnostic Results']/name/value
        //                FROM EHR e
        //                  CONTAINS COMPOSITION c1
        //                WHERE
        //                    c1/content[openEHR-EHR-SECTION.adhoc.v1]/name/value = 'test_value'
        //                """);

        // -- e/ehr_id/value,
        // -- o/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value
        // -- c/feeder_audit/originating_system_item_ids,
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
                SELECT
                    c/uid/value,
                    o/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value
                FROM EHR e
                    CONTAINS COMPOSITION c
                        CONTAINS OBSERVATION o[openEHR-EHR-OBSERVATION.blood_pressure.v2]
                WHERE o/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude = 110
                  AND c/feeder_audit/originating_system_item_ids/id = 'Patient/b3f5d4e0-f0d3-4b09-8bb5-36e313c625b5/_history/1'
                """);

        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        AslRootQuery rootQuery = new AqlSqlLayer(knowledgeCacheService, () -> "node").buildAslRootQuery(queryWrapper);

        System.out.println(AslGraph.createAslGraph(rootQuery));

        //        dumpConditionWrapper(queryWrapper.where());

        //        queryWrapper.selects().forEach(selectWrapper -> {
        //            dumpIdentifiedPath(selectWrapper.getIdentifiedPath().orElseThrow());
        //        });
    }

    private void dumpConditionWrapper(ConditionWrapper conditionWrapper) {

        switch (conditionWrapper) {
            case LogicalOperatorConditionWrapper logicalOperatorConditionWrapper -> {
                logicalOperatorConditionWrapper.logicalOperands().forEach(this::dumpConditionWrapper);
            }
            case ComparisonOperatorConditionWrapper comparisonOperatorConditionWrapper -> {
                ConditionWrapper.ComparisonConditionOperator operator = comparisonOperatorConditionWrapper.operator();
                ComparisonOperatorConditionWrapper.IdentifiedPathWrapper leftComparisonOperand =
                        comparisonOperatorConditionWrapper.leftComparisonOperand();
                List<Primitive> rightComparisonOperand = comparisonOperatorConditionWrapper.rightComparisonOperands();

                System.out.println("---------------------------------------");
                dumpIdentifiedPath(leftComparisonOperand.path());
                System.out.println(operator);
                System.out.println(rightComparisonOperand.getFirst().getValue());
                //                throw new NotImplementedException("not implemented " +
                // comparisonOperatorConditionWrapper);
            }
        }
    }

    private void dumpIdentifiedPath(IdentifiedPath identifiedPath) {

        ContainmentClassExpression root = (ContainmentClassExpression) identifiedPath.getRoot();
        AqlObjectPath path = identifiedPath.getPath();
        //        List<ComparisonOperatorPredicate.PredicateComparisonOperator> operators =
        // path.getPathNodes().getLast().getPredicateOrOperands().stream().map(AndOperatorPredicate::getOperands).flatMap(Collection::stream).map(ComparisonOperatorPredicate::getOperator).toList();
        System.out.println(root.getType() + "::" + path.render()); // + " " + operators + "");
    }
}
