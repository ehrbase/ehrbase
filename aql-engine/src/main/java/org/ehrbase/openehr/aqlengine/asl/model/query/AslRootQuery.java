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
import java.util.List;
import java.util.Map;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslDvOrderedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslOrderByField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.jooq.SortOrder;

public final class AslRootQuery extends AslEncapsulatingQuery {

    private final List<AslField> fields = new ArrayList<>();

    private final List<AslOrderByField> orderByFields = new ArrayList<>();
    private final List<AslField> groupByFields = new ArrayList<>();
    private final List<AslField> groupByDvOrderedMagnitudeFields = new ArrayList<>();
    private Long limit;
    private Long offset;

    public AslRootQuery() {
        super(null, null);
    }

    public List<AslField> getSelect() {
        return fields;
    }

    /**
     * @return all field known to the subqueries
     */
    public List<AslField> getAvailableFields() {
        return super.getSelect();
    }

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public List<AslOrderByField> getOrderByFields() {
        return orderByFields;
    }

    @Override
    public Map<IdentifiedPath, List<AslPathFilterJoinCondition>> joinConditionsForFiltering() {
        throw new UnsupportedOperationException();
    }

    public List<AslField> getGroupByFields() {
        return groupByFields;
    }

    public List<AslField> getGroupByDvOrderedMagnitudeFields() {
        return groupByDvOrderedMagnitudeFields;
    }

    public void addOrderBy(AslField field, SortOrder sortOrder, boolean usesAggregateFunctionOrDistinct) {
        getOrderByFields().add(new AslOrderByField(field, sortOrder));

        field.fieldsForAggregation(this).forEach(f -> {
            if (usesAggregateFunctionOrDistinct && !getGroupByFields().contains(f)) {
                (switch (f) {
                            case AslDvOrderedColumnField __ -> getGroupByDvOrderedMagnitudeFields();
                            case AslRmPathField arpf -> arpf.getDvOrderedTypes().isEmpty()
                                    ? getGroupByFields()
                                    : getGroupByDvOrderedMagnitudeFields();
                            default -> getGroupByFields();
                        })
                        .add(f);
            }
        });
    }
}
