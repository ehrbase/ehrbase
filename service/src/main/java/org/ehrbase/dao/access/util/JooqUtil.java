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
package org.ehrbase.dao.access.util;

import java.util.function.Function;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.jooq.Table;

public class JooqUtil {

    private JooqUtil() {}

    /**
     * Retrieves an entry from the database via <code>selectOp</code>.
     * If missing, it is added via <code>insertOp</code>.
     * The table <code>lockTable</code> is locked to prevent concurrent fom inserting duplicates.
     *
     * @param <R> type of returned entry
     * @param domainAccess
     * @param selectOp retrieves the entry from the database
     * @param lockTable table to be locked for the insert
     * @param insertOp inserts the entry
     * @return
     */
    public static <R> R retrieveOrCreate(
            I_DomainAccess domainAccess,
            Function<I_DomainAccess, R> selectOp,
            Table lockTable,
            Function<I_DomainAccess, R> insertOp) {
        // try to retrieve
        R res = selectOp.apply(domainAccess);

        if (res != null) {
            return res;
        }
        // separate ("requires new", not "nested") transaction so other callers will see result early, and to prevent
        // possible race conditions
        var ret = domainAccess
                .getContext()
                .transactionResultAsync(trx -> {

                    // Postgres specific locking
                    trx.dsl().execute("LOCK TABLE {0} IN SHARE ROW EXCLUSIVE MODE", lockTable);

                    // set jooq config from transaction
                    I_DomainAccess dataAccessWrapper =
                            new DataAccess(
                                    trx.dsl(),
                                    domainAccess.getKnowledgeManager(),
                                    domainAccess.getIntrospectService(),
                                    domainAccess.getServerConfig()) {
                                @Override
                                public DataAccess getDataAccess() {
                                    return this;
                                }
                            };
                    // double-checking
                    R newRes = selectOp.apply(dataAccessWrapper);
                    if (newRes == null) {
                        newRes = insertOp.apply(dataAccessWrapper);
                    }
                    return newRes;
                })
                .toCompletableFuture()
                .join();

        return ret;
    }
}
