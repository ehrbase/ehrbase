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
package org.ehrbase.openehr.aqlengine.querywrapper.contains;

import java.util.List;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;

public final class RmContainsWrapper implements ContainsWrapper {
    private final ContainmentClassExpression containment;

    public RmContainsWrapper(ContainmentClassExpression containment) {
        this.containment = containment;
    }

    public List<AndOperatorPredicate> getPredicate() {
        return containment.getPredicates();
    }

    public StructureRmType getStructureRmType() {
        return StructureRmType.byTypeName(containment.getType()).orElse(null);
    }

    @Override
    public String getRmType() {
        return StructureRmType.byTypeName(containment.getType())
                .map(StructureRmType::name)
                .orElse(containment.getType());
    }

    public String getIdentifier() {
        return containment.getIdentifier();
    }

    @Override
    public String alias() {
        return containment.getIdentifier();
    }

    public ContainmentClassExpression containment() {
        return containment;
    }

    @Override
    public String toString() {
        return "RmContainsWrapper[" + "containment=" + containment + ']';
    }
}
