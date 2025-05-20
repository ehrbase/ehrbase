/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery.AslSourceRelation;

/**
 * Specialized Join condition used to <code>COMPOSITION</code>s by <code>FOLDER.items[].id.value</code>
 */
public final class AslFolderItemJoinCondition extends AslAbstractJoinCondition {
    private final AslQuery leftProvider;
    private final AslSourceRelation descendantRelation;
    private final AslQuery rightProvider;

    public AslFolderItemJoinCondition(
            AslQuery leftProvider,
            AslQuery leftOwner,
            AslSourceRelation descendantRelation,
            AslQuery rightProvider,
            AslQuery rightOwner) {
        super(leftOwner, rightOwner);
        this.leftProvider = leftProvider;
        this.descendantRelation = descendantRelation;
        this.rightProvider = rightProvider;
    }

    public AslQuery getLeftProvider() {
        return leftProvider;
    }

    public AslSourceRelation descendantRelation() {
        return descendantRelation;
    }

    public AslQuery rightProvider() {
        return rightProvider;
    }
}
