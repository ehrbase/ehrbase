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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

public final class AslOrQueryCondition implements AslQueryCondition {
    private final List<AslQueryCondition> operands;

    public AslOrQueryCondition(AslQueryCondition... conditions) {
        this.operands = Arrays.stream(conditions).collect(Collectors.toList());
    }

    public AslOrQueryCondition(List<AslQueryCondition> operands) {
        this.operands = operands;
    }

    public List<AslQueryCondition> getOperands() {
        return operands;
    }

    @Override
    public AslOrQueryCondition withProvider(AslQuery provider) {
        return new AslOrQueryCondition(operands.stream()
                .map(condition -> condition.withProvider(provider))
                .collect(Collectors.toList()));
    }
}
