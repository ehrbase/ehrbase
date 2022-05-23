/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.compiler;

import org.ehrbase.aql.definition.I_VariableDefinition;

/**
 * Representation of the ORDER BY clause in an AQL query
 * Created by christian on 5/26/2016.
 */
public class OrderAttribute {
    public enum OrderDirection {
        ASC,
        DESC
    }

    private OrderDirection direction;
    private I_VariableDefinition variableDefinition;

    public OrderAttribute(I_VariableDefinition variableDefinition) {
        this.variableDefinition = variableDefinition;
    }

    public void setDirection(OrderDirection direction) {
        this.direction = direction;
    }

    public OrderDirection getDirection() {
        return direction;
    }

    public I_VariableDefinition getVariableDefinition() {
        return variableDefinition;
    }
}
