/*
 * Copyright (c) 2019-2025 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine.asl.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Contains backtracking information for the original AQL query.
 */
public class AslQueryOrigin {

    /**
     * Factory method to create a new {@link AslQueryOrigin} for a single {@link AslTypeOrigin}
     * @param aslTypeOrigin to create query origin for
     * @return origin
     */
    public static AslQueryOrigin ofType(AslTypeOrigin aslTypeOrigin) {
        return new AslQueryOrigin(List.of(aslTypeOrigin));
    }

    private final List<AslTypeOrigin> typeOrigins;

    public AslQueryOrigin(List<AslTypeOrigin> typeOrigins) {
        this.typeOrigins = new ArrayList<>(typeOrigins);
    }

    public List<AslTypeOrigin> getTypeOrigins() {
        return typeOrigins;
    }

    public void addTypeOrigins(List<AslTypeOrigin> aslTypeOrigins) {
        typeOrigins.addAll(aslTypeOrigins);
    }

    public AslQueryOrigin copyWithFirstTypeOrigin(Function<AslTypeOrigin, AslTypeOrigin> mappingFunction) {
        return new AslQueryOrigin(
                typeOrigins.stream().findFirst().map(mappingFunction).stream().toList());
    }
}
