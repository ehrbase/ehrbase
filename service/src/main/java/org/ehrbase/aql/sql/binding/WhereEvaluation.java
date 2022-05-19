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

import java.util.Arrays;
import java.util.List;

public class WhereEvaluation {

    private String[] operatorRequiringSQL = {"EXISTS"};

    List<Object> whereItems;

    public WhereEvaluation(List<Object> whereItems) {
        this.whereItems = whereItems;
    }

    public boolean requiresSQL() {

        List<String> operators = Arrays.asList(operatorRequiringSQL);

        for (Object item : whereItems) {
            if (item instanceof String) { // ignore variable definition
                String testItem = ((String) item).toUpperCase();
                if (operators.contains(testItem)) return true;
            }
        }
        return false;
    }
}
