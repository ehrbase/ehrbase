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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StoredQueryAccess;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.dao.access.util.SemVer;
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
        return DSL.using(connection, SQLDialect.POSTGRES);
    }

    @Test
    public void testRetrieveAqlText() {
        // assuming it does exist for now
        String qualifiedName = "org.example.departmentx.test::diabetes-patient-overview";

        I_StoredQueryAccess storedQueryAccess = StoredQueryAccess.retrieveQualified(
                        testDomainAccess, qualifiedName, SemVer.NO_VERSION)
                .orElseThrow(AssertionError::new);

        assertEquals("org.example.departmentx.test", storedQueryAccess.getReverseDomainName());
        assertEquals("diabetes-patient-overview", storedQueryAccess.getSemanticId());
        assertEquals("1.0.2", storedQueryAccess.getSemver());
        assertEquals("a_query", storedQueryAccess.getQueryText());
        assertEquals(
                new Timestamp(DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS")
                        .parseDateTime("2019-08-22 13:41:50.478")
                        .getMillis()),
                storedQueryAccess.getCreationDate());
    }

    @Test
    @Ignore
    public void testCreate() {
        // assuming it does exist for now
        String qualifiedName = "org.example.departmentx.test::diabetes-patient-overview";
        String queryText = "a_query";

        I_StoredQueryAccess storedQueryAccess = new StoredQueryAccess(
                        testDomainAccess,
                        qualifiedName,
                        SemVer.NO_VERSION,
                        queryText,
                        TenantAuthentication.DEFAULT_SYS_TENANT)
                .commit();

        assertNotNull(storedQueryAccess);

        assertEquals("org.example.departmentx.test", storedQueryAccess.getReverseDomainName());
        assertEquals("diabetes-patient-overview", storedQueryAccess.getSemanticId());
        assertEquals("1.0.2", storedQueryAccess.getSemver());
        assertEquals("a_query", storedQueryAccess.getQueryText());
        assertEquals(
                new Timestamp(DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS")
                        .parseDateTime("2019-08-22 13:41:50.478")
                        .getMillis()),
                storedQueryAccess.getCreationDate());
    }
}
