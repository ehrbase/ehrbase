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
package org.ehrbase.dao.access.jooq;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.UUID;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.junit.Before;
import org.junit.Test;

/***
 *@Created by Luis Marco-Ruiz on Jun 13, 2019
 */
public class FolderHistoryAccessTest {
    protected I_DomainAccess testDomainAccess;
    protected DSLContext context;
    protected I_KnowledgeCache knowledge;
    private String tenantId;

    @Before
    public void beforeClass() {
        /*DSLContext*/
        context = getMockingContext();
        tenantId = UUID.randomUUID().toString();

        try {
            testDomainAccess = new DummyDataAccess(context, null, null, KnowledgeCacheHelper.buildServerConfig());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DSLContext getMockingContext() {
        // Initialize  data provider
        FolderAccessHistoryMockDataProvider provider = new FolderAccessHistoryMockDataProvider();
        MockConnection connection = new MockConnection(provider);
        // Pass the mock connection to a jOOQ DSLContext:
        return DSL.using(connection, SQLDialect.POSTGRES);
    }

    @Test
    public void shouldRetrieveFolderByTimestampAfterLatestVersion() {
        /**
         * This test assumes status  in the file testFolderVersionsDB.sql
         */
        FolderHistoryAccess fa1 = new FolderHistoryAccess(testDomainAccess, tenantId);
        fa1.setFolderId(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"));

        // GET MOST RECENT VERSION IN TIME
        I_FolderAccess returned = fa1.retrieveInstanceForExistingFolder(
                fa1,
                UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"),
                Timestamp.valueOf("2021-12-17 15:10:33.54"));
        String middleNodeNamespace = returned.getSubfoldersList()
                .get(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"))
                .getItems()
                .get(0)
                .getNamespace();
        assertEquals(middleNodeNamespace, "middle leave UPDATED");

        // GET VERSION THAT CORRESPONDS TO A TIMESTAMP BETWEEN THE FIRST SUBMISSION AMD THE SECOND UPDATE SO HISTORY
        // VERSIONS ARE RETRIEVED
        I_FolderAccess returnedHistoricalVersion = fa1.retrieveInstanceForExistingFolder(
                fa1,
                UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"),
                Timestamp.valueOf("2019-12-07 15:10:33.54"));
        String leaveNodeLatestNamespaceHistorical = returnedHistoricalVersion
                .getSubfoldersList()
                .get(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"))
                .getSubfoldersList()
                .get(UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce"))
                .getItems()
                .get(0)
                .getNamespace();
        assertEquals(leaveNodeLatestNamespaceHistorical, "namespace leave");
    }

    @Test
    public void shouldRetrieveFolderByTimestampWithTimestampBetweenFirstAndLastVersion() {
        /**
         * This test assumes status  in the file testFolderVersionsDB.sql
         */
        FolderHistoryAccess fa1 = new FolderHistoryAccess(testDomainAccess, tenantId);
        // GET VERSION THAT CORRESPONDS TO A TIMESTAMP BETWEEN THE FIRST SUBMISSION AMD THE SECOND UPDATE SO HISTORY
        // VERSIONS ARE RETRIEVED
        I_FolderAccess returnedHistoricalVersion = fa1.retrieveInstanceForExistingFolder(
                fa1,
                UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"),
                Timestamp.valueOf("2019-12-07 15:10:33.54"));
        String leaveNodeLatestNamespaceHistorical = returnedHistoricalVersion
                .getSubfoldersList()
                .get(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"))
                .getSubfoldersList()
                .get(UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce"))
                .getItems()
                .get(0)
                .getNamespace();
        assertEquals(leaveNodeLatestNamespaceHistorical, "namespace leave");
    }
}
