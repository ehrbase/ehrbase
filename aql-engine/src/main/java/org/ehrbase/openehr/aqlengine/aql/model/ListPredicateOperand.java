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
package org.ehrbase.openehr.aqlengine.aql.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ehrbase.openehr.sdk.aql.dto.operand.PathPredicateOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.util.Freezable;

/**
 * This class is used to represent a MATCHES where condition as CONTAINS predicate.
 * Using it in other places will require further implementation.
 * @param <T>
 */
public class ListPredicateOperand<T extends Primitive> implements PathPredicateOperand<ListPredicateOperand<T>> {

    protected boolean frozen = false;
    private List<T> values;

    public ListPredicateOperand(List<T> values) {
        this.values = values;
    }

    public List<T> getValues() {
        return values;
    }

    public void setValues(final List<T> values) {
        this.values = values;
    }

    @Override
    public void render(final StringBuilder sb) {
        sb.append('{');
        for (int i = 0; i < values.size(); i++) {
            if (i != 0) {
                sb.append(" | ");
            }
            values.get(i).render(sb);
        }
        sb.append('}');
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public ListPredicateOperand<T> thawed() {
        return new ListPredicateOperand<>(new ArrayList<>(values));
    }

    @Override
    public ListPredicateOperand<T> frozen() {
        return Freezable.frozen(this, t -> {
            ListPredicateOperand<T> clone = clone();
            clone.frozen = true;
            clone.values = Collections.unmodifiableList(values);
            return clone;
        });
    }

    @Override
    public ListPredicateOperand<T> clone() {
        return Freezable.clone(this, ListPredicateOperand::thawed);
    }
}
