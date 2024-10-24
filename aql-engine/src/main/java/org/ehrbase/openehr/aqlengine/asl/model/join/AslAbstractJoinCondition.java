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

import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

public abstract sealed class AslAbstractJoinCondition implements AslJoinCondition
        permits AslDelegatingJoinCondition, AslPathFilterJoinCondition, AslFolderItemJoinCondition {
    protected AslQuery leftOwner;
    protected AslQuery rightOwner;

    public AslAbstractJoinCondition(AslQuery leftOwner, AslQuery rightOwner) {
        this.leftOwner = leftOwner;
        this.rightOwner = rightOwner;
    }

    @Override
    public AslQuery getLeftOwner() {
        return leftOwner;
    }

    @Override
    public AslQuery getRightOwner() {
        return rightOwner;
    }
}
