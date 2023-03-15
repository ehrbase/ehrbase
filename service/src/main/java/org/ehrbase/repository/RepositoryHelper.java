/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.repository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.ehrbase.api.exception.InternalServerException;
import org.jooq.DSLContext;
import org.jooq.Loader;
import org.jooq.Record;
import org.jooq.Table;

/**
 * @author Stefan Spiska
 */
public class RepositoryHelper {

    private RepositoryHelper() {

        // Helper Class

    }

    public static <T extends Record> void executeBulkInsert(DSLContext context, List<T> recordList, Table<?> table) {

        try {
            Loader<?> execute = context.loadInto(table)
                    .bulkAfter(500)
                    .loadRecords(recordList)
                    .fields(table.fields())
                    .execute();

            if (!execute.result().errors().isEmpty()) {

                throw new InternalServerException(execute.result().errors().stream()
                        .map(e -> e.exception().getMessage())
                        .collect(Collectors.joining(";")));
            }
        } catch (IOException e) {
            throw new InternalServerException(e);
        }
    }
}
