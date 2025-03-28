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
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;

/**
 * An {@link AslQuery} that is not necessary based on some RM-Type object and need tables provided by a
 * {@link org.ehrbase.openehr.aqlengine.sql.provider.AqlSqlExternalTableProvider}.
 */
public abstract non-sealed class AslExternalQuery extends AslQuery {

    private final List<AslField> fields = new ArrayList<>();

    protected AslExternalQuery(String alias, List<? extends AslField> fields) {
        super(alias, null, new ArrayList<>());
        fields.forEach(this::addField);
    }

    @Override
    public List<AslField> getSelect() {
        return fields;
    }

    public <T extends AslField> T addField(T aslField) {
        @SuppressWarnings("unchecked")
        T fieldWithOwner = (T) aslField.withOwner(this);
        fields.add(fieldWithOwner);
        return fieldWithOwner;
    }

    @Override
    public Map<IdentifiedPath, List<AslPathFilterJoinCondition>> joinConditionsForFiltering() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "AslProvidedQuery[" + getAlias() + "]";
    }
}
