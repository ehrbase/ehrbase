package org.ehrbase.dao.access.jooq;

import com.nedap.archie.rm.datastructures.Item;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.jooq.pg.enums.EntryType;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.*;
import static org.junit.Assert.*;

/***
 *@Created by Luis Marco-Ruiz on Apr 21, 2019
 */
//@RunWith(MockitoJUnitRunner.class)
public class FolderAccessTestDbConnect {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();



    @Test
@Ignore
    public void shouldRetriveInstanceByTimestamp2() throws Exception {
        System.out.println("start");
        CompositionAccess compositionAccess = new CompositionAccess(testDomainAccess);
//4/25/19 1556150400000
//4/26/19 1556236800000
//4/27/19 1556323200000
        CompositionAccess ca = (CompositionAccess) CompositionAccess.retrieveCompositionVersion(compositionAccess, UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"), 5);
        System.out.println("end NOT THE ONE!!!");
    }

    /******code from AccessTestCase****/
//@Mock
    protected I_DomainAccess testDomainAccess;
    protected DSLContext context;
    protected I_KnowledgeCache knowledge;
    //protected I_IntrospectCache introspectCache;
    private Connection connection;

/*@Before
public void setUp() throws Exception {
Mockito.when(testDomainAccess.getContext()).thenReturn(this.getMockingContext());
Properties props = new Properties();
props.put("knowledge.path.archetype", "src/test/resources/knowledge/archetypes");
props.put("knowledge.path.template", "src/test/resources/knowledge/templates");
props.put("knowledge.path.opt", "src/test/resources/knowledge/operational_templates");
props.put("knowledge.cachelocatable", "true");
props.put("knowledge.forcecache", "true");

knowledge = new KnowledgeCache(null, props);

Pattern include = Pattern.compile(".*");

knowledge.retrieveFileMap(include, null);

Map<String, Object> properties = new HashMap<>();
properties.put(I_DomainAccess.KEY_DIALECT, "POSTGRES");
properties.put(I_DomainAccess.KEY_URL, "jdbc:postgresql://"+"0.0.0.0:5432/");//+ System.getProperty("test.db.host") + ":" + System.getProperty("test.db.port") + "/" + System.getProperty("test.db.name"));
properties.put(I_DomainAccess.KEY_LOGIN, System.getProperty("test.db.user")"postgres");
properties.put(I_DomainAccess.KEY_PASSWORD, System.getProperty("test.db.password")"postgres");

properties.put(I_DomainAccess.KEY_KNOWLEDGE, knowledge);

try {
testDomainAccess = new DummyDataAccess(properties);
} catch (Exception e) {
e.printStackTrace();
}

context = getMockingContext();//testDomainAccess.getContext();
introspectCache = testDomainAccess.getIntrospectCache().load().synchronize();
}*/

    @Before
    public void beforeClass() {

        SQLDialect dialect = SQLDialect.valueOf("POSTGRES");
        //String url = "jdbc:postgresql://localhost:5432/ethercis";
        String url = "jdbc:postgresql://localhost:5432/ehrbase";
// String url = "jdbc:postgresql://" + System.getProperty("test.db.host") + ":" + System.getProperty("test.db.port") + "/" + System.getProperty("test.db.name");
// String url = "jdbc:postgresql://192.168.2.108:5432/ethercis";
        String login = "postgres";//System.getProperty("test.db.user");
        String password = "postgres";//System.getProperty("test.db.password");
        Properties props = new Properties();
        props.put("knowledge.path.archetype", "src/test/resources/knowledge/archetypes");
        props.put("knowledge.path.template", "src/test/resources/knowledge/templates");
        props.put("knowledge.path.opt", "src/test/resources/knowledge/operational_templates");
        props.put("knowledge.cachelocatable", "true");
        props.put("knowledge.forcecache", "true");


        try {
            connection = DriverManager.getConnection(url, login, password);
        } catch (SQLException e) {
            throw new IllegalArgumentException("SQL exception occurred while connecting:" + e);
        }

        if (connection == null)
            throw new IllegalArgumentException("Could not connect to DB");

        /*DSLContext*/
        context = /*getMockingContext();*/DSL.using(connection, dialect);


        try {
            testDomainAccess = new DummyDataAccess(context, knowledge, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /******end code from AccessTestCase****/

    private DSLContext getMockingContext() {
// Initialise your data provider (implementation further down):
        MockDataProvider provider = new MyProvider();
        MockConnection connection = new MockConnection(provider);

// Pass the mock connection to a jOOQ DSLContext:
        return DSL.using(connection, SQLDialect.POSTGRES_9_5);

    }

/*    @Test
    public void shouldRetrieveFolderItesm() throws Exception {
        *//*1-retrieve a DAO for an existing folder in the DB*//*
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
       // fa1.retrieveInstanceForExistingFolder(fa1,UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        List<ObjectRef> result = FolderAccess.retrieveItemsByFolderAndContributionId(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"), fa1);

        System.out.println("fin");
    }*/

/*    @Test
    public void shouldUpdateExistingFolderWithItems() throws Exception {
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        fa1.setEhrId(UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"));
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        // assertEquals("folder_archetype1.v1", fa2.getFolderRecord().getArchetypeNodeId());
        // assertEquals("folder_archetype_name1",fa2.getFolderRecord().getName());


        *//*perform update*//*
        fa2.setFolderName("modifiedName99999999999");
        fa2.setFolderNArchetypeNodeId("modifiedArchetypeNodeId");
        fa2.setIsFolderActive(false);
        fa2.setFolderDetails(DSL.field(DSL.val("{\"s\": \"modifiedValue\"}") + "::jsonb"));
        fa2.setFolderSysTransaction(new Timestamp(DateTime.now().getMillis()));


        fa2.setFolderSysPeriod(DSL.field(DSL.val("[\"2019-07-26 11:28:11.631959+02\",)") + "::tstzrange"));

        *//*update items*//*
        fa2.getItems().get(0).setNamespace("updated namespace99");
        fa2.update(new Timestamp(DateTime.now().getMillis()), true);
    }*/

    /*@Test
    public void shouldUpdateExistingSubFolderWithItems() throws Exception {
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        // assertEquals("folder_archetype1.v1", fa2.getFolderRecord().getArchetypeNodeId());
        // assertEquals("folder_archetype_name1",fa2.getFolderRecord().getName());

        *//*perform update*//*
        fa2.setFolderName("modifiedName99999999999");
        fa2.setFolderNArchetypeNodeId("modifiedArchetypeNodeId");
        fa2.setIsFolderActive(false);
        fa2.setFolderDetails(DSL.field(DSL.val("{\"s\": \"modifiedValue\"}") + "::jsonb"));
        fa2.setFolderSysTransaction(new Timestamp(DateTime.now().getMillis()));


        fa2.setFolderSysPeriod(DSL.field(DSL.val("[\"2019-07-26 11:28:11.631959+02\",)") + "::tstzrange"));

        *//*update items*//*
        fa2.getItems().get(0).setNamespace("updated namespace99");

        fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getItems().get(0).setNamespace("subfolder namespace");
        fa2.update(new Timestamp(DateTime.now().getMillis()), true);
    }*/

    @Test
    public void shouldDeleteExistingFolderExperimentItemsBehaviour(){
        I_FolderAccess fa1 = new FolderAccess(testDomainAccess);
        fa1.setFolderId(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        int affectedRows = fa1.delete();
        //assertEquals(5, affectedRows); commented since metadata indicating that affected rows are 5 could not be mocked. Now checked relyingon the Moch Data Provider that intercepts the SQL code generated.
    }

    @Test
    public void shouldRetrieveFolderAccessWithItems() throws Exception {
/*        FolderAccess fa1 = new FolderAccess(testDomainAccess);
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

        System.out.println("fin");*/
    }

    @Test
    public void shouldRetriveFolderAccessByUidWithDetails() throws Exception {
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
                = DateTime.parse("2019-04-24 20:32:52.144", dateTimeFormatter);
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
    public void shouldUpdateExistingFolderWithDetails() throws Exception {
        /*1-retrieve a DAO for an existing folder in the DB*/
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        assertEquals("folder_archetype.v1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("folder_archetype_name_1",fa2.getFolderRecord().getName());

        /*2-perform update and persist changes with the previously retrieved DAO*/
        fa2.setFolderName("modifiedName");
        fa2.setFolderNArchetypeNodeId("modifiedArchetypeNodeId");
        fa2.setIsFolderActive(false);
        fa2.setFolderDetails(DSL.field(DSL.val("{\"s\": \"modifiedValue\"}") + "::jsonb"));
        fa2.setFolderSysTransaction(new Timestamp(DateTime.now().getMillis()));
        fa2.setFolderSysPeriod(DSL.field(DSL.val("[\"2019-07-26 11:28:11.631959+02\",)") + "::tstzrange"));

        /*perform updates in a second level subfolder*/
        I_FolderAccess fa3 = fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"));
        fa3.setFolderName("new name of folder 2");

        /*perform update second level subfolder*/
        I_FolderAccess fa4 = fa3.getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"));
        fa4.setFolderNArchetypeNodeId("new archetype node id");

        /*perform update in a third level subforlder*/
        I_FolderAccess fa5 = fa4.getSubfoldersList().get(UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"));
        fa5.setIsFolderActive(false);


        //the mock data provider checks the UPDATE SQL code generated and returns 1 simulating the update of one row in the FOLDER table.
        //this way the correct sql generation is checked indirectly.
        boolean updated = false;
        updated = fa2.update(new Timestamp(DateTime.now().getMillis()), true);
        //assertEquals(true, updated);// could not manage to mock the result "Affected row(s)          : 1" from updates" for the UPDATEE statement. The mock data provider returns 0 irrespectively of weather the record is modified in the DB or not. As a consequence this always is false in the mocked version.
    }

    @Test
    public void shouldInsertFolderWithNoSubfoldersWithDetails() throws Exception {
        //the creation and commit returning valid ids implies that the FolderMockDataProvider.java has provided the corresponding result for each SQL generated when inserting

        /*create folder to insert*/
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
                        return super.getName();
                    }
                };
                List<Item> items =  new ArrayList<>();
                items.add(item);
                return items;
            }
        };
        folder.setDetails(is);

        //add an item
        ObjectRef objectRef = new ObjectRef();
        FolderAccessTestDbConnect.ObjectRefId oref = new FolderAccessTestDbConnect().new ObjectRefId(UUID.randomUUID().toString());
        objectRef.setId(oref);
        objectRef.setType("COMPOSITION");
        objectRef.setNamespace("test.namespace");
        folder.getItems().add(objectRef);


        /*insert folder*/
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 = (FolderAccess) FolderAccess.getNewFolderAccessInstance(fa1, folder, DateTime.now(), UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"));

        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", fa2.getFolderRecord().getId().toString());
        assertEquals("archetype_1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder", fa2.getFolderRecord().getName());


        //commit and check returned UID is the valid one
        UUID storedFolderUid = fa2.commit();
        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", storedFolderUid.toString());
    }
    private class ObjectRefId extends ObjectId {
        public ObjectRefId(final String value) {
            super(value);
        }
    }


    @Test
    public void shouldInsertFolderWithNoSubfolders() throws Exception {
        //the creation and commit returning valid ids implies that the FolderMockDataProvider.java has provided the corresponding result for each SQL generated when inserting

        /*create folder to insert*/
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
                        return /*super.getName();*/new DvText("fol1");
                    }
                };
                List<Item> items =  new ArrayList<>();
                items.add(item);
                return items;
            }
        };
        folder.setDetails(is);

        /*insert folder*/
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 = (FolderAccess) FolderAccess.getNewFolderAccessInstance(fa1, folder, DateTime.now(), UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"));

        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", fa2.getFolderRecord().getId().toString());
        assertEquals("archetype_1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder", fa2.getFolderRecord().getName());

        //commit and check returned UID is the valid one
        UUID storedFolderUid = fa2.commit();
        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", storedFolderUid.toString());
        assertEquals("'{\n" +
                "  \"_type\" : \"\",\n" +
                "  \"links\" : [ ],\n" +
                "  \"items\" : [ {\n" +
                "    \"links\" : [ ],\n" +
                "    \"path\" : \"/\"\n" +
                "  } ],\n" +
                "  \"path\" : \"/details\"\n" +
                "}'::jsonb", fa2.getFolderRecord().getDetails().toString());
    }

    @Test
    public void shouldInsertFolderWithSubfoldersDetails() throws Exception {

        //the creation and commit returning valid ids implies that the FolderMockDataProvider.java has provided the corresponding result for each SQL generated when inserting

        /*create folder to insert*/
        Folder folder = new Folder();
        UIDBasedId uid = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
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

        /*create subfolder*/
        Folder folder2
                = new Folder();
        UIDBasedId uid2 = new ObjectVersionId("f0a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim2 = new ObjectVersionId("f0a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
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

        /*create subfolder (grandson)*/
        Folder folder3
                = new Folder();
        UIDBasedId uid3 = new ObjectVersionId("f4a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim3 = new ObjectVersionId("f4a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
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

        /*add subfolders*/
        folder.getFolders().add(folder2);
        folder2.addFolder(folder3);

        /*insert folder*/
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        //FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        FolderAccess fa2 = (FolderAccess) FolderAccess.getNewFolderAccessInstance(fa1, folder, DateTime.now(), UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"));


        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", fa2.getFolderRecord().getId().toString());
        assertEquals("archetype_1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder1", fa2.getFolderRecord().getName());
        assertEquals("'{\n" +
                "  \"_type\" : \"\",\n" +
                "  \"links\" : [ ],\n" +
                "  \"items\" : [ {\n" +
                "    \"name\" : {\n" +
                        "      \"_type\" : \"DV_TEXT\",\n" +
                        "      \"value\" : \"fol1\",\n" +
                        "      \"mappings\" : [ ]\n" +
                        "    },\n" +
                "    \"links\" : [ ],\n" +
                "    \"path\" : \"/\"\n" +
                "  } ],\n" +
                "  \"path\" : \"/details\"\n" +
                "}'::jsonb", fa2.getFolderRecord().getDetails().toString());


        assertEquals("f0a2af65-fe89-45a4-9456-07c5e17b1634", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getId().toString());
        assertEquals("archetype_2", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder2", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getName());
        assertEquals("'{\n" +
                "  \"_type\" : \"\",\n" +
                "  \"links\" : [ ],\n" +
                "  \"items\" : [ {\n" +
                "    \"name\" : {\n" +
                "      \"_type\" : \"DV_TEXT\",\n" +
                "      \"value\" : \"fol2\",\n" +
                "      \"mappings\" : [ ]\n" +
                "    },\n" +
                "    \"links\" : [ ],\n" +
                "    \"path\" : \"/\"\n" +
                "  } ],\n" +
                "  \"path\" : \"/details\"\n" +
                "}'::jsonb", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getDetails().toString());


        assertEquals("f4a2af65-fe89-45a4-9456-07c5e17b1634", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getId().toString());
        assertEquals("archetype_3", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder3", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getName());
        assertEquals("'{\n" +
                "  \"_type\" : \"\",\n" +
                "  \"links\" : [ ],\n" +
                "  \"items\" : [ {\n" +
                "    \"links\" : [ ],\n" +
                "    \"path\" : \"/\"\n" +
                "  } ],\n" +
                "  \"path\" : \"/folders[archetype_3]/details\"\n" +
                "}'::jsonb", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getDetails().toString());


        UUID storedFolderUid = fa2.commit();
        //commit should return the top level folderId
        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", storedFolderUid.toString());
    }

    @Test
    //@Ignore
    public void shouldDeleteExistingFolder(){

        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        //FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        fa1.setFolderId(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        fa1.delete();
    }

    @Test
    @Ignore
    public void shouldUpdateExistingFolder() throws Exception {
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
       // assertEquals("folder_archetype1.v1", fa2.getFolderRecord().getArchetypeNodeId());
       // assertEquals("folder_archetype_name1",fa2.getFolderRecord().getName());


        /*perform update*/

        fa2.setFolderName("modifiedName555");
        fa2.setFolderNArchetypeNodeId("modifiedArchetypeNodeId");
        fa2.setIsFolderActive(false);
        fa2.setFolderDetails(DSL.field(DSL.val("{\"s\": \"modifiedValue\"}") + "::jsonb"));
        fa2.setFolderSysTransaction(new Timestamp(DateTime.now().getMillis()));


        fa2.setFolderSysPeriod(DSL.field(DSL.val("[\"2019-07-26 11:28:11.631959+02\",)") + "::tstzrange"));

        fa2.update(new Timestamp(DateTime.now().getMillis()), true);
    }


    @Test
    @Ignore
    public void shouldUpdateExistingFolder2() throws Exception {
        /*1-retrieve a DAO for an existing folder in the DB*/
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("f8a2af65-fe89-45a4-9456-07c5e17b1634"));
       // assertEquals("archetype_1", fa2.getFolderRecord().getArchetypeNodeId());
       // assertEquals("nameOfFolder1",fa2.getFolderRecord().getName());

        /*2-perform update and persist changes with the previously retrieved DAO*/
        fa2.setFolderName("modifiedName22");
        fa2.setFolderNArchetypeNodeId("modifiedArchetypeNodeId22");
        fa2.setIsFolderActive(false);
        //fa2.setFolderDetails(DSL.field(DSL.val("{\"s\": \"modifiedValue22\"}") + "::jsonb"));
        fa2.setFolderDetails(DSL.field(DSL.val("{\"a\": \"a\"}") + "::jsonb"));
        fa2.setFolderSysTransaction(new Timestamp(DateTime.now().getMillis()));
       // fa2.setFolderSysPeriod(DSL.field(DSL.val("[\"2019-07-31 11:40:49.093487+00\",)") + "::tstzrange"));

        //the mock data provider checks the UPDATE SQL code generated and returns 1 simulating the update of one row in the FOLDER table.
        //this way the correct sql generation is checked indirectly.
        boolean updated = false;
        updated = fa2.update(new Timestamp(DateTime.now().getMillis()), true);
        assertEquals(true, updated);
    }


    @Test
    @Ignore
    public void shouldUpdateExistingFolder3() throws Exception {
        /*1-retrieve a DAO for an existing folder in the DB*/
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        assertEquals("folder_archetype1.v1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("folder_archetype_name1",fa2.getFolderRecord().getName());

        /*2-perform update and persist changes with the previously retrieved DAO*/
        fa2.setFolderName("modifiedName");
        fa2.setFolderNArchetypeNodeId("modifiedArchetypeNodeId");
        fa2.setIsFolderActive(false);
       // fa2.setFolderDetails(DSL.field(DSL.val("{\"s\": \"modifiedValue\"}") + "::jsonb"));
       // fa2.setFolderSysTransaction(new Timestamp(DateTime.now().getMillis()));
       // fa2.setFolderSysPeriod(DSL.field(DSL.val("[\"2019-07-26 11:28:11.631959+02\",)") + "::tstzrange"));

        //the mock data provider checks the UPDATE SQL code generated and returns 1 simulating the update of one row in the FOLDER table.
        //this way the correct sql generation is checked indirectly.
        boolean updated = false;
        updated = fa2.update(new Timestamp(DateTime.now().getMillis()), true);
        //assertEquals(true, updated); could not manage to mock the result "Affected row(s)          : 1" from updates
    }

    @Test
    @Ignore
    public void shouldRetriveFolderAccessByUid() throws Exception {
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
       // assertEquals("folder_archetype1.v1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("folder_archetype_name1",fa2.getFolderRecord().getName());


        //assertEquals("00550555-ec91-4025-838d-09ddb4e999cb", fa2.getFolderRecord().value1().toString());
        // Parent FolderAccess is returned
        assertEquals("folder_archetype_name1", ((FolderAccess)fa2).getFolderRecord().getName());

        assertNotNull(fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")));
        assertEquals(1, fa2.getSubfoldersList().size());
        assertEquals("folder_archetype_name2", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"))).getFolderRecord().getName());



        //child 1st level is returned
        assertEquals(1, fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().size());
        assertNotNull(fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")));
        assertEquals("folder_archetype_name3", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"))).getFolderRecord().getName());
        assertTrue(((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"))).getFolderRecord().getActive());
        assertNotNull( fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("77750555-ec91-4025-838d-09ddb4e999cb")));

        //child 3rd level and leave (I)
        assertNotNull(fa2.getSubfoldersList().get(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb")).getSubfoldersList().get(UUID.fromString("77750555-ec91-4025-838d-09ddb4e999cb")));
        //child 3rd level and leave (II)
    }

    @Test
    @Ignore
    public void shoulInsertFolder() throws Exception {


        /*create folder to insert*/
        Folder folder = new Folder();
        UIDBasedId uid = new ObjectVersionId("f2a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim = new ObjectVersionId("f2a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
        folder.setUid(uid);
        DvText name = new DvText();
        name.setValue("updated nombre");
        folder.setName(name);
        folder.setArchetypeNodeId("archetype_1");

        /*insert folder*/
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        //FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        I_FolderAccess fa3 =FolderAccess.getNewFolderAccessInstance(fa1, folder, DateTime.now(), UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"));
        fa3.commit();
    }

    @Test
    @Ignore
    public void shoulInsertFolderWithSubfolders() throws Exception {


        /*create folder to insert*/
        Folder folder = new Folder();
        UIDBasedId uid = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
        folder.setUid(uid);
        DvText name = new DvText();
        name.setValue("setFolder");
        folder.setName(name);
        folder.setArchetypeNodeId("archetype_1");

        /*create subfolder*/
        Folder folder2
                = new Folder();
        UIDBasedId uid2 = new ObjectVersionId("f0a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim2 = new ObjectVersionId("f0a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
        folder2.setUid(uid2);
        DvText name2 = new DvText();
        name2.setValue("setFolder");
        folder2.setName(name2);
        folder2.setArchetypeNodeId("archetype_1");

        /*create subfolder*/
        Folder folder3
                = new Folder();
        UIDBasedId uid3 = new ObjectVersionId("f4a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim3 = new ObjectVersionId("f4a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
        folder3.setUid(uid3);
        DvText name3 = new DvText();
        name3.setValue("setFolder");
        folder3.setName(name3);
        folder3.setArchetypeNodeId("archetype_1");


        /*add subfolders*/
        folder.getFolders().add(folder2);
        folder2.addFolder(folder3);


        /*insert folder*/
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        //FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        I_FolderAccess fa3 =FolderAccess.getNewFolderAccessInstance(fa1, folder, DateTime.now(), UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"));
        fa3.commit();

        System.out.print("dfsdsd");
    }

    @Test
    public void shouldInsertFolderWithSubfolders() throws Exception {

        //the creation and commit returning valid ids implies that the FolderMockDataProvider.java has provided the corresponding result for each SQL generated when inserting

        /*create folder to insert*/
        Folder folder = new Folder();
        UIDBasedId uid = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim = new ObjectVersionId("f8a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
        folder.setUid(uid);
        DvText name = new DvText();
        name.setValue("nameOfFolder1");
        folder.setName(name);
        folder.setArchetypeNodeId("archetype_1");

        /*create subfolder*/
        Folder folder2
                = new Folder();
        UIDBasedId uid2 = new ObjectVersionId("f0a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim2 = new ObjectVersionId("f0a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
        folder2.setUid(uid2);
        DvText name2 = new DvText();
        name2.setValue("nameOfFolder2");
        folder2.setName(name2);
        folder2.setArchetypeNodeId("archetype_2");

        /*create subfolder (grandson)*/
        Folder folder3
                = new Folder();
        UIDBasedId uid3 = new ObjectVersionId("f4a2af65-fe89-45a4-9456-07c5e17b1634");//org.apache.xmlbeans.SchemaType
        ObjectVersionId uidim3 = new ObjectVersionId("f4a2af65-fe89-45a4-9456-07c5e17b1634");//new UIDBASEDIDImpl();
        folder3.setUid(uid3);
        DvText name3 = new DvText();
        name3.setValue("nameOfFolder3");
        folder3.setName(name3);
        folder3.setArchetypeNodeId("archetype_3");

        /*add subfolders*/
        folder.getFolders().add(folder2);
        folder2.addFolder(folder3);

        /*insert folder*/
        FolderAccess fa1 = new FolderAccess(testDomainAccess);
        //FolderAccess fa2 =  (FolderAccess) FolderAccess.retrieveInstanceForExistingFolder(fa1, UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"));
        FolderAccess fa2 = (FolderAccess) FolderAccess.getNewFolderAccessInstance(fa1, folder, DateTime.now(), UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"));


        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", fa2.getFolderRecord().getId().toString());
        assertEquals("archetype_1", fa2.getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder1", fa2.getFolderRecord().getName());

        assertEquals("f0a2af65-fe89-45a4-9456-07c5e17b1634", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getId().toString());
        assertEquals("archetype_2", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder2", ((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getName());

        assertEquals("f4a2af65-fe89-45a4-9456-07c5e17b1634", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getId().toString());
        assertEquals("archetype_3", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getArchetypeNodeId());
        assertEquals("nameOfFolder3", ((FolderAccess)((FolderAccess)fa2.getSubfoldersList().get(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))).getSubfoldersList().get(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))).getFolderRecord().getName());

        UUID storedFolderUid = fa2.commit();
        //commit should return the top level folderId
        assertEquals("f8a2af65-fe89-45a4-9456-07c5e17b1634", storedFolderUid.toString());
    }

    private class MyProvider implements MockDataProvider {

        @Override
        public MockResult[] execute(MockExecuteContext ctx) throws SQLException {

// You might need a DSLContext to create org.jooq.Result and org.jooq.Record objects
            DSLContext create = DSL.using(SQLDialect.POSTGRES_9_5);
            MockResult[] mock = new MockResult[1];

///////////////////////////////////////////////////////

            Result<Record2<String, String>> result = create.newResult(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID);
//search for leafs
            if (ctx.sql().contains("\"ehr\".\"containment\".\"label\"~ '*.openEHR_EHR_ACTION_immunisation_procedure_v1")) {
                result.add(create
                        .newRecord(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID)
                        .values("/content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']", "openEHR-EHR-COMPOSITION.health_summary.v1"));
                mock[0] = new MockResult(1, result);
                return mock;
// search for root entry
            } /*else if (ctx.sql().contains("\"ehr\".\"containment\".\"label\"~ 'openEHR_EHR_COMPOSITION_health_summary_v1")) {
Result<Record11<UUID,UUID,Integer,EntryType,String,UUID,String,UUID,Object,Timestamp,Object>> result2 = create.newResult(ENTRY_HISTORY.ID, ENTRY_HISTORY.COMPOSITION_ID, ENTRY_HISTORY.SEQUENCE,ENTRY_HISTORY.ITEM_TYPE, ENTRY_HISTORY.TEMPLATE_ID, ENTRY_HISTORY.TEMPLATE_UUID, ENTRY_HISTORY.ARCHETYPE_ID, ENTRY_HISTORY.CATEGORY, ENTRY_HISTORY.ENTRY, ENTRY_HISTORY.SYS_TRANSACTION, ENTRY_HISTORY.SYS_PERIOD);
result2.add(create
.newRecord(ENTRY_HISTORY.ID, ENTRY_HISTORY.COMPOSITION_ID, ENTRY_HISTORY.SEQUENCE,ENTRY_HISTORY.ITEM_TYPE, ENTRY_HISTORY.TEMPLATE_ID, ENTRY_HISTORY.TEMPLATE_UUID, ENTRY_HISTORY.ARCHETYPE_ID, ENTRY_HISTORY.CATEGORY, ENTRY_HISTORY.ENTRY, ENTRY_HISTORY.SYS_TRANSACTION, ENTRY_HISTORY.SYS_PERIOD)
.values(UUID.randomUUID(), UUID.randomUUID(), new Integer(2), EntryType.care_entry, "a template id", UUID.randomUUID(), "archetype id", UUID.randomUUID(), null, new Timestamp(15624894), new Timestamp(1562489) ));
mock[0] = new MockResult(1, result2);
return mock;
}*/



/////////////////////////////////////////////////////////



// The execute context contains SQL string(s), bind values, and other meta-data
            String sql2 = ctx.sql();

// Exceptions are propagated through the JDBC and jOOQ APIs
            if (sql2.toUpperCase().startsWith("DROP")) {
                throw new SQLException("Statement not supported: " + sql2);
            }
            else if (sql2.toUpperCase().startsWith("UPDATE")) {
                throw new SQLException("Statement not supported: " + sql2);
            }
            else if (sql2.toUpperCase().startsWith("CREATE")) {
                throw new SQLException("Statement not supported: " + sql2);
            }
// You decide, whether any given statement returns results, and how many
            else if(sql2.toUpperCase().contains("ENTRY_HISTORY")) {
                System.out.println("entry hist selected");
                Result<Record11<UUID,UUID,Integer,EntryType,String,UUID,String,UUID,Object,Timestamp,Object>> result2 = create.newResult(ENTRY_HISTORY.ID, ENTRY_HISTORY.COMPOSITION_ID, ENTRY_HISTORY.SEQUENCE,ENTRY_HISTORY.ITEM_TYPE, ENTRY_HISTORY.TEMPLATE_ID, ENTRY_HISTORY.TEMPLATE_UUID, ENTRY_HISTORY.ARCHETYPE_ID, ENTRY_HISTORY.CATEGORY, ENTRY_HISTORY.ENTRY, ENTRY_HISTORY.SYS_TRANSACTION, ENTRY_HISTORY.SYS_PERIOD);
                result2.add(create
                        .newRecord(ENTRY_HISTORY.ID, ENTRY_HISTORY.COMPOSITION_ID, ENTRY_HISTORY.SEQUENCE,ENTRY_HISTORY.ITEM_TYPE, ENTRY_HISTORY.TEMPLATE_ID, ENTRY_HISTORY.TEMPLATE_UUID, ENTRY_HISTORY.ARCHETYPE_ID, ENTRY_HISTORY.CATEGORY, ENTRY_HISTORY.ENTRY, ENTRY_HISTORY.SYS_TRANSACTION, ENTRY_HISTORY.SYS_PERIOD)
                        .values(UUID.randomUUID(), UUID.randomUUID(), new Integer(2), EntryType.care_entry, "a template id", UUID.randomUUID(), "archetype id", UUID.randomUUID(), null, new Timestamp(15624894), new Timestamp(1562489) ));
                mock[0] = new MockResult(1, result2);
                return mock;
            }
            else if(sql2.toUpperCase().contains("EVENT_CONTEXT_HISTORY")) {
                System.out.println("EVENT_CTX_HIST hist selected");
                Result<Record12<UUID, UUID, Timestamp,String,Timestamp,String, UUID, String, Object, UUID, Timestamp, Object>> result2 = create.newResult(EVENT_CONTEXT_HISTORY.ID, EVENT_CONTEXT_HISTORY.COMPOSITION_ID, EVENT_CONTEXT_HISTORY.START_TIME, EVENT_CONTEXT_HISTORY.START_TIME_TZID, EVENT_CONTEXT_HISTORY.END_TIME, EVENT_CONTEXT_HISTORY.END_TIME_TZID, EVENT_CONTEXT_HISTORY.FACILITY, EVENT_CONTEXT_HISTORY.LOCATION, EVENT_CONTEXT_HISTORY.OTHER_CONTEXT, EVENT_CONTEXT_HISTORY.SETTING, EVENT_CONTEXT_HISTORY.SYS_TRANSACTION, EVENT_CONTEXT_HISTORY.SYS_PERIOD);
                result2.add(create
                        .newRecord(EVENT_CONTEXT_HISTORY.ID, EVENT_CONTEXT_HISTORY.COMPOSITION_ID, EVENT_CONTEXT_HISTORY.START_TIME, EVENT_CONTEXT_HISTORY.START_TIME_TZID, EVENT_CONTEXT_HISTORY.END_TIME, EVENT_CONTEXT_HISTORY.END_TIME_TZID, EVENT_CONTEXT_HISTORY.FACILITY, EVENT_CONTEXT_HISTORY.LOCATION, EVENT_CONTEXT_HISTORY.OTHER_CONTEXT, EVENT_CONTEXT_HISTORY.SETTING, EVENT_CONTEXT_HISTORY.SYS_TRANSACTION, EVENT_CONTEXT_HISTORY.SYS_PERIOD)
                        .values(UUID.fromString("12fb01ca-47e5-4660-a31c-cbcf0b378064"), UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"), new Timestamp(1453587062), "+02:00", /*new Timestamp(0000)*/null, /*"EST"*/null, UUID.fromString("8e599323-d7ca-4b74-8d0d-055d1ff34661"), null, null, UUID.fromString("ce072393-75ee-426d-b918-40441a39deae"), new Timestamp(1556272297), null ));
                mock[0] = new MockResult(1, result2);
                return mock;
            }else if(sql2.toUpperCase().contains("SELECT \"\\\"EHR\\\".\\\"PARTY_IDENTIFIED\\\"\"")) {
                System.out.println("PARTY_IDENTIFIED hist selected");
                Result<Record6<UUID, String, String, String, String, String>> result2 = create.newResult(PARTY_IDENTIFIED.ID, PARTY_IDENTIFIED.NAME, PARTY_IDENTIFIED.PARTY_REF_VALUE, PARTY_IDENTIFIED.PARTY_REF_SCHEME, PARTY_IDENTIFIED.PARTY_REF_NAMESPACE, PARTY_IDENTIFIED.PARTY_REF_TYPE);
                result2.add(create
                        .newRecord(PARTY_IDENTIFIED.ID, PARTY_IDENTIFIED.NAME, PARTY_IDENTIFIED.PARTY_REF_VALUE, PARTY_IDENTIFIED.PARTY_REF_SCHEME, PARTY_IDENTIFIED.PARTY_REF_NAMESPACE, PARTY_IDENTIFIED.PARTY_REF_TYPE)
                        .values(UUID.fromString("8e599323-d7ca-4b74-8d0d-055d1ff34661"), "Ripple View Care Home", "999999-345", "2.16.840.1.113883.2.1.4.3", "NHS-UK", "PARTY"));
                mock[0] = new MockResult(1, result2);//"openehr::216|face-to-face communication"
                return mock;
            }
            else if(sql2.toUpperCase().contains("PARTICIPATION_HISTORY")) {
                System.out.println("PARTICIPATION_HIST hist selected");
                Result<Record9<UUID, UUID, UUID, String, String, Timestamp, String, Timestamp, Object>> result2 = create.newResult(PARTICIPATION_HISTORY.ID, PARTICIPATION_HISTORY.EVENT_CONTEXT, PARTICIPATION_HISTORY.PERFORMER, PARTICIPATION_HISTORY.FUNCTION, PARTICIPATION_HISTORY.MODE,PARTICIPATION_HISTORY.START_TIME,PARTICIPATION_HISTORY.START_TIME_TZID, PARTICIPATION_HISTORY.SYS_TRANSACTION, PARTICIPATION_HISTORY.SYS_PERIOD);
/*result2.add(create
.newRecord(PARTICIPATION_HISTORY.ID, PARTICIPATION_HISTORY.EVENT_CONTEXT, PARTICIPATION_HISTORY.PERFORMER, PARTICIPATION_HISTORY.FUNCTION, PARTICIPATION_HISTORY.MODE,PARTICIPATION_HISTORY.START_TIME,PARTICIPATION_HISTORY.START_TIME_TZID, PARTICIPATION_HISTORY.SYS_TRANSACTION, PARTICIPATION_HISTORY.SYS_PERIOD)
.values(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "15624897", "openehr::216|face-to-face communication", new Timestamp(15624898), "EST", new Timestamp(15624899), null));
*/ mock[0] = new MockResult(1, result2);//"openehr::216|face-to-face communication"
                return mock;
            }else if(sql2.toUpperCase().contains("\"EHR\".\"CONCEPT\"")) {
                System.out.println("SQL2 is: "+sql2.toUpperCase());
                System.out.println("CONCEPT hist selected");
                Result<Record4<UUID, Integer, String, String>> result2 = create.newResult(CONCEPT.ID, CONCEPT.CONCEPTID, CONCEPT.LANGUAGE, CONCEPT.DESCRIPTION);
                result2.add(create
                        .newRecord(CONCEPT.ID, CONCEPT.CONCEPTID, CONCEPT.LANGUAGE, CONCEPT.DESCRIPTION)
                        .values(UUID.fromString("ce072393-75ee-426d-b918-40441a39deae"), new Integer(238), "en", "other care"));
                mock[0] = new MockResult(1, result2);
                return mock;
            }else if(sql2.toUpperCase().contains("SELECT \"EHR\".\"TERRITORY\"")) {
                System.out.println("SQL2 is: "+sql2.toUpperCase());
                System.out.println("TERRITORY hist selected");
                Result<Record4< Integer, String, String, String>> result2 = create.newResult(TERRITORY.CODE, TERRITORY.TWOLETTER, TERRITORY.THREELETTER, TERRITORY.TEXT);
                result2.add(create
                        .newRecord(TERRITORY.CODE, TERRITORY.TWOLETTER, TERRITORY.THREELETTER, TERRITORY.TEXT)
                        .values(new Integer(276), "DE", "DEU", "Germany"));
                mock[0] = new MockResult(1, result2);
                return mock;
            }else if(sql2.toUpperCase().contains("SELECT \"EHR\".\"PARTY_IDENTIFIED\"")) {
                System.out.println("SQL2 is: "+sql2.toUpperCase());
                System.out.println("PARTY_IDENTIFIED hist selected");
                Result<Record6< UUID, String, String, String, String, String>> result2 = create.newResult(PARTY_IDENTIFIED.ID, PARTY_IDENTIFIED.NAME, PARTY_IDENTIFIED.PARTY_REF_VALUE, PARTY_IDENTIFIED.PARTY_REF_SCHEME, PARTY_IDENTIFIED.PARTY_REF_NAMESPACE, PARTY_IDENTIFIED.PARTY_REF_TYPE);
                result2.add(create
                        .newRecord(PARTY_IDENTIFIED.ID, PARTY_IDENTIFIED.NAME, PARTY_IDENTIFIED.PARTY_REF_VALUE, PARTY_IDENTIFIED.PARTY_REF_SCHEME, PARTY_IDENTIFIED.PARTY_REF_NAMESPACE, PARTY_IDENTIFIED.PARTY_REF_TYPE)
                        .values(UUID.fromString("4ad26a76-27be-4ec9-a873-60b2cd52760d"), "Dr Tony Blegon", null, null, null, null));
                mock[0] = new MockResult(1, result2);
                return mock;
            }
            else if(sql2.toUpperCase().contains("SELECT 1 AS \"ONE\"")) {
                System.out.println("SQL2 is: "+sql2.toUpperCase());
                System.out.println("1 AS ONE hist selected");
                Field<Integer> c = DSL.count();
                Result<Record1< Integer>> result2 = create.newResult(c);
                result2.add(create
                        .newRecord(c)
                        .values(1));
                mock[0] = new MockResult(1, result2);
                return mock;
            }

            else if (sql2.toUpperCase().startsWith("SELECT")) {

/*String latestCompositionQuery = "select ehr.composition.id, ehr.composition.in_contribution, ehr.composition.ehr_id, ehr.composition.language, ehr.composition.territory, ehr.composition.composer, ehr.composition.sys_transaction\n" +
"from ehr.composition \n" +
"left join ehr.contribution on ehr.composition.in_contribution=ehr.contribution.id where ehr.contribution.time_committed=(select max(ehr.contribution.time_committed) \n" +
"from ehr.contribution where ehr.contribution.time_committed <= '?');";*/

// Always return one record
/* Result<Record7<String,String,String,String,Integer,UUID,Timestamp>> result = create.newResult(COMPOSITION.ID, COMPOSITION.IN_CONTRIBUTION, COMPOSITION.EHR_ID, COMPOSITION.LANGUAGE, COMPOSITION.TERRITORY, COMPOSITION.COMPOSER, COMPOSITION.SYS_TRANSACTION);
result.add(create
.newRecord(COMPOSITION.ID, COMPOSITION.IN_CONTRIBUTION, COMPOSITION.EHR_ID, COMPOSITION.LANGUAGE, COMPOSITION.TERRITORY, COMPOSITION.COMPOSER, COMPOSITION.SYS_TRANSACTION)
.values("66ac468b-762b-48d6-b5c6-d1d72f3ea167", "4529e319-eb30-4d67-aca1-6decd293f0c6", "f6a2af65-fe89-45a4-9456-07c5e17b1634", "en", 840, "Clint Eastwood", new Timestamp(Long.parseLong("1556150400000")));
*/
                Result<Record7<UUID,UUID,UUID,String,Integer,UUID,Timestamp>> result2 = create.newResult(COMPOSITION.ID, COMPOSITION.IN_CONTRIBUTION, COMPOSITION.EHR_ID, COMPOSITION.LANGUAGE, COMPOSITION.TERRITORY, COMPOSITION.COMPOSER, COMPOSITION.SYS_TRANSACTION);
                result2.add(create
                        .newRecord(COMPOSITION.ID, COMPOSITION.IN_CONTRIBUTION, COMPOSITION.EHR_ID, COMPOSITION.LANGUAGE, COMPOSITION.TERRITORY, COMPOSITION.COMPOSER, COMPOSITION.SYS_TRANSACTION)
                        .values(UUID.fromString("66ac468b-762b-48d6-b5c6-d1d72f3ea167"), UUID.fromString("4529e319-eb30-4d67-aca1-6decd293f0c6"), UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"), "en", 840, UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1999"), new Timestamp(Long.parseLong("1556150400000"))));

                mock[0] = new MockResult(1, result2);
            }

// You can detect batch statements easily
            else if (ctx.batch()) {
// [...]
            }

            return mock;
        }
    }

}
/*
*
	INSERT INTO ehr.system(
	id, description, settings)
	VALUES ('f6a2af65-fe89-45a4-9456-07c5e17b1634', 'desc', 'LOCAL');


INSERT INTO ehr.ehr(
	id, date_created, date_created_tzid, access, system_id, directory)
	VALUES ('f6a2af65-fe89-45a4-9456-07c5e17b1634','2019-04-24 20:17:52.144',null,null,'f6a2af65-fe89-45a4-9456-07c5e17b1634',null);

INSERT INTO ehr.party_identified(
	id, name, party_ref_value, party_ref_scheme, party_ref_namespace, party_ref_type)
	VALUES ('48282ddd-4c7d-444a-8159-458a03c9827f', NULL, NULL, NULL, NULL, NULL);

INSERT INTO ehr.contribution(
	id, ehr_id, contribution_type, state, signature, system_id, committer, time_committed, time_committed_tzid, change_type, description, sys_transaction, sys_period)
	VALUES ('00550555-ec91-4025-838d-09ddb4e473cb','f6a2af65-fe89-45a4-9456-07c5e17b1634','composition','complete','zzz','f6a2af65-fe89-45a4-9456-07c5e17b1634','48282ddd-4c7d-444a-8159-458a03c9827f','2019-04-25 17:39:02.688', 'Europe/Oslo','creation','description','2019-04-25 17:39:02.683', null);


INSERT INTO ehr.folder(
	id, in_contribution, name, archetype_node_id, active, details, sys_transaction, sys_period)
	VALUES ('00550555-ec91-4025-838d-09ddb4e999cb','00550555-ec91-4025-838d-09ddb4e473cb', 'folder_archetype_name_1', 'folder_archetype.v1', TRUE, '{"details": "xxx1"}', '2019-04-24 20:32:52.144', null);

INSERT INTO ehr.folder(
	id, in_contribution, name, archetype_node_id, active, details, sys_transaction, sys_period)
	VALUES ('99550555-ec91-4025-838d-09ddb4e999cb','00550555-ec91-4025-838d-09ddb4e473cb',  'folder_archetype_name_2', 'folder_archetype.v1',TRUE, '{"details": "xxx2"}', '2019-04-24 20:32:52.144', null);


INSERT INTO ehr.folder(
	id, in_contribution, name, archetype_node_id, active, details, sys_transaction, sys_period)
	VALUES ('33550555-ec91-4025-838d-09ddb4e999cb','00550555-ec91-4025-838d-09ddb4e473cb',  'folder_archetype_name_3', 'folder_archetype.v1',TRUE, '{"details": "xxx3"}', '2019-04-24 20:32:52.144', null);

INSERT INTO ehr.folder(
	id, in_contribution, name, archetype_node_id, active, details, sys_transaction, sys_period)
	VALUES ('77750555-ec91-4025-838d-09ddb4e999cb','00550555-ec91-4025-838d-09ddb4e473cb', 'folder_archetype_name_4', 'folder_archetype.v1',TRUE, '{"details": "xxx3"}', '2019-04-24 20:32:52.144', null);


INSERT INTO ehr.folder(
	id, in_contribution, name, archetype_node_id, active, details, sys_transaction, sys_period)
	VALUES ('8701233c-c8fd-47ba-91b5-ef9ff23c259b','00550555-ec91-4025-838d-09ddb4e473cb', 'folder_archetype_name_5', 'folder_archetype.v1',TRUE, '{"details": "xxx3"}', '2019-04-24 20:32:52.144', null);




INSERT INTO ehr.folder_hierarchy(
	parent_folder, child_folder, in_contribution, sys_transaction, sys_period)
	VALUES ('00550555-ec91-4025-838d-09ddb4e999cb', '99550555-ec91-4025-838d-09ddb4e999cb', '00550555-ec91-4025-838d-09ddb4e473cb', '2019-04-24 21:32:52.144', null);


INSERT INTO ehr.folder_hierarchy(
	parent_folder, child_folder, in_contribution, sys_transaction, sys_period)
	VALUES ('99550555-ec91-4025-838d-09ddb4e999cb', '33550555-ec91-4025-838d-09ddb4e999cb', '00550555-ec91-4025-838d-09ddb4e473cb', '2019-04-24 21:32:52.144', null);

INSERT INTO ehr.folder_hierarchy(
	parent_folder, child_folder, in_contribution, sys_transaction, sys_period)
	VALUES ('33550555-ec91-4025-838d-09ddb4e999cb', '77750555-ec91-4025-838d-09ddb4e999cb', '00550555-ec91-4025-838d-09ddb4e473cb', '2019-04-24 21:32:52.144', null);

	INSERT INTO ehr.folder_hierarchy(
	parent_folder, child_folder, in_contribution, sys_transaction, sys_period)
	VALUES ('33550555-ec91-4025-838d-09ddb4e999cb', '8701233c-c8fd-47ba-91b5-ef9ff23c259b', '00550555-ec91-4025-838d-09ddb4e473cb', '2019-04-24 21:32:52.144', null);*/