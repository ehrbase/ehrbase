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
package org.ehrbase.openehr.aqlengine.asl.model.join;

import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

public final class AslPathFilterJoinCondition extends AslAbstractJoinCondition {

    private AslQueryCondition condition;

    public AslPathFilterJoinCondition(AslQuery leftOwner, AslQueryCondition condition) {
        super(leftOwner, null);
        this.condition = condition;
    }

    public AslQueryCondition getCondition() {
        return condition;
    }

    public void setCondition(AslQueryCondition condition) {
        this.condition = condition;
    }

    public AslPathFilterJoinCondition withLeftProvider(AslQuery provider) {
        return new AslPathFilterJoinCondition(leftOwner, condition.withProvider(provider));
    }
}
