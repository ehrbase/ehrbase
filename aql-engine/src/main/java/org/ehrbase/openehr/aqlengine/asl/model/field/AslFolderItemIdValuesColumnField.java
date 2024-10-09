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

import java.util.UUID;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

/**
 * A virtual column field representing a <code>FOLDER.items[].id.value</code> path.
 */
public final class AslFolderItemIdValuesColumnField extends AslVirtualField {

    private final String columnName;

    private final String rmType;

    private final String idType;

    public AslFolderItemIdValuesColumnField() {
        this(AslExtractedColumn.FOLDER_ITEM_ID, null, "item_id_value", "VERSIONED_COMPOSITION", "HIER_OBJECT_ID");
    }

    public AslFolderItemIdValuesColumnField(
            AslExtractedColumn extractedColumn,
            FieldSource fieldSource,
            String columnName,
            String rmType,
            String idType) {
        super(UUID[].class, fieldSource, extractedColumn);
        this.extractedColumn = extractedColumn;
        this.columnName = columnName;
        this.rmType = rmType;
        this.idType = idType;
    }

    @Override
    public AslFolderItemIdValuesColumnField withProvider(AslQuery provider) {
        return new AslFolderItemIdValuesColumnField(
                extractedColumn, fieldSource.withProvider(provider), columnName, rmType, idType);
    }

    @Override
    public AslFolderItemIdValuesColumnField copyWithOwner(AslQuery owner) {
        return new AslFolderItemIdValuesColumnField(
                extractedColumn, FieldSource.withOwner(owner), columnName, rmType, idType);
    }

    public String getColumnName() {
        return columnName;
    }

    public String aliasedName() {
        return super.aliasedName(columnName);
    }

    public String getRmType() {
        return rmType;
    }

    public String getIdType() {
        return idType;
    }
}
