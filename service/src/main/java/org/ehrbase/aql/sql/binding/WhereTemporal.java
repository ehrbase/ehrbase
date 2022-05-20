/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.binding;

import java.util.List;
import org.ehrbase.aql.definition.VariableDefinition;

/**
 * check if a where variable represents a temporal object. This is used to apply proper type casting and
 * relevant operator using EPOCH_OFFSET instead of string value when dealing with date/time comparison in
 * json structure
 */
public class WhereTemporal {
    List<Object> whereItems;

    public WhereTemporal(List<Object> whereItems) {
        this.whereItems = whereItems;
    }

    public boolean containsTemporalItem(VariableDefinition variableDefinition) {

        // get the index of variable definition in item list
        int pos = whereItems.indexOf(variableDefinition);

        for (Object item : whereItems.subList(pos, whereItems.size())) {

            if (item instanceof String
                    && new DateTimes((String) item).isDateTimeZoned()) { // ignore variable definition
                return true;
            }
        }
        return false;
    }
}
