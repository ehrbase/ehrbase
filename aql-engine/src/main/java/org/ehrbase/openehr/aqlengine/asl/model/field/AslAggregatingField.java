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
package org.ehrbase.openehr.aqlengine.asl.model.field;

import org.ehrbase.openehr.aqlengine.asl.meta.AslFieldOrigin;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction.AggregateFunctionName;

public final class AslAggregatingField extends AslVirtualField {

    private final AggregateFunctionName function;
    private final AslField baseField;
    private final boolean distinct;

    public AslAggregatingField(AggregateFunctionName function, AslField baseField, boolean distinct) {
        super(Number.class, null, baseField != null ? baseField.origin : null, null);
        this.function = function;
        this.baseField = baseField;
        this.distinct = distinct;
    }

    public AggregateFunctionName getFunction() {
        return function;
    }

    public AslField getBaseField() {
        return baseField;
    }

    @Override
    public AslQuery getOwner() {
        return baseField == null ? null : baseField.getOwner();
    }

    @Override
    public AslQuery getInternalProvider() {
        return baseField == null ? null : baseField.getInternalProvider();
    }

    @Override
    public AslQuery getProvider() {
        return baseField == null ? null : baseField.getProvider();
    }

    @Override
    public String aliasedName(String name) {
        return "agg_" + baseField.aliasedName(name);
    }

    @Override
    public AslField withProvider(AslQuery provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AslField copyWithOwner(AslQuery aslFilteringQuery) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AslField withOrigin(AslFieldOrigin origin) {
        throw new UnsupportedOperationException();
    }

    public boolean isDistinct() {
        return distinct;
    }
}
