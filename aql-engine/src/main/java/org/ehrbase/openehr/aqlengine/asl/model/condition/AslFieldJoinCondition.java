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

import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

public final class AslFieldJoinCondition implements AslProvidesJoinCondition {
    private final AslField leftField;
    private final AslConditionOperator operator;
    private final AslField rightField;

    public AslFieldJoinCondition(
            final AslField leftField, final AslConditionOperator operator, final AslField rightField) {
        this.leftField = leftField;
        this.rightField = rightField;
        this.operator = operator;
    }

    public AslField getLeftField() {
        return leftField;
    }

    public AslConditionOperator getOperator() {
        return operator;
    }

    public AslField getRightField() {
        return rightField;
    }

    @Override
    public AslQuery getLeftOwner() {
        return leftField.getOwner();
    }

    @Override
    public AslQuery getRightOwner() {
        return rightField.getOwner();
    }

    public AslQuery getLeftProvider() {
        return leftField.getProvider();
    }

    public AslQuery getRightProvider() {
        return rightField.getProvider();
    }
}
