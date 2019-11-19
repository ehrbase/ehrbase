/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.aql.sql;

import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.ehrbase.service.CacheRule;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.CONTAINMENT;
import static org.ehrbase.jooq.pg.Tables.ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryProcessorTest {


    public static class AqlTestCase {

        private final Integer id;
        private final String query;
        private final String expectedSql;
        private final boolean outputWithJson;

        /**
         * @param id             Id
         * @param query          Input aql query
         * @param expectedSql    expected output sql
         * @param outputWithJson expected output contains json fields
         */
        AqlTestCase(Integer id, String query, String expectedSql, boolean outputWithJson) {
            this.id = id;
            this.query = query;
            this.expectedSql = expectedSql;
            this.outputWithJson = outputWithJson;
        }


        public String getQuery() {
            return query;
        }

        public String getExpectedSql() {
            return expectedSql;
        }

        public Integer getId() {
            return id;
        }

        public boolean isOutputWithJson() {
            return outputWithJson;
        }
    }

    @Rule
    public CacheRule cacheRule = new CacheRule();
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    /**
     * Builds the {@link AqlTestCase}
     *
     * @return
     */
    public static List<AqlTestCase> buildAqlTestCase() {

        List<AqlTestCase> testCases = new ArrayList<>();

        //   Select expectedSql column  from ehr
        testCases.add(new AqlTestCase(1,
                "select e/ehr_id/value from EHR e",
                "select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                        "from \"ehr\".\"entry\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\"",
                false
        ));


        //   where expectedSql column from ehr
        testCases.add(new AqlTestCase(2,
                "select e/ehr_id/value from EHR e " +
                        "where e/ehr_id/value = '30580007' ",
                "select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                        "from \"ehr\".\"entry\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\"" +
                        "where (\"ehr_join\".\"id\"='30580007')",
                false));


        // Select expectedSql column from composition
        testCases.add(new AqlTestCase(3,
                "select c/composer/name from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]",
                "select \"composer_ref\".\"name\" as \"/composer/name\" " +
                        "from \"ehr\".\"entry\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "join \"ehr\".\"party_identified\" as \"composer_ref\" on \"composition_join\".\"composer\" = \"composer_ref\".\"id\" where \"ehr\".\"entry\".\"template_id\" = ?",
                false));

        // order  by clause on expectedSql column from composition
        testCases.add(new AqlTestCase(4,
                "select c/composer/name from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1] " +
                        "order by c/context/start_time/value DESC",
                "select \"composer_ref\".\"name\" as \"/composer/name\" " +
                        "from \"ehr\".\"entry\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "join \"ehr\".\"party_identified\" as \"composer_ref\" on \"composition_join\".\"composer\" = \"composer_ref\".\"id\" " +
                        "where \"ehr\".\"entry\".\"template_id\" = ? order by c desc",
                false));


        // Select json column  from entry
        testCases.add(new AqlTestCase(5,
                "select a/description[at0001]/items[at0002]/value/value from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                "select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0002],0,/value,value}') as \"/description[at0001]/items[at0002]/value/value\" " +
                        "from \"ehr\".\"entry\" " +
                        "where \"ehr\".\"entry\".\"template_id\" = ?",
                true));

        // order  by clause json column  from entry with alias
        testCases.add(new AqlTestCase(6,
                "select a/description[at0001]/items[at0002]/value/value as description from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  " +
                        "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]" +
                        "order by description ASC",
                "select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0002],0,/value,value}') as \"description\" " +
                        "from \"ehr\".\"entry\" " +
                        "where \"ehr\".\"entry\".\"template_id\" = ? " +
                        "order by description asc",
                true));

        // where  clause json column  from entry
        testCases.add(new AqlTestCase(7,
                "select a/description[at0001]/items[at0002]/value/value from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  " +
                        "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]" +
                        "where a/description[at0001]/items[at0002]/value/value = 'Hepatitis A'",
                "select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0002],0,/value,value}') as \"/description[at0001]/items[at0002]/value/value\" " +
                        "from \"ehr\".\"entry\" " +
                        "where (\"ehr\".\"entry\".\"template_id\" = ? " +
                        "and (\"ehr\".\"entry\".\"entry\" @@ '\"/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary'']\".\"/content[openEHR-EHR-ACTION.immunisation_procedure.v1]\".#.\"/description[at0001]\".\"/items[at0002]\".#.\"/value\".\"value\"=\"Hepatitis A\" '::jsquery))",
                true));

        // select full composition
        testCases.add(new AqlTestCase(8,
                "select c from EHR e contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]",
                "select ehr.js_composition(composition_join.id, 'local')::text as \"c\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "where \"ehr\".\"entry\".\"template_id\" = ?",
                true));


        // select full entry
        testCases.add(new AqlTestCase(9,
                "select a from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  " +
                        "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                "select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{}') as \"a\"" +
                        "from \"ehr\".\"entry\" where \"ehr\".\"entry\".\"template_id\" = ?",
                true));

        // select Limit and Offset
        testCases.add(new AqlTestCase(10,
                "select e/ehr_id/value from EHR e LIMIT 10 OFFSET 5",
                "select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                        "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                        "limit ? offset ?",
                false));

        // select TOP
        testCases.add(new AqlTestCase(11,
                "select TOP 5 e/ehr_id/value from EHR e ",
                "select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                        "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                        "limit ?",
                false));

        // where clausal json column from entry  with matches
        testCases.add(new AqlTestCase(12,
                "select e/ehr_id/value from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  " +
                        "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]" +
                        "where a/description[at0001]/items[at0002]/value/value matches {'Hepatitis A','Hepatitis B'} ",
                "select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                        "from \"ehr\".\"entry\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                        "where (\"ehr\".\"entry\".\"template_id\" = ? " +
                        "and (\"ehr\".\"entry\".\"entry\" @@ '\"/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary'']\".\"/content[openEHR-EHR-ACTION.immunisation_procedure.v1]\".#.\"/description[at0001]\".\"/items[at0002]\".#.\"/value\".\"value\" IN (\"Hepatitis A\",\"Hepatitis B\") '::jsquery))",
                true));

        // select with function
        testCases.add(new AqlTestCase(13,
                "select  max (d/description[at0001]/items[at0004]/value/magnitude) as max_magnitude from EHR e  contains COMPOSITION  contains ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                "select max(\"max_magnitude\") as \"max_magnitude\" " +
                        "from (select ((jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0004],0,/value,magnitude}'))::numeric as \"max_magnitude\" " +
                        "from \"ehr\".\"entry\" " +
                        "where \"ehr\".\"entry\".\"template_id\" = ?" +
                        ") as \"\"",
                true));

        // Select  from unknown  composition
        testCases.add(new AqlTestCase(14,
                "select c/composer/name from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.unknown.v1]",
                //Do to the way the query is build it is not possible to build sql if the AQL contains only compositions which have no instances in the DB. Thus we must manual handle this case.
                "select 1 where 1 = 0",
                false));

        return testCases;
    }


//    private static AqlExpression parseAql(String query) {
//
//        AqlExpression queryParser = new AqlExpression(query).parse();
//        queryParser.pass1();
//        queryParser.pass2();
//        return queryParser;
//    }


    @Test
    public void testBuildAqlSelectQuery() throws Exception {
        IntrospectService introspectCache = KnowledgeCacheHelper.buildKnowledgeCache(testFolder, cacheRule);

        /** mocks the sql query such that there simulate a table  ehr.containment as
         *   comp_id    |   label                                                                               |   path
         *   ?          | openEHR_EHR_COMPOSITION_health_summary_v1                                             |  /composition[openEHR-EHR-COMPOSITION.health_summary.v1]
         *   ?          | openEHR_EHR_COMPOSITION_health_summary_v1.openEHR_EHR_ACTION_immunisation_procedure_v1| /content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']
         */
        DSLContext context = DSLContextHelper.buildContext(ctx -> {
            DSLContext create = DSLContextHelper.buildContext();
            MockResult[] mock = new MockResult[1];

            //search for leafs
            if (ctx.sql().contains("entry_root")) {

                Result<Record4<String, UUID, Object, Object>> result = create.newResult(ENTRY.TEMPLATE_ID, CONTAINMENT.COMP_ID, CONTAINMENT.LABEL, DSL.field("entry_root"));
                //If AqlTestCase 13 then return empty results
                if (!ctx.sql().contains("unknown")) {
                    result.add(create.newRecord(ENTRY.TEMPLATE_ID, CONTAINMENT.COMP_ID, CONTAINMENT.LABEL, DSL.field("entry_root")).values("IDCR - Immunisation summary.v0", UUID.fromString("8a33ca66-705d-4115-9483-52c3350f2135"), "openEHR_EHR_COMPOSITION_health_summary_v1.openEHR_EHR_ACTION_immunisation_procedure_v1", "/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value='Immunisation summary']"));
                }
                mock[0] = new MockResult(1, result);
            } else if (ctx.sql().contains("\"ehr\".\"containment\".\"label\"~ '*.openEHR_EHR_ACTION_immunisation_procedure_v1")) {
                Result<Record2<String, String>> result = create.newResult(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID);
                result.add(create
                        .newRecord(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID)
                        .values("/content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']", "openEHR-EHR-COMPOSITION.health_summary.v1"));
                mock[0] = new MockResult(1, result);
                // search for root entry
            } else if (ctx.sql().contains("\"ehr\".\"containment\".\"label\"~ 'openEHR_EHR_COMPOSITION_health_summary_v1")) {
                Result<Record2<String, String>> result = create.newResult(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID);
                result.add(create
                        .newRecord(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID)
                        .values("/composition[openEHR-EHR-COMPOSITION.health_summary.v1]", "openEHR-EHR-COMPOSITION.health_summary.v1"));
                mock[0] = new MockResult(1, result);
            }

            return mock;
        });

//        QueryProcessor cut = new QueryProcessor(context, null, introspectCache, containInterpreter, statementInterpreter, getServerNodeId());

        buildAqlTestCase().stream().forEach(
                t -> {
                    AqlExpression aqlExpression = new AqlExpression().parse(t.getQuery());
                    Contains contains = new Contains(aqlExpression.getParseTree()).process();
                    Statements statements = new Statements(aqlExpression.getParseTree(), contains.getIdentifierMapper()).process();

                    QueryProcessor cut = new QueryProcessor(context, null, introspectCache, contains, statements, "local", true);

                    QueryProcessor.AqlSelectQuery actual = cut.buildAqlSelectQuery();
                    // check that generated sql is expected sql
                    assertThat(removeAlias(actual.getSelectQuery().getSQL())).as(String.format("Test Case %d", t.getId())).isEqualToIgnoringWhitespace(removeAlias(t.getExpectedSql()));
                    //check if
                    assertThat(actual.isOutputWithJson()).as(String.format("Test Case %d", t.getId())).isEqualTo(t.isOutputWithJson());

                });

    }

    private String removeAlias(String s) {
        return s.replaceAll("alias_\\d+", "");
    }
}