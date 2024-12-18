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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslDvOrderedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField.FieldSource;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;

public final class AslFilteringQuery extends AslQuery {

    private final AslField sourceField;
    private final AslField select;

    public AslFilteringQuery(String alias, AslField sourceField) {
        super(alias, Collections.emptyList());
        this.sourceField = sourceField;
        if (sourceField instanceof AslRmPathField pf) {
            if (pf.getDvOrderedTypes().isEmpty()) {
                this.select = new AslColumnField(pf.getType(), pf.getSrcField().getColumnName(), false).withOwner(this);
            } else {
                this.select = new AslDvOrderedColumnField(
                        pf.getSrcField().getColumnName(), FieldSource.withOwner(this), pf.getDvOrderedTypes());
            }
        } else {
            this.select = sourceField.copyWithOwner(this);
        }
    }

    @Override
    public Map<IdentifiedPath, List<AslPathFilterJoinCondition>> joinConditionsForFiltering() {
        return Collections.emptyMap();
    }

    @Override
    public List<AslField> getSelect() {
        return List.of(select);
    }

    public AslField getSourceField() {
        return sourceField;
    }
}
