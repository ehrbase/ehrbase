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
package org.ehrbase.dao.access.jooq;

import static org.ehrbase.jooq.pg.Tables.STORED_QUERY;

import java.sql.SQLException;
import java.sql.Timestamp;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.joda.time.format.DateTimeFormat;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record7;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;

public class StoredQueryAccessTestMockDataProvider implements MockDataProvider {

    @Override
    public MockResult[] execute(MockExecuteContext ctx) throws SQLException {

        DSLContext create = DSL.using(SQLDialect.POSTGRES);
        MockResult[] mock = new MockResult[1];
        String sql2 = ctx.sql();

        if (sql2.toUpperCase().startsWith("DROP")) {
            throw new SQLException("Statement not supported: " + sql2);
        } else if (sql2.toUpperCase().startsWith("UPDATE")) {
            throw new SQLException("Statement not supported: " + sql2);
        } else if (sql2.toUpperCase().startsWith("INSERT")) {
            return new MockResult[] {};
        } else if (sql2.toUpperCase().contains("SELECT 1 AS \"ONE\"")) {
            Field<Integer> c = DSL.count();
            Result<Record1<Integer>> result2 = create.newResult(c);
            result2.add(create.newRecord(c).values(1));
            mock[0] = new MockResult(1, result2);
            return mock;

        } else if (sql2.startsWith(
                "select \"ehr\".\"stored_query\".\"reverse_domain_name\", \"ehr\".\"stored_query\".\"semantic_id\", \"ehr\".\"stored_query\".\"semver\", \"ehr\".\"stored_query\".\"query_text\", \"ehr\".\"stored_query\".\"creation_date\", \"ehr\".\"stored_query\".\"type\", \"ehr\".\"stored_query\".\"sys_tenant\" from \"ehr\".\"stored_query\"")) {
            MockResult[] mock2 = new MockResult[1];
            Result<Record7<String, String, String, String, Timestamp, String, Short>> result = create.newResult(
                    STORED_QUERY.REVERSE_DOMAIN_NAME,
                    STORED_QUERY.SEMANTIC_ID,
                    STORED_QUERY.SEMVER,
                    STORED_QUERY.QUERY_TEXT,
                    STORED_QUERY.CREATION_DATE,
                    STORED_QUERY.TYPE,
                    STORED_QUERY.SYS_TENANT);
            result.add(create.newRecord(
                            STORED_QUERY.REVERSE_DOMAIN_NAME,
                            STORED_QUERY.SEMANTIC_ID,
                            STORED_QUERY.SEMVER,
                            STORED_QUERY.QUERY_TEXT,
                            STORED_QUERY.CREATION_DATE,
                            STORED_QUERY.TYPE,
                            STORED_QUERY.SYS_TENANT)
                    .values(
                            "org.example.departmentx.test",
                            "diabetes-patient-overview",
                            "1.0.2",
                            "a_query",
                            new Timestamp(DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS")
                                    .parseDateTime("2019-08-22 13:41:50.478")
                                    .getMillis()),
                            "AQL",
                            TenantAuthentication.DEFAULT_SYS_TENANT));
            mock2[0] = new MockResult(1, result);
            return mock2;

        } else {
            throw new SQLException("statement not mocked, add it for appropriate mocking");
        }
    }
}
