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
package org.ehrbase.openehr.aqlengine.featurecheck;

import java.util.EnumSet;
import org.ehrbase.api.exception.AqlFeatureNotImplementedException;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.aqlengine.aql.AqlQueryUtils;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.condition.ComparisonOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.ComparisonOperatorSymbol;
import org.ehrbase.openehr.sdk.aql.dto.condition.ExistsCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.LikeCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.MatchesCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.WhereCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.ComparisonLeftOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.LikeOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;

final class WhereCheck implements FeatureCheck {
    private final SystemService systemService;

    public WhereCheck(SystemService systemService) {
        this.systemService = systemService;
    }

    @Override
    public void ensureSupported(AqlQuery aqlQuery) {
        WhereCondition where = aqlQuery.getWhere();

        AqlQueryUtils.streamWhereConditions(where).forEach(c -> {
            switch (c) {
                case ComparisonOperatorCondition comp -> ensureWhereComparisonConditionSupported(comp);
                case LikeCondition like -> ensureLikeConditionSupported(like);
                case MatchesCondition matches -> ensureMatchesConditionSupported(matches);
                case ExistsCondition exists -> ensureExistsConditionSupported(exists);
                default -> throw new IllegalAqlException("Unexpected condition type %s".formatted(c));
            }
        });
    }

    private void ensureWhereComparisonConditionSupported(ComparisonOperatorCondition condition) {
        ComparisonLeftOperand conditionStatement = condition.getStatement();

        if (conditionStatement instanceof IdentifiedPath conditionField) {
            FeatureCheckUtils.PathDetails pathWithType = FeatureCheckUtils.findSupportedIdentifiedPath(
                    conditionField, false, ClauseType.WHERE, systemService.getSystemId());
            if (conditionField.getPath().equals(AslExtractedColumn.ARCHETYPE_NODE_ID.getPath())
                    && !EnumSet.of(ComparisonOperatorSymbol.EQ, ComparisonOperatorSymbol.NEQ)
                            .contains(condition.getSymbol())) {
                throw new AqlFeatureNotImplementedException(
                        "Conditions on 'archetype_node_id' only support =,!=, LIKE and MATCHES");
            }
            if (conditionField.getPath().equals(AslExtractedColumn.TEMPLATE_ID.getPath())
                    && !EnumSet.of(ComparisonOperatorSymbol.EQ, ComparisonOperatorSymbol.NEQ)
                            .contains(condition.getSymbol())) {
                throw new AqlFeatureNotImplementedException(
                        "Conditions on 'archetype_details/template_id/value' only support =,!= and MATCHES");
            }
            if (pathWithType.extractedColumn() == AslExtractedColumn.OV_TIME_COMMITTED) {
                throw new AqlFeatureNotImplementedException("Conditions on %s of VERSION"
                        .formatted(conditionField.getPath().render()));
            }
            if (EnumSet.of(
                                    AslExtractedColumn.AD_CHANGE_TYPE_VALUE,
                                    AslExtractedColumn.AD_CHANGE_TYPE_CODE_STRING,
                                    AslExtractedColumn.AD_CHANGE_TYPE_PREFERRED_TERM)
                            .contains(pathWithType.extractedColumn())
                    && !EnumSet.of(ComparisonOperatorSymbol.EQ, ComparisonOperatorSymbol.NEQ)
                            .contains(condition.getSymbol())) {
                throw new AqlFeatureNotImplementedException("Conditions on %s of VERSION only support =,!= and MATCHES"
                        .formatted(conditionField.getPath().render()));
            }
            FeatureCheckUtils.ensureOperandSupported(pathWithType, condition.getValue(), systemService.getSystemId());
        } else {
            throw new AqlFeatureNotImplementedException("Functions are not supported in WHERE");
        }
    }

    private static void ensureExistsConditionSupported(ExistsCondition exists) {
        throw new AqlFeatureNotImplementedException("WHERE: EXISTS operator is not supported");
        // ensureIdentifiedPathSupported(exists.getValue(), false, "WHERE");
    }

    private void ensureMatchesConditionSupported(MatchesCondition matches) {
        FeatureCheckUtils.PathDetails pathWithType = FeatureCheckUtils.findSupportedIdentifiedPath(
                matches.getStatement(), false, ClauseType.WHERE, systemService.getSystemId());
        matches.getValues()
                .forEach(operand ->
                        FeatureCheckUtils.ensureOperandSupported(pathWithType, operand, systemService.getSystemId()));
    }

    private void ensureLikeConditionSupported(LikeCondition like) {
        AqlObjectPath path = like.getStatement().getPath();
        FeatureCheckUtils.findSupportedIdentifiedPath(
                like.getStatement(), false, ClauseType.WHERE, systemService.getSystemId());
        LikeOperand operand = like.getValue();
        if (AslExtractedColumn.VO_ID.getPath().equals(path)) {
            throw new AqlFeatureNotImplementedException("LIKE on /uid/value is not supported");
        }
        if (!(operand instanceof Primitive primitive)) {
            throw new AqlFeatureNotImplementedException("Only primitive operands are supported");
        }
        Object value = primitive.getValue();
        if (!(value instanceof String s)) {
            throw new AqlFeatureNotImplementedException("LIKE must use String values");
        }
        if (AslExtractedColumn.ARCHETYPE_NODE_ID.getPath().equals(path) && !s.matches("openEHR-EHR-[A-Z]+\\..*")) {
            throw new AqlFeatureNotImplementedException(
                    "LIKE on archetype_node_id has to start with 'openEHR-EHR-{RM-TYPE}.'");
        }
        if (AslExtractedColumn.TEMPLATE_ID.getPath().equals(path)) {
            throw new AqlFeatureNotImplementedException(
                    "Conditions on 'archetype_details/template_id/value' only support =,!= and MATCHES");
        }
    }
}
