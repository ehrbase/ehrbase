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
package org.ehrbase.openehr.aqlengine.querywrapper.contains;

import java.util.Collections;
import java.util.List;

public final class ContainsChain {
    private final List<ContainsWrapper> chain;
    private final ContainsSetOperationWrapper trailingSetOperation;

    public ContainsChain(List<ContainsWrapper> chain, ContainsSetOperationWrapper trailingSetOperation) {
        this.chain = Collections.unmodifiableList(chain);
        this.trailingSetOperation = trailingSetOperation;
    }

    public boolean hasTrailingSetOperation() {
        return trailingSetOperation != null;
    }

    public int size() {
        return chain.size() + (hasTrailingSetOperation() ? 1 : 0);
    }

    public List<ContainsWrapper> chain() {
        return chain;
    }

    public ContainsSetOperationWrapper trailingSetOperation() {
        return trailingSetOperation;
    }

    @Override
    public String toString() {
        return "ContainsChain[" + "chain=" + chain + ", " + "trailingSetOperation=" + trailingSetOperation + ']';
    }
}
