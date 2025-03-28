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
import java.util.function.UnaryOperator;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentVersionExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;

/**
 * Contains backtracking information for the original AQL query.
 */
public record AslQueryOrigin(List<AslTypeOrigin> typeOrigins) {

    /**
     * Factory method to create a new {@link AslQueryOrigin} for a single {@link AslTypeOrigin}
     *
     * @param aslTypeOrigin to create query origin for
     * @return origin
     */
    public static AslQueryOrigin ofType(AslTypeOrigin aslTypeOrigin) {
        return new AslQueryOrigin(List.of(aslTypeOrigin));
    }

    public AslQueryOrigin(List<AslTypeOrigin> typeOrigins) {
        this.typeOrigins = new ArrayList<>(typeOrigins);
    }

    public void addTypeOrigins(List<AslTypeOrigin> aslTypeOrigins) {
        typeOrigins.addAll(aslTypeOrigins);
    }

    public AslQueryOrigin copyWithFirstTypeOrigin(UnaryOperator<AslTypeOrigin> mappingFunction) {
        return new AslQueryOrigin(
                typeOrigins.stream().findFirst().map(mappingFunction).stream().toList());
    }

    /**
     * Add the given <code>paths</code> to an existing {@link AslTypeOrigin} or create a new one if needed
     * @param paths to add
     */
    public void addPaths(List<IdentifiedPath> paths) {

        paths.forEach(path -> {
            String alias = path.getRoot().getIdentifier();
            String rmType =
                    switch (path.getRoot()) {
                        case ContainmentClassExpression classExpression -> classExpression.getType();
                        case ContainmentVersionExpression versionExpression -> ((ContainmentClassExpression)
                                        versionExpression.getContains())
                                .getType();
                    };

            typeOrigins.stream()
                    .filter(to -> to.getRmType().equals(rmType) && to.getAlias().equals(alias))
                    .findFirst()
                    .orElseGet(() -> {
                        AslTypeOrigin.AslRmTypeOrigin aslRmTypeOrigin =
                                new AslTypeOrigin.AslRmTypeOrigin(alias, rmType, List.of());
                        typeOrigins.add(aslRmTypeOrigin);
                        return aslRmTypeOrigin;
                    })
                    .addPath(path);
        });
    }
}
