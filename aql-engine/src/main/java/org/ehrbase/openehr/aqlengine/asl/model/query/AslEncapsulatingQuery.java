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
package org.ehrbase.openehr.aqlengine.asl.model.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoin;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;

public sealed class AslEncapsulatingQuery extends AslQuery permits AslRootQuery {
    private final List<Pair<AslQuery, AslJoin>> children = new ArrayList<>();

    public AslEncapsulatingQuery(String alias) {
        super(alias, new ArrayList<>());
    }

    public List<Pair<AslQuery, AslJoin>> getChildren() {
        return children;
    }

    public Pair<AslQuery, AslJoin> getLastChild() {
        if (this.children.isEmpty()) {
            return null;
        }
        return this.children.get(this.children.size() - 1);
    }

    public void addChild(AslQuery child, AslJoin join) {
        this.children.add(Pair.of(child, join));
    }

    @Override
    public Map<IdentifiedPath, List<AslPathFilterJoinCondition>> joinConditionsForFiltering() {
        return children.stream()
                .map(Pair::getLeft)
                .map(AslQuery::joinConditionsForFiltering)
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .map(e -> Pair.of(
                        e.getKey(),
                        e.getValue().stream()
                                .map(jc -> jc.withLeftProvider(this))
                                .toList()))
                .collect(Collectors.groupingBy(
                        Pair::getKey,
                        LinkedHashMap::new,
                        Collectors.flatMapping(e -> e.getValue().stream(), Collectors.toList())));
    }

    @Override
    public List<AslField> getSelect() {
        return children.stream()
                .map(Pair::getLeft)
                .map(AslQuery::getSelect)
                .flatMap(List::stream)
                .map(f -> f.withProvider(this))
                .toList();
    }

    public void addStructureCondition(AslQueryCondition condition) {
        this.structureConditions.add(condition);
    }
}
