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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;

public final class AslDvOrderedValueQueryCondition<T> extends AslFieldValueQueryCondition<T> {
    private final Set<String> typesToCompare;

    public AslDvOrderedValueQueryCondition(
            Set<String> typesToCompare, AslField field, AslConditionOperator operator, List<T> values) {
        super(field, operator, values);
        if (CollectionUtils.isEmpty(typesToCompare)) {
            throw new IllegalArgumentException("Affected DV_ORDERED types not specified");
        }
        this.typesToCompare = Collections.unmodifiableSet(typesToCompare);
    }

    public Set<String> getTypesToCompare() {
        return typesToCompare;
    }
}
