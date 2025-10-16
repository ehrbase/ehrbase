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

import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

public final class AslStringAggregationField extends AslVirtualField {

    private final AslColumnField baseField;
    private final String alias;
    private final String separator;

    public AslStringAggregationField(final AslColumnField baseField, final String alias, final String separator) {
        super(String.class, null, null);
        this.baseField = baseField;
        this.alias = alias;
        this.separator = separator;
    }

    @Override
    public AslQuery getOwner() {
        return baseField.getOwner();
    }

    @Override
    public AslQuery getInternalProvider() {
        return baseField.getInternalProvider();
    }

    @Override
    public AslQuery getProvider() {
        return baseField.getProvider();
    }

    @Override
    public String aliasedName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AslField withProvider(AslQuery provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AslField copyWithOwner(AslQuery aslFilteringQuery) {
        throw new UnsupportedOperationException();
    }

    public AslColumnField getBaseField() {
        return baseField;
    }

    public String alias() {
        return alias;
    }

    public String getSeparator() {
        return separator;
    }
}
