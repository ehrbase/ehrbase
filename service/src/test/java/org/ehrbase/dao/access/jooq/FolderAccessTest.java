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

import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.ehrbase.test_data.folder.FolderTestDataCanonicalJson;
import com.nedap.archie.rm.datastructures.Item;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.ehrbase.serialisation.CanonicalJson;
import org.apache.commons.io.IOUtils;
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
public class FolderAccessTest {
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
        FolderMockDataProvider provider = new FolderMockDataProvider();
        MockConnection connection = new MockConnection(provider);
        // Pass the mock connection to a jOOQ DSLContext:
        return DSL.using(connection, SQLDialect.POSTGRES_9_5);
    }

    @Test
    @Ignore
    public void shouldRetriveFolderAccessByUid() throws Exception {
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        assertEquals("folder_archetype.v1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("folder_archetype_name_1",fa2.getFolderRecord().getName());

        // Parent FolderAccess is returned
        assertEquals("folder_archetype_name_1", ((FolderAccess)fa2).getFolderRecord().getName());
        assertEquals("[\"2019-08-09 09:56:52.464799+02\",)", ((FolderAccess)fa2).getFolderRecord().getSysPeriod().toString());
        assertEquals("{\"details\": \"xxx1\"}", ((FolderAccess)fa2).getFolderRecord().getDetails().toString());

        assertNotNull(fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")));
        assertEquals(1, fa2.getSubfoldersList().size());
        assertEquals("folder_archetype_name_2", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"))).getFolderRecord().getName());
        DateTimeFormatter dateTimeFormatter
                = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS");
        DateTime expected
                = DateTime.parse("2019-06-13 18:10:33.760", dateTimeFormatter);
        DateTime actual
                = DateTime.parse(((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"))).getFolderRecord().getSysTransaction().toString(), dateTimeFormatter);
        assertEquals(expected, actual);

        //child 1st level is returned
        assertEquals(1, fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().size());
        assertNotNull(fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")));
        assertEquals("folder_archetype_name_3", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"))).getFolderRecord().getName());
        assertEquals("{\"details\": \"xxx3\"}", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"))).getFolderRecord().getDetails().toString());

        assertTrue(((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"))).getFolderRecord().getActive());
        assertEquals(2, fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().size());
        assertNotNull( fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("77750555-ec91-4025-838d-09ddb4e999cb")));

        //child 3rd level and leave (I)
        assertNotNull(fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("77750555-ec91-4025-838d-09ddb4e999cb")));
        //child 3rd level and leave (II)
        assertNotNull(fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b")));
    }

    @Test
    @Ignore
    public void shouldRetrieveFolderAccessWithItems() throws Exception {
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));

        //top folder with one item
        assertEquals(1, fa2.getItems().size());
        assertEquals("namespace", fa2.getItems().get(0).getNamespace());
        assertEquals("FOLDER", fa2.getItems().get(0).getType());
        assertEquals("48282ddd-4c7d-444a-8159-458a03c9827f", fa2.getItems().get(0).getId().toString());

        //child item of depth 2 with two items
        assertEquals(2, fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getItems().size());
        assertEquals("namespace2", fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getItems().get(0).getNamespace());
        assertEquals("COMPOSITION", fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getItems().get(0).getType());
        assertEquals("076f09ee-8da3-ae1b-0072-3ee18965fbb9", fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getItems().get(0).getId().toString());

        assertEquals("namespace3", fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getItems().get(1).getNamespace());
        assertEquals("EHR", fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getItems().get(1).getType());
        assertEquals("5bf07118-e22e-e233-35c9-78820d76627c", fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getItems().get(1).getId().toString());
    }

    @Test
    @Ignore
    public void shouldInsertFolderWithNoSubfolders() throws Exception {
        //the creation and commit returning valid ids implies that the FolderMockDataProvider.java has provided the corresponding result for each SQL generated when inserting

        //create folder to insert
        Folder folder = new Folder();
        UIDBasedId uid = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");
        ObjectVersionId uidim = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");
        folder.setUid(uid);
        DvText name = new DvText();
        name.setValue("nameOfFolder");
        folder.setName(name);
        folder.setArchetypeNodeId("archetype_1");
        ItemStructure is = new ItemStructure() {
            @Override
            public List getItems() {
                Item item = new Item() {
                    @Override
                    public DvText getName() {
                        return new DvText("fol1");
                    }
                };
                List<Item> items =  new ArrayList<>();
                items.add(item);
                return items;
            }
        };
        folder.setDetails(is);

        //insert folder
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 = (FolderAccess) FolderAccess.getNewFolderAccessInstance(fa1, folder, DateTime.now(), UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"));


        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", fa2.getFolderRecord().getId().toString());
        assertEquals("archetype_1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder", fa2.getFolderRecord().getName());


        String expected = ("'{\n" +
                "  \"_type\" : \"\",\n" +
                "  \"items\" : [ {\n" +
                "    \"name\" : {\n" +
                "      \"_type\" : \"DV_TEXT\",\n" +
                "      \"value\" : \"fol1\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}'::jsonb").replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));//avoids problems amond different platforms due to different representations of line change.
        StringWriter expectedStringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(expectedStringWriter);
        printWriter.print(expected);//avoids problems amond different platforms due to different representations of line change.
        printWriter.close();

        assertEquals(expectedStringWriter.toString(), fa2.getFolderRecord().getDetails().toString());

        //commit and check returned UID is the valid one
        UUID storedFolderUid = fa2.commit();
        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", storedFolderUid.toString());
    }

    @Test
    @Ignore
    public void shouldInsertFolderWithSubfolders() throws Exception {

        //the creation and commit returning valid ids implies that the FolderMockDataProvider.java has provided the corresponding result for each SQL generated when inserting

        //create folder to insert
        Folder folder = new Folder();
        UIDBasedId uid = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");
        ObjectVersionId uidim = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");
        folder.setUid(uid);
        DvText name = new DvText();
        name.setValue("nameOfFolder1");
        folder.setName(name);
        folder.setArchetypeNodeId("archetype_1");
        ItemStructure is = new ItemStructure() {
            @Override
            public List getItems() {
                Item item = new Item() {
                    @Override
                    public DvText getName() {
                        return new DvText("fol1");
                    }
                };
                List<Item> items =  new ArrayList<>();
                items.add(item);
                return items;
            }
        };
        folder.setDetails(is);

        //create subfolder
        Folder folder2
                = new Folder();
        UIDBasedId uid2 = new ObjectVersionId("f0a2af65-fe89-45a4-9456-07c5e17b1634");
        ObjectVersionId uidim2 = new ObjectVersionId("f0a2af65-fe89-45a4-9456-07c5e17b1634");
        folder2.setUid(uid2);
        DvText name2 = new DvText();
        name2.setValue("nameOfFolder2");
        folder2.setName(name2);
        folder2.setArchetypeNodeId("archetype_2");
        ItemStructure is2 = new ItemStructure() {
            @Override
            public List getItems() {
                Item item = new Item() {
                    @Override
                    public DvText getName() {
                        return new DvText("fol2");
                    }
                };
                List<Item> items =  new ArrayList<>();
                items.add(item);
                return items;
            }
        };
        folder2.setDetails(is2);

        //create subfolder (grandson)
        Folder folder3
                = new Folder();
        UIDBasedId uid3 = new ObjectVersionId("f4a2af65-fe89-45a4-9456-07c5e17b1634");
        ObjectVersionId uidim3 = new ObjectVersionId("f4a2af65-fe89-45a4-9456-07c5e17b1634");
        folder3.setUid(uid3);
        DvText name3 = new DvText();
        name3.setValue("nameOfFolder3");
        folder3.setName(name3);
        folder3.setArchetypeNodeId("archetype_3");
        ItemStructure is3 = new ItemStructure() {
            @Override
            public List getItems() {
                Item item = new Item() {
                    @Override
                    public DvText getName() {
                        return super.getName();
                    }
                };
                List<Item> items =  new ArrayList<>();
                items.add(item);
                return items;
            }
        };
        folder3.setDetails(is3);

        //add subfolders
        folder.getFolders().add(folder2);
        folder2.addFolder(folder3);

        //insert folder
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 = (FolderAccess) FolderAccess.getNewFolderAccessInstance(fa1, folder, DateTime.now(), UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"));


        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", fa2.getFolderRecord().getId().toString());
        assertEquals("archetype_1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder1", fa2.getFolderRecord().getName());
        String expected = ("'{\n" +
                "  \"_type\" : \"\",\n" +
                "  \"items\" : [ {\n" +
                "    \"name\" : {\n" +
                "      \"_type\" : \"DV_TEXT\",\n" +
                "      \"value\" : \"fol1\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}'::jsonb").replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));;//avoids problems amond different platforms due to different representations of line change.
        StringWriter expectedStringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(expectedStringWriter);
        printWriter.print(expected);//avoids problems amond different platforms due to different representations of line change.
        printWriter.close();
        assertEquals(expectedStringWriter.toString(), fa2.getFolderRecord().getDetails().toString());

        assertEquals("f0a2af65-fe89-45a4-9456-07c5e17b1634", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getId().toString());
        assertEquals("archetype_2", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder2", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getName());
        String expected2 = ("'{\n" +
                "  \"_type\" : \"\",\n" +
                "  \"items\" : [ {\n" +
                "    \"name\" : {\n" +
                "      \"_type\" : \"DV_TEXT\",\n" +
                "      \"value\" : \"fol2\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}'::jsonb").replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));//avoids problems amond different platforms due to different representations of line change.
        expectedStringWriter = new StringWriter();
        printWriter = new PrintWriter(expectedStringWriter);//avoids problems amond different platforms due to different representations of line change.
        printWriter.print(expected2);
        printWriter.close();
        assertEquals(expectedStringWriter.toString(), ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getDetails().toString());


        assertEquals("f4a2af65-fe89-45a4-9456-07c5e17b1634", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getId().toString());
        assertEquals("archetype_3", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder3", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getName());
        String expected3 = ("'{\n" +
                "  \"_type\" : \"\",\n" +
                "  \"items\" : [ { } ]\n" +
                "}'::jsonb").replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));//avoids problems amond different platforms due to different representations of line change.
        expectedStringWriter = new StringWriter();
        printWriter = new PrintWriter(expectedStringWriter);
        printWriter.print(expected3);//avoids problems amond different platforms due to different representations of line change.
        assertEquals(expectedStringWriter.toString(), ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getDetails().toString());
        expectedStringWriter.flush();
        printWriter.flush();
        printWriter.close();

        UUID storedFolderUid = fa2.commit();
        //commit should return the top level folderId
        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", storedFolderUid.toString());

    }

    @Test
    @Ignore
    public void shouldUpdateExistingFolder() throws Exception {
        //1-retrieve a DAO for an existing folder in the DB
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        assertEquals("folder_archetype.v1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("folder_archetype_name_1",fa2.getFolderRecord().getName());

        //2-perform update and persist changes with the previously retrieved DAO
        fa2.setFolderName("modifiedName");
        fa2.setFolderNArchetypeNodeId("modifiedArchetypeNodeId");
        fa2.setIsFolderActive(false);
        ItemStructure is = new ItemStructure() {
            @Override
            public List getItems() {
                Item item = new Item() {
                    @Override
                    public DvText getName() {
                        return new DvText("modifiedValue");
                    }
                };
                List<Item> items =  new ArrayList<>();
                items.add(item);
                return items;
            }
        };
        //fa2.setFolderDetails(DSL.field(DSL.val("{\"s\": \"modifiedValue\"}") + "::jsonb"));
        fa2.setFolderDetails(is);
        fa2.setFolderSysTransaction(new Timestamp(DateTime.now().getMillis()));
        fa2.setFolderSysPeriod(DSL.field(DSL.val("[\"2019-07-26 11:28:11.631959+02\",)") + "::tstzrange"));

        //perform updates in a second level subfolder
        I_FolderAccess fa3 = fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"));
        fa3.setFolderName("new name of folder 2");

        //perform update second level subfolder
        I_FolderAccess fa4 = fa3.getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"));
        fa4.setFolderNArchetypeNodeId("new archetype node id");

        //perform update in a third level subforlder
        I_FolderAccess fa5 = fa4.getSubfoldersList().get(UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"));
        fa5.setIsFolderActive(false);


        //the mock data provider checks the UPDATE SQL code generated and returns 1 simulating the update of one row in the FOLDER table.
        //this way the correct sql generation is checked indirectly.
        boolean updated = false;
        updated = fa2.update(new Timestamp(DateTime.now().getMillis()), true);
        //assertEquals(true, updated);// could not manage to mock the result "Affected row(s)          : 1" from updates" for the UPDATEE statement. The mock data provider returns 0 irrespectively of weather the record is modified in the DB or not. As a consequence this always is false in the mocked version.
    }

    @Test
    public void shouldDeleteExistingFolder(){
        I_FolderAccess fa1 = new FolderAccess(testDomainAccess);
        fa1.setFolderId(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        int affectedRows = fa1.delete();
        //assertEquals(5, affectedRows); commented since metadata indicating that affected rows are 5 could not be mocked. Now checked relyingon the Moch Data Provider that intercepts the SQL code generated.
    }

    @Test
    public void canBuildBasicFolderAccessForInsert() throws IOException {

        UUID ehrId = UUID.randomUUID();

        Folder folder = generateFolderFromTestFile(FolderTestDataCanonicalJson.FLAT_FOLDER_INSERT);

        I_ContributionAccess contributionAccess =
                I_ContributionAccess.getInstance(testDomainAccess, ehrId);

        I_FolderAccess folderAccess = FolderAccess.buildNewFolderAccessHierarchy(
                testDomainAccess,
                folder,
                DateTime.now(),
                ehrId,
                contributionAccess
        );

        assertThat(folderAccess).isNotNull();
        assertThat(folderAccess.getSubfoldersList().size()).isEqualTo(0);
        assertThat(folderAccess.getFolderName()).isEqualTo("Flat root");
        assertThat(folderAccess.isFolderActive()).isTrue();
    }

    @Test
    public void canBuildNestedFolderAccessForInsert() throws IOException {

        UUID ehrId = UUID.randomUUID();
        Folder folder = generateFolderFromTestFile(FolderTestDataCanonicalJson.NESTED_FOLDER);

        I_ContributionAccess contributionAccess =
                I_ContributionAccess.getInstance(testDomainAccess, ehrId);

        I_FolderAccess folderAccess= FolderAccess.buildNewFolderAccessHierarchy(
                testDomainAccess,
                folder,
                DateTime.now(),
                ehrId,
                contributionAccess
        );

        assertThat(folderAccess).isNotNull();
        assertThat(folderAccess.getSubfoldersList().size()).isEqualTo(2);
        assertThat(folderAccess.getFolderName()).isEqualTo("hospital episodes");
    }

    private Folder generateFolderFromTestFile(FolderTestDataCanonicalJson testEntry) throws IOException {
        String value = IOUtils.toString(testEntry.getStream(), UTF_8);
        return new CanonicalJson().unmarshal(value, Folder.class);
    }
}
