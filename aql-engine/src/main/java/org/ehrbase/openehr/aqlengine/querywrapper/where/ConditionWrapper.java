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

import java.util.List;
import java.util.function.Function;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslAndQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFalseQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslOrQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition.AslConditionOperator;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslTrueQueryCondition;

public sealed interface ConditionWrapper permits ComparisonOperatorConditionWrapper, LogicalOperatorConditionWrapper {
    enum LogicalConditionOperator {
        AND(AslAndQueryCondition::new, AslTrueQueryCondition.class, AslFalseQueryCondition.class),
        OR(AslOrQueryCondition::new, AslFalseQueryCondition.class, AslTrueQueryCondition.class),
        NOT(l -> l.stream().findFirst().map(AslNotQueryCondition::new).orElse(null), Void.class, Void.class);

        LogicalConditionOperator(
                Function<List<AslQueryCondition>, AslQueryCondition> setOperator,
                Class<?> noopCondition,
                Class<?> shortCircuitCondition) {
            this.setOperator = setOperator;
            this.noopCondition = noopCondition;
            this.shortCircuitCondition = shortCircuitCondition;
        }

        private final Function<List<AslQueryCondition>, AslQueryCondition> setOperator;
        private final Class<?> noopCondition;
        private final Class<?> shortCircuitCondition;

        public AslQueryCondition build(List<AslQueryCondition> params) {
            return setOperator.apply(params);
        }

        public boolean filterNotNoop(AslQueryCondition condition) {
            return !noopCondition.isInstance(condition);
        }

        public boolean filterShortCircuit(AslQueryCondition condition) {
            return shortCircuitCondition.isInstance(condition);
        }
    }

    enum ComparisonConditionOperator {
        EXISTS(AslConditionOperator.IS_NOT_NULL),
        LIKE(AslConditionOperator.LIKE),
        MATCHES(AslConditionOperator.IN),
        EQ(AslConditionOperator.EQ),
        NEQ(AslConditionOperator.NEQ),
        GT_EQ(AslConditionOperator.GT_EQ),
        GT(AslConditionOperator.GT),
        LT_EQ(AslConditionOperator.LT_EQ),
        LT(AslConditionOperator.LT);

        private final AslConditionOperator aslOperator;

        ComparisonConditionOperator(AslConditionOperator aslOperator) {
            this.aslOperator = aslOperator;
        }

        public AslConditionOperator getAslOperator() {
            return aslOperator;
        }

        public ComparisonConditionOperator negate() {
            return switch (this) {
                case EXISTS, MATCHES, LIKE ->
                    throw new UnsupportedOperationException("No operator known to  represent negated " + this);
                case EQ -> NEQ;
                case NEQ -> EQ;
                case GT_EQ -> LT;
                case GT -> LT_EQ;
                case LT_EQ -> GT;
                case LT -> GT_EQ;
            };
        }

        public static ComparisonConditionOperator valueOf(String name, boolean negated) {
            ComparisonConditionOperator operator = valueOf(name);
            return negated ? operator.negate() : operator;
        }
    }
}
