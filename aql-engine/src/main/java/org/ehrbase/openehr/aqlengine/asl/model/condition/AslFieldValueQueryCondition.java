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
package org.ehrbase.openehr.aqlengine.asl.model.condition;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.ListUtils;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

public sealed class AslFieldValueQueryCondition<T> implements AslQueryCondition
        permits AslDvOrderedValueQueryCondition {

    private final AslField field;
    private final AslConditionOperator operator;
    private final List<T> values;

    public AslFieldValueQueryCondition(AslField field, AslConditionOperator operator, List<T> values) {
        this.field = field;
        this.operator = operator;
        this.values = ListUtils.emptyIfNull(values);
    }

    public AslField getField() {
        return field;
    }

    public AslConditionOperator getOperator() {
        return operator;
    }

    public List<T> getValues() {
        return values;
    }

    @Override
    public AslFieldValueQueryCondition<T> withProvider(AslQuery provider) {
        return new AslFieldValueQueryCondition<>(field.withProvider(provider), operator, new ArrayList<>(values));
    }
}
