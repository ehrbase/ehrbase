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
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.openehr.aqlengine.asl.meta.AslQueryOrigin;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslDvOrderedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField.FieldSource;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;

public final class AslPathDataQuery extends AslDataQuery {
    public static final String DATA_COLUMN_NAME = "data";

    private final List<AqlObjectPath.PathNode> dataPath;
    private final AslColumnField dataField;
    private final boolean multipleValued;
    private final Set<String> dvOrderedTypes;

    public AslPathDataQuery(
            String alias,
            AslQueryOrigin origin,
            AslQuery base,
            AslQuery baseProvider,
            List<PathNode> dataPath,
            boolean multipleValued,
            Set<String> dvOrderedTypes,
            Class<?> fieldType) {
        super(alias, origin, base, baseProvider);
        this.dvOrderedTypes = Collections.unmodifiableSet(dvOrderedTypes);
        if (!(base instanceof AslStructureQuery || base instanceof AslPathDataQuery)) {
            throw new IllegalArgumentException(
                    "%s is not a valid base for AslPathDataQuery".formatted(base.getClass()));
        }
        this.dataPath = dataPath;
        FieldSource fieldSource = FieldSource.withOwner(this);
        this.dataField = CollectionUtils.isEmpty(dvOrderedTypes)
                ? new AslColumnField(fieldType, DATA_COLUMN_NAME, fieldSource, false)
                : new AslDvOrderedColumnField(DATA_COLUMN_NAME, fieldSource, dvOrderedTypes);
        this.multipleValued = multipleValued;
    }

    public AslColumnField getDataField() {
        return dataField;
    }

    @Override
    public Map<IdentifiedPath, List<AslPathFilterJoinCondition>> joinConditionsForFiltering() {
        return Collections.emptyMap();
    }

    @Override
    public List<AslField> getSelect() {
        return List.of(dataField);
    }

    public List<AqlObjectPath.PathNode> getPathNodes(AslColumnField field) {
        if (field != dataField) {
            throw new IllegalArgumentException("field is not part of this AslPathDataQuery");
        }
        return dataPath;
    }

    public boolean isMultipleValued() {
        return multipleValued;
    }

    public Set<String> getDvOrderedTypes() {
        return dvOrderedTypes;
    }
}
