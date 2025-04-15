/*
 * Copyright (c) 2019-2025 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine.sql.provider;

import java.util.Optional;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslExternalQuery;
import org.jooq.Record;
import org.jooq.Table;

/**
 * A provider that is used to provide tables for {@link AslExternalQuery}
 */
@FunctionalInterface
public interface AqlSqlExternalTableProvider {

    /**
     * Provides the {@link Table} for the given {@link AslExternalQuery} or en empty Optional in cas eno table could be
     * found.
     *
     * @param aslExternalQuery to provide a {@link Table} for
     * @return optionalTable for the given {@link AslExternalQuery}
     */
    Optional<Table<Record>> tableForExternalQuery(AslExternalQuery aslExternalQuery);
}
