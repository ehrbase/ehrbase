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

import org.ehrbase.openehr.aqlengine.asl.meta.AslFieldOrigin;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

public final class AslComplexExtractedColumnField extends AslVirtualField {

    public AslComplexExtractedColumnField(AslExtractedColumn extractedColumn, FieldSource fieldSource) {
        this(extractedColumn, fieldSource, null);
    }

    public AslComplexExtractedColumnField(
            AslExtractedColumn extractedColumn, FieldSource fieldSource, AslFieldOrigin origin) {
        super(extractedColumn.getColumnType(), fieldSource, origin, extractedColumn);
        this.extractedColumn = extractedColumn;
    }

    @Override
    public AslComplexExtractedColumnField withProvider(AslQuery provider) {
        return new AslComplexExtractedColumnField(extractedColumn, fieldSource.withProvider(provider), origin);
    }

    @Override
    public AslComplexExtractedColumnField copyWithOwner(AslQuery owner) {
        return new AslComplexExtractedColumnField(extractedColumn, FieldSource.withOwner(owner), origin);
    }

    @Override
    public AslField withOrigin(AslFieldOrigin origin) {
        return new AslComplexExtractedColumnField(extractedColumn, fieldSource, origin);
    }

    public static AslComplexExtractedColumnField archetypeNodeIdField(FieldSource fieldSource) {
        return new AslComplexExtractedColumnField(AslExtractedColumn.ARCHETYPE_NODE_ID, fieldSource, null);
    }

    public static AslComplexExtractedColumnField voIdField(FieldSource fieldSource) {
        return new AslComplexExtractedColumnField(AslExtractedColumn.VO_ID, fieldSource, null);
    }
}
