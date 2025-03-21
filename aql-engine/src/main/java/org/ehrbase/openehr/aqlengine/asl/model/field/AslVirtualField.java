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

public abstract sealed class AslVirtualField extends AslField
        permits AslAggregatingField, AslComplexExtractedColumnField, AslFolderItemIdVirtualField, AslRmPathField {

    public AslVirtualField(
            Class<?> type, FieldSource fieldSource, AslFieldOrigin origin, AslExtractedColumn extractedColumn) {
        super(type, fieldSource, origin, extractedColumn);
    }

    @Override
    public String aliasedName(String name) {
        return super.aliasedName(name);
    }
}
