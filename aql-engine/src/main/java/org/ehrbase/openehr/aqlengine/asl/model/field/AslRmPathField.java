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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.ehrbase.openehr.aqlengine.asl.meta.AslFieldOrigin;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;

public final class AslRmPathField extends AslVirtualField {

    private final AslColumnField srcField;
    private final List<PathNode> pathInJson;
    private final Set<String> dvOrderedTypes;

    public AslRmPathField(
            AslColumnField srcField, List<PathNode> pathInJson, Set<String> dvOrderedTypes, Class<?> type) {
        this(srcField, null, pathInJson, dvOrderedTypes, type);
    }

    public AslRmPathField(
            AslColumnField srcField,
            AslFieldOrigin origin,
            List<PathNode> pathInJson,
            Set<String> dvOrderedTypes,
            Class<?> type) {
        super(type, srcField.fieldSource, origin, null);
        this.srcField = srcField;
        this.pathInJson = new ArrayList<>(pathInJson);
        this.dvOrderedTypes = dvOrderedTypes;
    }

    @Override
    public AslField withProvider(final AslQuery provider) {
        return new AslRmPathField(srcField.withProvider(provider), origin, pathInJson, dvOrderedTypes, type);
    }

    @Override
    public AslField copyWithOwner(final AslQuery aslFilteringQuery) {
        return new AslRmPathField(srcField.copyWithOwner(aslFilteringQuery), origin, pathInJson, dvOrderedTypes, type);
    }

    @Override
    public AslField withOrigin(AslFieldOrigin origin) {
        return new AslRmPathField(srcField, origin, pathInJson, dvOrderedTypes, type);
    }

    public AslColumnField getSrcField() {
        return srcField;
    }

    public List<PathNode> getPathInJson() {
        return pathInJson;
    }

    public Set<String> getDvOrderedTypes() {
        return dvOrderedTypes;
    }
}
