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
package org.ehrbase.openehr.aqlengine.querywrapper.orderby;

import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.orderby.OrderByExpression.OrderByDirection;

public final class OrderByWrapper {
    private final IdentifiedPath identifiedPath;
    private final OrderByDirection direction;
    private final ContainsWrapper root;

    public OrderByWrapper(IdentifiedPath identifiedPath, OrderByDirection direction, ContainsWrapper root) {
        this.identifiedPath = identifiedPath;
        this.direction = direction;
        this.root = root;
    }

    public IdentifiedPath identifiedPath() {
        return identifiedPath;
    }

    public OrderByDirection direction() {
        return direction;
    }

    public ContainsWrapper root() {
        return root;
    }

    @Override
    public String toString() {
        return "OrderByWrapper[" + "identifiedPath="
                + identifiedPath + ", " + "direction="
                + direction + ", " + "root="
                + root + ']';
    }
}
