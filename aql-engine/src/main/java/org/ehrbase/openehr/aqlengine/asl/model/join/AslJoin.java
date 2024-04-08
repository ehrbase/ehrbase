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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.jooq.JoinType;

public class AslJoin {
    private final AslQuery left;
    private final JoinType joinType;
    private final AslQuery right;
    private final List<AslJoinCondition> on;

    public AslJoin(AslQuery left, JoinType joinType, AslQuery right, List<AslJoinCondition> on) {
        this.left = left;
        this.joinType = joinType;
        this.right = right;
        this.on = new ArrayList<>(on);
    }

    public AslJoin(AslQuery left, JoinType joinType, AslQuery right, AslJoinCondition... on) {
        this(left, joinType, right, Arrays.asList(on));
    }

    public AslQuery getLeft() {
        return left;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public AslQuery getRight() {
        return right;
    }

    public List<AslJoinCondition> getOn() {
        return on;
    }
}
