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

public sealed class AslColumnField extends AslField permits AslDvOrderedColumnField {

    private final String columnName;
    private final Boolean versionTableField;

    public AslColumnField(Class<?> type, String columnName, Boolean versionTableField) {
        this(type, columnName, null, null, null, versionTableField);
    }

    public AslColumnField(Class<?> type, String columnName, FieldSource fieldSource, Boolean versionTableField) {
        this(type, columnName, fieldSource, null, null, versionTableField);
    }

    public AslColumnField(
            Class<?> type,
            String columnName,
            FieldSource fieldSource,
            AslExtractedColumn extractedColumn,
            Boolean versionTableField) {
        this(type, columnName, fieldSource, null, extractedColumn, versionTableField);
    }

    public AslColumnField(
            Class<?> type,
            String columnName,
            FieldSource fieldSource,
            AslFieldOrigin origin,
            AslExtractedColumn extractedColumn,
            Boolean versionTableField) {
        super(type, fieldSource, origin, extractedColumn);
        this.columnName = columnName;
        this.versionTableField = versionTableField;
    }

    public String getName(boolean aliased) {
        return aliased ? getAliasedName() : getColumnName();
    }

    public String getAliasedName() {
        return aliasedName(columnName);
    }

    public String getColumnName() {
        return columnName;
    }

    public boolean isVersionTableField() {
        return !Boolean.FALSE.equals(versionTableField);
    }

    public boolean isDataTableField() {
        return !Boolean.TRUE.equals(versionTableField);
    }

    @Override
    public AslColumnField withProvider(AslQuery provider) {
        return new AslColumnField(
                type, columnName, fieldSource.withProvider(provider), origin, getExtractedColumn(), versionTableField);
    }

    @Override
    public AslColumnField copyWithOwner(AslQuery owner) {
        return new AslColumnField(
                type, columnName, FieldSource.withOwner(owner), origin, getExtractedColumn(), versionTableField);
    }

    @Override
    public AslField withOrigin(AslFieldOrigin origin) {
        return new AslColumnField(type, columnName, fieldSource, origin, getExtractedColumn(), versionTableField);
    }
}
