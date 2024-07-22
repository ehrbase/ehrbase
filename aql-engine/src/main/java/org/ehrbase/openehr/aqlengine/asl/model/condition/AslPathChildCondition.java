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

import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery.AslSourceRelation;

/**
 * For contains and path joins
 */
public final class AslPathChildCondition implements AslProvidesJoinCondition {
    private final AslSourceRelation parentRelation;
    private final AslSourceRelation childRelation;
    private final AslQuery leftProvider;
    private final AslQuery leftOwner;
    private final AslQuery rightProvider;
    private final AslQuery rightOwner;

    public AslPathChildCondition(
            AslSourceRelation parentRelation,
            AslQuery leftProvider,
            AslQuery leftOwner,
            AslSourceRelation childRelation,
            AslQuery rightProvider,
            AslQuery rightOwner) {
        this.parentRelation = parentRelation;
        this.leftProvider = leftProvider;
        this.leftOwner = leftOwner;
        this.childRelation = childRelation;
        this.rightProvider = rightProvider;
        this.rightOwner = rightOwner;
    }

    public AslSourceRelation getParentRelation() {
        return parentRelation;
    }

    public AslSourceRelation getChildRelation() {
        return childRelation;
    }

    @Override
    public AslQuery getLeftOwner() {
        return leftOwner;
    }

    @Override
    public AslQuery getRightOwner() {
        return rightOwner;
    }

    public AslQuery getLeftProvider() {
        return leftProvider;
    }

    public AslQuery getRightProvider() {
        return rightProvider;
    }
}
