package org.ehrbase.dao.access.jooq;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StoredQueryAccess;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.joda.time.format.DateTimeFormat;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Timestamp;

import static org.junit.Assert.*;

public class StoredQueryAccessTest {

    protected I_DomainAccess testDomainAccess;
    protected DSLContext context;
    protected I_KnowledgeCache knowledge;

    @Before
    public void beforeClass() {
        /*DSLContext*/
        context = getMockingContext();

        try {
            testDomainAccess = new DummyDataAccess(context, null, null, KnowledgeCacheHelper.buildServerConfig());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DSLContext getMockingContext() {
        // Initialize  data provider
        StoredQueryAccessTestMockDataProvider provider = new StoredQueryAccessTestMockDataProvider();
        MockConnection connection = new MockConnection(provider);
        // Pass the mock connection to a jOOQ DSLContext:
        return DSL.using(connection, SQLDialect.POSTGRES_9_5);
    }

    @Test
    public void testRetrieveAqlText() {
//        StoredQueryAccess storedQueryAccess = new StoredQueryAccess(testDomainAccess);

        //assuming it does exist for now
        String qualifiedName = "org.example.departmentx.test::diabetes-patient-overview";

        I_StoredQueryAccess storedQueryAccess = StoredQueryAccess.retrieveQualified(testDomainAccess, qualifiedName);

        assertNotNull(storedQueryAccess);

        assertEquals("org.example.departmentx.test::diabetes-patient-overview/1.0.2", storedQueryAccess.getQualifiedName());
        assertEquals("a_query", storedQueryAccess.getQueryText());
        assertEquals(new Timestamp(DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS").parseDateTime("2019-08-22 13:41:50.478").getMillis()), storedQueryAccess.getCreationDate());
    }

    @Test
    @Ignore
    public void testCreate() {
//        StoredQueryAccess storedQueryAccess = new StoredQueryAccess(testDomainAccess);

        //assuming it does exist for now
        String qualifiedName = "org.example.departmentx.test::diabetes-patient-overview";
        String queryText = "a_query";

        I_StoredQueryAccess storedQueryAccess = new StoredQueryAccess(testDomainAccess, qualifiedName, queryText).commit();

        assertNotNull(storedQueryAccess);

        assertEquals("org.example.departmentx.test::diabetes-patient-overview/1.0.2", storedQueryAccess.getQualifiedName());
        assertEquals("a_query", storedQueryAccess.getQueryText());
        assertEquals(new Timestamp(DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS").parseDateTime("2019-08-22 13:41:50.478").getMillis()), storedQueryAccess.getCreationDate());
    }

}