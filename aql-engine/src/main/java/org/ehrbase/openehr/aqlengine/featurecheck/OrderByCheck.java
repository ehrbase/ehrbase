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
import java.util.List;
import java.util.Optional;
import org.ehrbase.api.exception.AqlFeatureNotImplementedException;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.orderby.OrderByExpression;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;

final class OrderByCheck implements FeatureCheck {
    private final SystemService systemService;

    public OrderByCheck(SystemService systemService) {
        this.systemService = systemService;
    }

    @Override
    public void ensureSupported(AqlQuery aqlQuery) {
        Optional.of(aqlQuery).map(AqlQuery::getOrderBy).stream()
                .flatMap(List::stream)
                .map(OrderByExpression::getStatement)
                .forEach(ip -> ensureOrderByStatementSupported(aqlQuery, ip));
    }

    private void ensureOrderByStatementSupported(AqlQuery aqlQuery, IdentifiedPath ip) {

        // find fields not present in SELECT
        if (aqlQuery.getSelect().getStatement().stream()
                .map(SelectExpression::getColumnExpression)
                .filter(IdentifiedPath.class::isInstance)
                .map(IdentifiedPath.class::cast)
                .noneMatch(selected -> FeatureCheckUtils.startsWith(selected, ip))) {
            throw new AqlFeatureNotImplementedException("ORDER BY: Path: %s%s/%s is not present in SELECT statement"
                    .formatted(
                            ip.getRoot().getIdentifier(),
                            ip.getRootPredicate() == null ? "" : AqlRenderer.renderPredicate(ip.getRootPredicate()),
                            ip.getPath().render()));
        }
        FeatureCheckUtils.PathDetails pathWithType = FeatureCheckUtils.findSupportedIdentifiedPath(
                ip, false, ClauseType.ORDER_BY, systemService.getSystemId());
        if (EnumSet.of(
                        AslExtractedColumn.AD_SYSTEM_ID,
                        AslExtractedColumn.AD_CHANGE_TYPE_TERMINOLOGY_ID_VALUE)
                .contains(pathWithType.extractedColumn())) {
            throw new AqlFeatureNotImplementedException(
                    "ORDER BY: Path: %s on VERSION".formatted(ip.getPath().render()));
        }
    }
}
