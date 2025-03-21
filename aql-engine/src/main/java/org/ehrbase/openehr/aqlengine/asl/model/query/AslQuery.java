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
package org.ehrbase.openehr.aqlengine.asl.model.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.ehrbase.openehr.aqlengine.asl.meta.AslTypeOrigin;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslAndQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslOrQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;

public abstract sealed class AslQuery
        permits AslDataQuery, AslEncapsulatingQuery, AslFilteringQuery, AslStructureQuery {

    protected List<AslQueryCondition> structureConditions;
    private final String alias;
    private final AslTypeOrigin origin;
    private AslQueryCondition condition;

    protected AslQuery(String alias, AslTypeOrigin origin, List<AslQueryCondition> structureConditions) {
        this.alias = alias;
        this.origin = origin;
        this.structureConditions = structureConditions;
    }

    public abstract Map<IdentifiedPath, List<AslPathFilterJoinCondition>> joinConditionsForFiltering();

    public abstract List<AslField> getSelect();

    public String getAlias() {
        return alias;
    }

    public AslTypeOrigin getOrigin() {
        return origin;
    }

    public AslQueryCondition getCondition() {
        return condition;
    }

    public void setCondition(AslQueryCondition condition) {
        this.condition = condition;
    }

    public AslQuery addConditionAnd(AslQueryCondition toAdd) {
        if (this.condition == null) {
            this.condition = toAdd;
        } else if (this.condition instanceof AslAndQueryCondition and) {
            and.getOperands().add(toAdd);
        } else {
            this.condition = new AslAndQueryCondition(condition, toAdd);
        }
        return this;
    }

    public AslQuery addConditionOr(AslQueryCondition toAdd) {
        if (this.condition == null) {
            this.condition = toAdd;
        } else if (this.condition instanceof AslOrQueryCondition or) {
            or.getOperands().add(toAdd);
        } else {
            this.condition = new AslOrQueryCondition(condition, toAdd);
        }
        return this;
    }

    public List<AslQueryCondition> getStructureConditions() {
        return Collections.unmodifiableList(structureConditions);
    }
}
