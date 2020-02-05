/*
 * Copyright (c) 2019 Vitasystems GmbH, Hannover Medical School, and Luis Marco-Ruiz (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.dao.access.jooq;

import com.nedap.archie.rm.datastructures.Item;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.apache.commons.io.IOUtils;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.serialisation.CanonicalJson;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.ehrbase.test_data.folder.FolderTestDataCanonicalJson;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/***
 *@Created by Luis Marco-Ruiz on Jun 13, 2019
 */
public class FolderHistoryAccessTest {
    protected I_DomainAccess testDomainAccess;
    protected DSLContext context;
    protected I_KnowledgeCache knowledge;

    @Before
    public  void beforeClass() {
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
        FolderAccessHistoryMockDataProvider provider = new FolderAccessHistoryMockDataProvider();
        MockConnection connection = new MockConnection(provider);
        // Pass the mock connection to a jOOQ DSLContext:
        return DSL.using(connection, SQLDialect.POSTGRES);
    }

    @Test
    public void shouldRetrieveFolderByTimestampAfterLatestVersion(){
        /**
         * This test assumes status  in the file testFolderVersionsDB.sql
         */
        FolderHistoryAccess fa1 = new FolderHistoryAccess(testDomainAccess);
        fa1.setFolderId(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"));

        //GET MOST RECENT VERSION IN TIME
        I_FolderAccess returned = fa1.retrieveInstanceForExistingFolder(fa1,UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"),Timestamp.valueOf("2021-12-17 15:10:33.54"));
        String middleNodeNamespace = returned.getSubfoldersList().get(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c")).getItems().get(0).getNamespace();
        assertEquals(middleNodeNamespace, "middle leave UPDATED" );

        //GET VERSION THAT CORRESPONDS TO A TIMESTAMP BETWEEN THE FIRST SUBMISSION AMD THE SECOND UPDATE SO HISTORY VERSIONS ARE RETRIEVED
        I_FolderAccess returnedHistoricalVersion = fa1.retrieveInstanceForExistingFolder(fa1,UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"),Timestamp.valueOf("2019-12-07 15:10:33.54"));
        String leaveNodeLatestNamespaceHistorical = returnedHistoricalVersion.getSubfoldersList().get(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c")).getSubfoldersList().get(UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce")).getItems().get(0).getNamespace();
        assertEquals(leaveNodeLatestNamespaceHistorical, "namespace leave" );
    }

    @Test
    public void shouldRetrieveFolderByTimestampWithTimestampBetweenFirstAndLastVersion(){
        /**
         * This test assumes status  in the file testFolderVersionsDB.sql
         */
        FolderHistoryAccess fa1 = new FolderHistoryAccess(testDomainAccess);
        //GET VERSION THAT CORRESPONDS TO A TIMESTAMP BETWEEN THE FIRST SUBMISSION AMD THE SECOND UPDATE SO HISTORY VERSIONS ARE RETRIEVED
        I_FolderAccess returnedHistoricalVersion = fa1.retrieveInstanceForExistingFolder(fa1,UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"),Timestamp.valueOf("2019-12-07 15:10:33.54"));
        String leaveNodeLatestNamespaceHistorical = returnedHistoricalVersion.getSubfoldersList().get(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c")).getSubfoldersList().get(UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce")).getItems().get(0).getNamespace();
        assertEquals(leaveNodeLatestNamespaceHistorical, "namespace leave" );
    }
}
