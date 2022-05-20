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
package org.ehrbase.aql.definition;

import java.util.Iterator;
import org.ehrbase.aql.sql.binding.VariableDefinitions;

/**
 * Created by christian on 9/20/2017.
 */
public class Variables {

    private VariableDefinitions variableDefinitions;

    public Variables(VariableDefinitions variableDefinitions) {
        this.variableDefinitions = variableDefinitions;
    }

    public boolean hasDefinedDistinct() {
        Iterator<I_VariableDefinition> iterator = variableDefinitions.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isDistinct()) return true;
        }
        return false;
    }

    public boolean hasDefinedFunction() {
        Iterator<I_VariableDefinition> iterator = variableDefinitions.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isFunction()) return true;
        }
        return false;
    }
}
