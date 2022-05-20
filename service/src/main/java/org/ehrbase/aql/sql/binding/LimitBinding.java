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
package org.ehrbase.aql.sql.binding;

import org.jooq.SelectQuery;

/**
 * Created by christian on 4/27/2018.
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class LimitBinding {

    private SelectQuery selectQuery;
    private final Integer limitAttribute;
    private final Integer offsetAttribute;

    public LimitBinding(Integer limitAttribute, Integer offsetAttribute, SelectQuery selectQuery) {
        this.selectQuery = selectQuery;
        this.limitAttribute = limitAttribute;
        this.offsetAttribute = offsetAttribute;
    }

    public SelectQuery bind() {

        if (limitAttribute != null || offsetAttribute != null) {
            selectQuery.addLimit(
                    offsetAttribute == null ? 0 : offsetAttribute, limitAttribute == null ? 0 : limitAttribute);
        }
        return selectQuery;
    }
}
