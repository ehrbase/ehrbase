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

/**
 * This condition is used to make sure a left-joined subquery is not empty,
 * by checking that a field based on a column with a NOT NULL constraint (i.e. COMP.VO_ID) is not null.
 */
public final class AslNotNullQueryCondition implements AslQueryCondition {
    private final AslField field;

    public AslNotNullQueryCondition(AslField field) {
        this.field = field;
    }

    public AslField getField() {
        return field;
    }

    @Override
    public AslNotNullQueryCondition withProvider(AslQuery provider) {
        return new AslNotNullQueryCondition(field.withProvider(provider));
    }
}
