package org.ehrbase.dao.access.jooq;

import org.joda.time.format.DateTimeFormat;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;

import java.sql.SQLException;
import java.sql.Timestamp;

import static org.ehrbase.jooq.pg.Tables.STORED_QUERY;

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
           return new MockResult[]{};
        } else if (sql2.toUpperCase().contains("SELECT 1 AS \"ONE\"")) {
            Field<Integer> c = DSL.count();
            Result<Record1<Integer>> result2 = create.newResult(c);
            result2.add(create
                    .newRecord(c)
                    .values(1));
            mock[0] = new MockResult(1, result2);
            return mock;

        } else if (sql2.startsWith("select \"ehr\".\"stored_query\".\"reverse_domain_name\", \"ehr\".\"stored_query\".\"semantic_id\", \"ehr\".\"stored_query\".\"semver\", \"ehr\".\"stored_query\".\"query_text\", \"ehr\".\"stored_query\".\"creation_date\", \"ehr\".\"stored_query\".\"type\" from \"ehr\".\"stored_query\"")) {
            MockResult[] mock2 = new MockResult[1];
            Result<Record6<String, String, String, String, Timestamp, String>> result =
                    create.newResult(STORED_QUERY.REVERSE_DOMAIN_NAME, STORED_QUERY.SEMANTIC_ID, STORED_QUERY.SEMVER, STORED_QUERY.QUERY_TEXT, STORED_QUERY.CREATION_DATE, STORED_QUERY.TYPE);
            result.add(create.newRecord(STORED_QUERY.REVERSE_DOMAIN_NAME, STORED_QUERY.SEMANTIC_ID, STORED_QUERY.SEMVER, STORED_QUERY.QUERY_TEXT, STORED_QUERY.CREATION_DATE, STORED_QUERY.TYPE)
                    .values("org.example.departmentx.test", "diabetes-patient-overview", "1.0.2", "a_query",
                            new Timestamp(DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS").parseDateTime("2019-08-22 13:41:50.478").getMillis()),
                            "AQL"));
            mock2[0] = new MockResult(1, result);
            return mock2;


        } else {
            throw new SQLException("statement not mocked, add it for appropriate mocking");
        }
    }
}
