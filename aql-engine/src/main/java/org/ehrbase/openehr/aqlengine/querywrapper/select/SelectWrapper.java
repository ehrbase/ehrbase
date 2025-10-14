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
package org.ehrbase.openehr.aqlengine.querywrapper.select;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction.AggregateFunctionName;
import org.ehrbase.openehr.sdk.aql.dto.operand.CountDistinctAggregateFunction;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;

public final class SelectWrapper {
    private final SelectExpression selectExpression;
    private final SelectType type;
    private final ContainsWrapper root;

    public SelectWrapper(SelectExpression selectExpression, SelectType type, ContainsWrapper root) {
        this.selectExpression = selectExpression;
        this.type = type;
        this.root = root;
    }

    public enum SelectType {
        PATH,
        PRIMITIVE,
        AGGREGATE_FUNCTION,
        FUNCTION
    }

    public String getSelectAlias() {
        return selectExpression.getAlias();
    }

    public Optional<IdentifiedPath> getIdentifiedPath() {
        return Optional.ofNullable(
                        switch (type) {
                            case PATH -> selectExpression.getColumnExpression();
                            case PRIMITIVE -> null;
                            case AGGREGATE_FUNCTION ->
                                ((AggregateFunction) selectExpression.getColumnExpression()).getIdentifiedPath();
                            case FUNCTION -> throw new UnsupportedOperationException("Not implemented");
                        })
                .map(IdentifiedPath.class::cast);
    }

    public AggregateFunctionName getAggregateFunctionName() {
        if (type == SelectType.AGGREGATE_FUNCTION) {
            return ((AggregateFunction) selectExpression.getColumnExpression()).getFunctionName();
        }
        throw new UnsupportedOperationException();
    }

    public boolean isCountDistinct() {
        if (type != SelectType.AGGREGATE_FUNCTION) {
            throw new UnsupportedOperationException();
        }
        return selectExpression.getColumnExpression() instanceof CountDistinctAggregateFunction;
    }

    public Primitive getPrimitive() {
        if (type != SelectType.PRIMITIVE) {
            throw new UnsupportedOperationException();
        }
        return (Primitive) selectExpression.getColumnExpression();
    }

    public Optional<String> getSelectPath() {
        if (type == SelectType.PATH) {
            return Optional.of(Stream.of(
                            root.alias(),
                            getIdentifiedPath()
                                    .map(IdentifiedPath::getPath)
                                    .map(AqlObjectPath::render)
                                    .orElse(null))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.joining("/")));
        } else {
            return Optional.empty();
        }
    }

    public SelectExpression selectExpression() {
        return selectExpression;
    }

    public SelectType type() {
        return type;
    }

    public ContainsWrapper root() {
        return root;
    }

    @Override
    public String toString() {
        return "SelectWrapper[" + "selectExpression="
                + selectExpression + ", " + "type="
                + type + ", " + "root="
                + root + ']';
    }
}
