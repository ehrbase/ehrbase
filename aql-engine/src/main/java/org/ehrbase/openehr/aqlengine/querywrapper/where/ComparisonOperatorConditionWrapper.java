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
package org.ehrbase.openehr.aqlengine.querywrapper.where;

import java.util.Collections;
import java.util.List;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;

public final class ComparisonOperatorConditionWrapper implements ConditionWrapper {
    private final IdentifiedPathWrapper leftComparisonOperand;
    private final ComparisonConditionOperator operator;
    private final List<Primitive> rightComparisonOperands;

    public ComparisonOperatorConditionWrapper(
            IdentifiedPathWrapper leftComparisonOperand,
            ComparisonConditionOperator operator,
            List<Primitive> rightComparisonOperands) {
        this.leftComparisonOperand = leftComparisonOperand;
        this.operator = operator;
        this.rightComparisonOperands = Collections.unmodifiableList(rightComparisonOperands);
    }

    public ComparisonOperatorConditionWrapper(
            IdentifiedPathWrapper leftComparisonOperand,
            ComparisonConditionOperator operator,
            Primitive rightComparisonOperand) {
        this(leftComparisonOperand, operator, List.of(rightComparisonOperand));
    }

    public IdentifiedPathWrapper leftComparisonOperand() {
        return leftComparisonOperand;
    }

    public ComparisonConditionOperator operator() {
        return operator;
    }

    public List<Primitive> rightComparisonOperands() {
        return rightComparisonOperands;
    }

    @Override
    public String toString() {
        return "ComparisonOperatorConditionWrapper[" + "leftComparisonOperand="
                + leftComparisonOperand + ", " + "operator="
                + operator + ", " + "rightComparisonOperands="
                + rightComparisonOperands + ']';
    }

    public record IdentifiedPathWrapper(ContainsWrapper root, IdentifiedPath path) {
        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
