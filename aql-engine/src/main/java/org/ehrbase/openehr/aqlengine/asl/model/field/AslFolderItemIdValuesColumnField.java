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

import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

/**
 * A virtual column field representing a <code>FOLDER.items[].id.value</code> path.
 */
public final class AslFolderItemIdValuesColumnField extends AslVirtualField {

    private final String columnName;

    public AslFolderItemIdValuesColumnField() {
        this(AslExtractedColumn.FOLDER_ITEM_ID, null, "items_id_value");
    }

    public AslFolderItemIdValuesColumnField(
            AslExtractedColumn extractedColumn, FieldSource fieldSource, String columnName) {
        super(extractedColumn.getColumnType(), fieldSource, extractedColumn);
        this.extractedColumn = extractedColumn;
        this.columnName = columnName;
    }

    @Override
    public AslFolderItemIdValuesColumnField withProvider(AslQuery provider) {
        return new AslFolderItemIdValuesColumnField(extractedColumn, fieldSource.withProvider(provider), columnName);
    }

    @Override
    public AslFolderItemIdValuesColumnField copyWithOwner(AslQuery owner) {
        return new AslFolderItemIdValuesColumnField(extractedColumn, FieldSource.withOwner(owner), columnName);
    }

    public String getColumnName() {
        return columnName;
    }

    public String aliasedName() {
        return super.aliasedName(columnName);
    }
}
