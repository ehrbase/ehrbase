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

import java.util.Collections;
import java.util.Set;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.jooq.JSONB;

public final class AslDvOrderedColumnField extends AslColumnField {

    private final Set<String> dvOrderedTypes;

    public AslDvOrderedColumnField(String columnName, FieldSource fieldSource, Set<String> dvOrderedTypes) {
        super(JSONB.class, columnName, fieldSource, false);
        this.dvOrderedTypes = Collections.unmodifiableSet(dvOrderedTypes);
    }

    public Set<String> getDvOrderedTypes() {
        return dvOrderedTypes;
    }

    @Override
    public AslDvOrderedColumnField withProvider(AslQuery provider) {
        return new AslDvOrderedColumnField(getColumnName(), fieldSource.withProvider(provider), dvOrderedTypes);
    }

    @Override
    public AslDvOrderedColumnField copyWithOwner(AslQuery owner) {
        return new AslDvOrderedColumnField(getColumnName(), FieldSource.withOwner(owner), dvOrderedTypes);
    }
}
