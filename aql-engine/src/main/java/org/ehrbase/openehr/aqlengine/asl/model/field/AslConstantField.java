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

import java.lang.constant.Constable;
import org.ehrbase.openehr.aqlengine.asl.meta.AslFieldOrigin;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;

public final class AslConstantField<T extends Constable> extends AslField {
    private final T value;

    public AslConstantField(Class<T> type, T value, FieldSource fieldSource, AslExtractedColumn extractedColumn) {
        this(type, value, fieldSource, null, extractedColumn);
    }

    public AslConstantField(
            Class<T> type,
            T value,
            FieldSource fieldSource,
            AslFieldOrigin origin,
            AslExtractedColumn extractedColumn) {
        super(type, fieldSource, origin, extractedColumn);
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AslConstantField<T> withProvider(AslQuery provider) {
        return new AslConstantField<>(
                (Class<T>) type, value, fieldSource.withProvider(provider), origin, getExtractedColumn());
    }

    @SuppressWarnings("unchecked")
    @Override
    public AslConstantField<T> copyWithOwner(AslQuery owner) {
        return new AslConstantField<>(
                (Class<T>) type, value, FieldSource.withOwner(owner), origin, getExtractedColumn());
    }

    @SuppressWarnings("unchecked")
    @Override
    public AslField withOrigin(AslFieldOrigin origin) {
        return new AslConstantField<>((Class<T>) type, value, fieldSource, origin, getExtractedColumn());
    }
}
