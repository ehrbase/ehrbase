/*
 * Copyright (c) 2025 vitasystems GmbH.
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

import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

public final class AslCoalesceJoinCondition implements AslProvidesJoinCondition {
    private final AslFieldFieldQueryCondition ternaryCondition;
    private final boolean defaultValue;

    public AslCoalesceJoinCondition(final AslFieldFieldQueryCondition ternaryCondition, boolean defaultValue) {
        this.ternaryCondition = ternaryCondition;
        this.defaultValue = defaultValue;
    }

    @Override
    public AslQuery getLeftOwner() {
        return ternaryCondition.getLeftOwner();
    }

    @Override
    public AslQuery getRightOwner() {
        return ternaryCondition.getRightOwner();
    }

    @Override
    public AslQuery getLeftProvider() {
        return ternaryCondition.getLeftProvider();
    }

    @Override
    public AslQuery getRightProvider() {
        return ternaryCondition.getRightProvider();
    }

    public AslFieldFieldQueryCondition getTernaryCondition() {
        return ternaryCondition;
    }

    public boolean isDefaultValue() {
        return defaultValue;
    }
}
