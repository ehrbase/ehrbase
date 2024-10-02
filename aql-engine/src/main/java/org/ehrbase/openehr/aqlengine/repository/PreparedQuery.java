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
package org.ehrbase.openehr.aqlengine.repository;

import java.util.List;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.sql.postprocessor.AqlSqlResultPostprocessor;
import org.jooq.Record;
import org.jooq.SelectQuery;

/**
 * Represents a prepared but not executed SQL query for the {@link AqlQueryRepository} that is constructed by
 * {@link AqlQueryRepository#prepareQuery(AslRootQuery, List)}. This prepared query can be executed by
 * {@link AqlQueryRepository#executeQuery(PreparedQuery)} or can be used to obtain the raw SQL query using
 * {@link AqlQueryRepository#getQuerySql(PreparedQuery)}} or the query planer output
 * {@link AqlQueryRepository#explainQuery(boolean, PreparedQuery)}.
 */
public final class PreparedQuery {

    final SelectQuery<Record> selectQuery;
    final AqlSqlResultPostprocessor[] postProcessors;

    public PreparedQuery(SelectQuery<Record> selectQuery, AqlSqlResultPostprocessor[] postProcessors) {
        this.selectQuery = selectQuery;
        this.postProcessors = postProcessors;
    }

    @Override
    public String toString() {
        return selectQuery.getSQL();
    }
}
