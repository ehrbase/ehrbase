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
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction;
import org.ehrbase.openehr.sdk.aql.dto.operand.CountDistinctAggregateFunction;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;

final class SelectCheck implements FeatureCheck {
    private final SystemService systemService;

    public SelectCheck(SystemService systemService) {
        this.systemService = systemService;
    }

    @Override
    public void ensureSupported(AqlQuery aqlQuery) {
        // SELECT
        var select = aqlQuery.getSelect();

        select.getStatement().forEach(selectExp -> {
            switch (selectExp.getColumnExpression()) {
                case IdentifiedPath ip -> ensureSelectPathSupported(ip);
                case AggregateFunction af -> ensureAggregateFunctionSupported(af);
                case Primitive __ -> {
                    // Primitives are allowed
                }
                default ->
                    throw new AqlFeatureNotImplementedException("%s is not supported in SELECT"
                            .formatted(selectExp.getClass().getSimpleName()));
            }
        });
    }

    private void ensureAggregateFunctionSupported(AggregateFunction af) {
        AggregateFunction.AggregateFunctionName func = af.getFunctionName();
        IdentifiedPath ip = af.getIdentifiedPath();
        if (ip == null) {
            // These check for invalid AQL -> IllegalAqlException
            if (func != AggregateFunction.AggregateFunctionName.COUNT) {
                throw new IllegalAqlException(
                        "Aggregate function %s requires an identified path argument.".formatted(func));
            } else if (af instanceof CountDistinctAggregateFunction) {
                throw new IllegalAqlException("COUNT(DISTINCT) requires an identified path argument");
            }
        } else {
            AbstractContainmentExpression containment = ip.getRoot();
            FeatureCheckUtils.PathDetails pathWithType = FeatureCheckUtils.findSupportedIdentifiedPath(
                    ip, true, ClauseType.SELECT, systemService.getSystemId());
            if (func != AggregateFunction.AggregateFunctionName.COUNT) {
                if (pathWithType.extractedColumn() != null
                        && !EnumSet.of(
                                        AslExtractedColumn.OV_TIME_COMMITTED,
                                        AslExtractedColumn.OV_TIME_COMMITTED_DV,
                                        AslExtractedColumn.EHR_TIME_CREATED,
                                        AslExtractedColumn.EHR_TIME_CREATED_DV)
                                .contains(pathWithType.extractedColumn())) {
                    throw new AqlFeatureNotImplementedException(
                            "SELECT: Aggregate function %s is not supported for path %s/%s (COUNT only)"
                                    .formatted(
                                            func,
                                            containment.getIdentifier(),
                                            ip.getPath().render()));
                }
                if (EnumSet.of(AggregateFunction.AggregateFunctionName.AVG, AggregateFunction.AggregateFunctionName.SUM)
                        .contains(func)) {
                    if (EnumSet.of(
                                    AslExtractedColumn.OV_TIME_COMMITTED,
                                    AslExtractedColumn.OV_TIME_COMMITTED_DV,
                                    AslExtractedColumn.EHR_TIME_CREATED,
                                    AslExtractedColumn.EHR_TIME_CREATED_DV)
                            .contains(pathWithType.extractedColumn())) {
                        throw new AqlFeatureNotImplementedException(
                                "SELECT: Aggregate function %s(%s/%s) not applicable to the given path"
                                        .formatted(
                                                func,
                                                containment.getIdentifier(),
                                                ip.getPath().render()));
                    }
                    if (pathWithType.targetsDvOrdered()) {
                        throw new AqlFeatureNotImplementedException(
                                "SELECT: Aggregate function %s(%s/%s) not applicable to paths targeting subtypes of DV_ORDERED"
                                        .formatted(
                                                func,
                                                containment.getIdentifier(),
                                                ip.getPath().render()));
                    }
                    if (!pathWithType.targetsPrimitive()) {
                        throw new AqlFeatureNotImplementedException(
                                "SELECT: Aggregate function %s(%s/%s) only applicable to paths targeting primitive types"
                                        .formatted(
                                                func,
                                                containment.getIdentifier(),
                                                ip.getPath().render()));
                    }
                } else if (EnumSet.of(
                                        AggregateFunction.AggregateFunctionName.MAX,
                                        AggregateFunction.AggregateFunctionName.MIN)
                                .contains(func)
                        && !(pathWithType.targetsPrimitive() || pathWithType.targetsDvOrdered())) {
                    throw new AqlFeatureNotImplementedException(
                            "SELECT: Aggregate function %s(%s/%s) only applicable to paths targeting primitive types or subtypes of DV_ORDERED"
                                    .formatted(
                                            func,
                                            containment.getIdentifier(),
                                            ip.getPath().render()));
                }
            }
        }
    }

    private void ensureSelectPathSupported(IdentifiedPath ip) {
        FeatureCheckUtils.findSupportedIdentifiedPath(ip, true, ClauseType.SELECT, systemService.getSystemId());
    }
}
