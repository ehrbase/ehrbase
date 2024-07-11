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

import java.util.stream.Stream;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;

public abstract sealed class AslField permits AslColumnField, AslConstantField, AslSubqueryField, AslVirtualField {
    public record FieldSource(
            /**
             * The table that the fields originates from
             */
            AslQuery owner,
            /**
             * The table that provides the field to "provider"
             */
            AslQuery internalProvider,
            /**
             * The table that provides the field
             */
            AslQuery provider) {

        public static FieldSource withOwner(AslQuery owner) {
            return new FieldSource(owner, owner, owner);
        }

        public FieldSource withProvider(AslQuery newProvider) {
            return new FieldSource(owner, provider, newProvider);
        }
    }

    protected Class<?> type;
    protected FieldSource fieldSource;
    protected AslExtractedColumn extractedColumn;

    protected AslField(Class<?> type, FieldSource fieldSource, AslExtractedColumn extractedColumn) {
        this.type = type;
        this.fieldSource = fieldSource;
        this.extractedColumn = extractedColumn;
    }

    public Class<?> getType() {
        return type;
    }

    public AslQuery getOwner() {
        return fieldSource.owner();
    }

    public AslQuery getInternalProvider() {
        return fieldSource.internalProvider();
    }

    public AslQuery getProvider() {
        return fieldSource.provider();
    }

    public abstract AslField withProvider(AslQuery provider);

    public AslField withOwner(AslQuery owner) {
        if (fieldSource != null) {
            throw new IllegalArgumentException("fieldSource is already set");
        }
        return copyWithOwner(owner);
    }

    public AslExtractedColumn getExtractedColumn() {
        return extractedColumn;
    }

    protected String aliasedName(String name) {
        return fieldSource.owner().getAlias() + "_" + name;
    }

    public abstract AslField copyWithOwner(AslQuery aslFilteringQuery);

    public Stream<AslField> fieldsForAggregation(AslRootQuery rootQuery) {
        return Stream.of(this);
    }
}
