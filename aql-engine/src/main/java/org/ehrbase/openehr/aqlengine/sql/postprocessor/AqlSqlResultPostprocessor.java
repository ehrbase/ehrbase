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
package org.ehrbase.openehr.aqlengine.sql.postprocessor;

/**
 * Applied to one column of all records returned by the SQL query executed for a given {@link org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery}.
 * Selection of the applicable post processor for each column is performed by {@link org.ehrbase.openehr.aqlengine.repository.AqlQueryRepository}
 */
@FunctionalInterface
public interface AqlSqlResultPostprocessor {

    Object postProcessColumn(Object columnValue);
}
