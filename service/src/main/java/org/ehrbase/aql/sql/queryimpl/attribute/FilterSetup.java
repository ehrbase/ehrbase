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
package org.ehrbase.aql.sql.queryimpl.attribute;

/**
 * maintain the state of specific filter to apply in WHERE clause
 */
public class FilterSetup {
    private boolean ehrIdFiltered = false; // true if the query specifies the ehr id (in the AQL FROM clause)
    private boolean compositionIdFiltered =
            false; // true if the query contains a where clause with composition id specified

    public boolean isEhrIdFiltered() {
        return ehrIdFiltered;
    }

    public void setEhrIdFiltered(boolean ehrIdFiltered) {
        this.ehrIdFiltered = ehrIdFiltered;
    }

    public boolean isCompositionIdFiltered() {
        return compositionIdFiltered;
    }

    public void setCompositionIdFiltered(boolean compositionIdFiltered) {
        this.compositionIdFiltered = compositionIdFiltered;
    }
}
