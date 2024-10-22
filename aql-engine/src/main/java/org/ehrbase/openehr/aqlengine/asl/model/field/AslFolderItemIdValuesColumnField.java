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
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

/**
 * A virtual column field representing a <code>FOLDER.items[].id.value</code> path.
 */
public final class AslFolderItemIdValuesColumnField extends AslVirtualField {

    private static final String FIELD_NAME = "item_id_value";

    public AslFolderItemIdValuesColumnField() {
        this(null);
    }

    public AslFolderItemIdValuesColumnField(FieldSource fieldSource) {
        super(UUID[].class, fieldSource, null);
    }

    @Override
    public AslFolderItemIdValuesColumnField withProvider(AslQuery provider) {
        return new AslFolderItemIdValuesColumnField(fieldSource.withProvider(provider));
    }

    @Override
    public AslFolderItemIdValuesColumnField copyWithOwner(AslQuery owner) {
        return new AslFolderItemIdValuesColumnField(FieldSource.withOwner(owner));
    }

    public String getFieldName() {
        return FIELD_NAME;
    }

    public String aliasedName() {
        return super.aliasedName(FIELD_NAME);
    }
}
