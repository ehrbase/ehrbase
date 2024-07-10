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
package org.ehrbase.openehr.aqlengine.asl.model.field;

import java.util.List;
import java.util.stream.Stream;
import org.ehrbase.openehr.aqlengine.asl.AslUtils;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRmObjectDataQuery;

public final class AslSubqueryField extends AslField {

    private final AslQuery baseQuery;
    private final List<AslQueryCondition> filterConditions;

    private AslSubqueryField(Class<?> type, AslQuery baseQuery, List<AslQueryCondition> filterConditions) {
        super(type, null, null);
        this.baseQuery = baseQuery;
        this.filterConditions = filterConditions;
    }

    public static AslSubqueryField createAslSubqueryField(Class<?> type, AslQuery baseQuery) {
        return new AslSubqueryField(type, baseQuery, List.of());
    }

    public AslQuery getBaseQuery() {
        return baseQuery;
    }

    public List<AslQueryCondition> getFilterConditions() {
        return filterConditions;
    }

    @Override
    public AslQuery getOwner() {
        return null;
    }

    @Override
    public AslQuery getInternalProvider() {
        return null;
    }

    @Override
    public AslQuery getProvider() {
        return null;
    }

    @Override
    protected String aliasedName(String name) {
        throw new UnsupportedOperationException();
    }

    public String getAliasedName() {
        return baseQuery.getAlias();
    }

    @Override
    public AslField withProvider(AslQuery provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AslField copyWithOwner(AslQuery aslFilteringQuery) {
        throw new UnsupportedOperationException();
    }

    public AslSubqueryField withFilterConditions(List<AslJoinCondition> filterConditions) {
        List<AslQueryCondition> conditions = filterConditions.stream()
                .map(c -> switch (c) {
                    case AslPathFilterJoinCondition pfc -> pfc.getCondition();
                    default -> throw new IllegalArgumentException("Unsupported condition type: " + c.getClass());
                })
                .toList();

        return new AslSubqueryField(getType(), baseQuery, conditions);
    }

    @Override
    public Stream<AslField> fieldsForAggregation() {
        if (getBaseQuery() instanceof AslRmObjectDataQuery odq) {
            List<AslField> baseProviderFields = odq.getBaseProvider().getSelect();
            AslQuery base = odq.getBase();
            return Stream.concat(
                    Stream.of(
                            AslUtils.findFieldForOwner(AslStructureColumn.VO_ID, baseProviderFields, base),
                            AslUtils.findFieldForOwner(AslStructureColumn.ENTITY_IDX, baseProviderFields, base),
                            AslUtils.findFieldForOwner(AslStructureColumn.ENTITY_IDX_CAP, baseProviderFields, base)),
                    filterConditions.stream()
                            .flatMap(AslUtils::streamConditionFields)
                            .distinct());
        }

        return super.fieldsForAggregation();
    }
}
