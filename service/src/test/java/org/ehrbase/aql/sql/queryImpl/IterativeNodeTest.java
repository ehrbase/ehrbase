/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.aql.sql.queryImpl;

import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.jooq.DSLContext;
import org.junit.BeforeClass;
import org.junit.Ignore;

import java.sql.Connection;

/**
 * Created by christian on 5/17/2018.
 */
@Ignore
public class IterativeNodeTest {

    protected static DSLContext context;
    protected static Connection connection;

    //  private static I_IntrospectCache introspectCache;
    private static I_KnowledgeCache knowledge;

    @BeforeClass
    public static void beforeClass() {
/*
        SQLDialect dialect = SQLDialect.valueOf("POSTGRES");
//        String url = "jdbc:postgresql://localhost:5434/ethercis";
        String url = "jdbc:postgresql://" + System.getProperty("test.db.host") + ":" + System.getProperty("test.db.port") + "/" + System.getProperty("test.db.name");
//        String url = "jdbc:postgresql://192.168.2.108:5432/ethercis";
        String login = System.getProperty("test.db.user");
        String password = System.getProperty("test.db.password");
        Properties props = new Properties();
        props.put("knowledge.path.archetype", "src/test/resources/knowledge/adl");
        props.put("knowledge.path.template", "src/test/resources/knowledge/oet");
        props.put("knowledge.path.opt", "src/test/resources/knowledge/opt");
        props.put("knowledge.cachelocatable", "true");
        props.put("knowledge.forcecache", "true");
        try {
            knowledge = new KnowledgeCacheService(null, props);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not set knowledge cache:" + e);
        }

        try {
            connection = DriverManager.getConnection(url, login, password);
        } catch (SQLException e) {
            throw new IllegalArgumentException("SQL exception occurred while connecting:" + e);
        }

        if (connection == null)
            throw new IllegalArgumentException("Could not connect to DB");

        context = DSL.using(connection, dialect);

        try {
         //   introspectCache = new IntrospectCache(context, knowledge).load().synchronize();

        } catch (Exception e) {
            fail("could not initialize intropection meta data cache, please check your configuration");
        }*/
    }

  /*  @Before
    public void setUp() {
        BasicConfigurator.configure();
    }

    @AfterClass
    public static void afterClass() throws SQLException {
        connection.close();
    }

    @Test
    @Ignore
    public void testIsIterative() throws Exception {

        String valuePointPath = "/content[openEHR-EHR-OBSERVATION.laboratory_test.v0]/data[at0001]/events[at0002]/data[at0003]/items[openEHR-EHR-CLUSTER.laboratory_test_panel.v0]/items[at0002]/items[at0001]/value";

        String[] ignoreIterativeOnRegexp = {"^content.*", "^events.*"};

        IterativeNode iterativeNode = new IterativeNode("IDCR - Laboratory Test Report.v0", introspectCache, Arrays.asList(ignoreIterativeOnRegexp), 1);

        Integer[] result = iterativeNode.iterativeAt(Locatable.dividePathIntoSegments(valuePointPath));

        assertEquals((Integer) 6, result[0]);

    }

    @Test
    @Ignore
    public void testClipInIterative() throws Exception {

        String valuePointPath = "/content[openEHR-EHR-OBSERVATION.laboratory_test.v0]/data[at0001]/events[at0002]/data[at0003]/items[openEHR-EHR-CLUSTER.laboratory_test_panel.v0]/items[at0002]/items[at0001]/value";

        String[] ignoreIterativeOnRegexp = {"^content.*", "^events.*"};

        IterativeNode iterativeNode = new IterativeNode("IDCR - Laboratory Test Report.v0", introspectCache, Arrays.asList(ignoreIterativeOnRegexp), 1);

        List<String> segmentedPath = Locatable.dividePathIntoSegments(valuePointPath);

        List<String> result = iterativeNode.clipInIterativeMarker(segmentedPath, iterativeNode.iterativeAt(segmentedPath));

        assertTrue(result.contains(I_QueryImpl.AQL_NODE_ITERATIVE_MARKER));

    }

    @Test
    @Ignore
    public void resolveJsonbArrayFunctionCall() throws Exception {

        List<String> segmentedPath = Arrays.asList(new String[]{
                "/composition[openEHR-EHR-COMPOSITION.adverse_reaction_list.v1 and name/value=''Adverse reaction list'']",
                "/content[openEHR-EHR-SECTION.allergies_adverse_reactions_rcp.v1]",
                "0",
                "/items[openEHR-EHR-EVALUATION.adverse_reaction_risk.v1]",
                "0",
                "/data[at0001]",
                "/items[at0009]",
                "0",
                "/items[at0011]",
                "0",
                "/value,value"
        });

        String[] ignoreIterativeOnRegexp = {"^/content.*", "^/events.*"};

        IterativeNode iterativeNode = new IterativeNode("IDCR - Adverse Reaction List.v1", introspectCache, Arrays.asList(ignoreIterativeOnRegexp), 1);

        Integer[] pos = iterativeNode.iterativeAt(segmentedPath);

        List<String> result = iterativeNode.clipInIterativeMarker(segmentedPath, pos);

        //check the generation of the SQL expression

        result = new JsonbFunctionCall(result, I_QueryImpl.AQL_NODE_ITERATIVE_MARKER, I_QueryImpl.AQL_NODE_ITERATIVE_FUNCTION).resolve();

        assertEquals(1, result.size());
        assertEquals("(jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.adverse_reaction_list.v1 and name/value=''Adverse reaction list''],/content[openEHR-EHR-SECTION.allergies_adverse_reactions_rcp.v1],0,/items[openEHR-EHR-EVALUATION.adverse_reaction_risk.v1],0,/data[at0001],/items[at0009],0,/items[at0011]}')::jsonb)#>>'{/value,value}')",
                result.get(0));

    }

    *//**
     * The expression we want to get is embedding two call to jsonb_array_elements as follows
     * <p>
     * (jsonb_array_elements((
     * (jsonb_array_elements
     * (("ehr"."entry"."entry"
     * #>>
     * '{/composition[openEHR-EHR-COMPOSITION.adverse_reaction_list.v1 and name/value=''Adverse reaction list''],/content[openEHR-EHR-SECTION.allergies_adverse_reactions_rcp.v1],0,/items[openEHR-EHR-EVALUATION.adverse_reaction_risk.v1]}'
     * )::jsonb)
     * ) #>>'{/data[at0001],/items[at0009],0,/items[at0011]}'
     * )::jsonb)
     * )::jsonb #>> '{/value, value}'
     *
     * @throws Exception
     *//*
    @Test
    @Ignore
    public void resolveJsonbArrayFunctionCall_multi_levels() throws Exception {

        List<String> segmentedPath = Arrays.asList(new String[]{
                "/composition[openEHR-EHR-COMPOSITION.adverse_reaction_list.v1 and name/value=''Adverse reaction list'']",
                "/content[openEHR-EHR-SECTION.allergies_adverse_reactions_rcp.v1]",
                "0",
                "/items[openEHR-EHR-EVALUATION.adverse_reaction_risk.v1]",
                "0",
                "/data[at0001]",
                "/items[at0009]",
                "0",
                "/items[at0011]",
                "0",
                "/value,value"
        });

        String[] ignoreIterativeOnRegexp = {"^/content.*", "^/events.*"};

        IterativeNode iterativeNode = new IterativeNode("IDCR - Adverse Reaction List.v1", introspectCache, null, 3);

        Integer[] pos = iterativeNode.iterativeAt(segmentedPath);

        List<String> result = iterativeNode.clipInIterativeMarker(segmentedPath, pos);

        //check the generation of the SQL expression

        result = new JsonbFunctionCall(result, I_QueryImpl.AQL_NODE_ITERATIVE_MARKER, I_QueryImpl.AQL_NODE_ITERATIVE_FUNCTION).resolve();

        assertEquals("(jsonb_array_elements((jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.adverse_reaction_list.v1 and name/value=''Adverse reaction list''],/content[openEHR-EHR-SECTION.allergies_adverse_reactions_rcp.v1],0,/items[openEHR-EHR-EVALUATION.adverse_reaction_risk.v1]}')::jsonb)#>>'{/data[at0001],/items[at0009],0,/items[at0011]}')::jsonb)#>>'{/value,value}')"
                , result.get(0));

    }*/
}