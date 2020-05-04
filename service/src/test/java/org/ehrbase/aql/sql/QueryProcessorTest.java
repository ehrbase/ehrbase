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

import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.Contains;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.ehrbase.jooq.pg.Tables.CONTAINMENT;
import static org.ehrbase.jooq.pg.Tables.ENTRY;

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

        String getExpectedSql() {
            return expectedSql;
        }

        public Integer getId() {
            return id;
        }

        boolean isOutputWithJson() {
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
     * @return List of {@link AqlTestCase}
     */
    private static List<AqlTestCase> buildAqlTestCase() {

        List<AqlTestCase> testCases = new ArrayList<>();

        //   Select expectedSql column  from ehr
        testCases.add(new AqlTestCase(1,
                "select e/ehr_id/value from EHR e",
                "select distinct on (\"/ehr_id/value\") \"\".\"/ehr_id/value\" from (" +
                        "select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                        "from \"ehr\".\"entry\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\"" +
                        ") as \"\"",
                false
        ));


        //   where expectedSql column from ehr
        testCases.add(new AqlTestCase(2,
                "select e/ehr_id/value from EHR e " +
                        "where e/ehr_id/value = '30580007' ",
                "select distinct on (\"/ehr_id/value\") \"\".\"/ehr_id/value\" from (" +
                        "select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                        "from \"ehr\".\"entry\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\"" +
                        "where (\"ehr_join\".\"id\"='30580007')" +
                        ") as \"\"",
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
                "select \"\".\"/composer/name\", \"\".\"/context/start_time/value\" from (select \"composer_ref\".\"name\" as \"/composer/name\"," +
                        "timezone(COALESCE(event_context.START_TIME_TZID::text,'UTC'),\"ehr\".\"event_context\".\"start_time\"::timestamp)" +
                        "as \"/context/start_time/value\" " +
                        "from \"ehr\".\"entry\" join \"ehr\".\"event_context\" " +
                        "on \"ehr\".\"event_context\".\"composition_id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" " +
                        "on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "join \"ehr\".\"party_identified\" as \"composer_ref\" " +
                        "on \"composition_join\".\"composer\" = \"composer_ref\".\"id\" " +
                        "where \"ehr\".\"entry\".\"template_id\" = ?) as \"\" " +
                        "order by \"/context/start_time/value\" desc",
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
                "select \"\".\"description\" " +
                        "from (" +
                        "select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0002],0,/value,value}') " +
                        "as \"description\" from \"ehr\".\"entry\" where \"ehr\".\"entry\".\"template_id\" = ?" +
                        ") as \"\" order by \"description\" asc",
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
                "select distinct on (\"/ehr_id/value\") \"\".\"/ehr_id/value\" from (" +
                        "select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                        "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                        ") as \"\"" +
                        "limit ? offset ?",
                false));

        // select TOP
        testCases.add(new AqlTestCase(11,
                "select TOP 5 e/ehr_id/value from EHR e ",
                "select distinct on (\"/ehr_id/value\") \"\".\"/ehr_id/value\" from (" +
                        "select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                        "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                        ") as \"\"" +
                        " limit ?",
                false));

        // where clausal json column from entry  with matches
        //CHC 10.3.20: the 'IN' operator implies that the condition is SQL instead of JSQUERY
        testCases.add(new AqlTestCase(12,
                "select e/ehr_id/value from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  " +
                        "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]" +
                        "where a/description[at0001]/items[at0002]/value/value matches {'Hepatitis A','Hepatitis B'} ",
                "select distinct on (\"/ehr_id/value\") \"\".\"/ehr_id/value\" " +
                        "from (" +
                        "select \"ehr_join\".\"id\" as \"/ehr_id/value\" from \"ehr\".\"entry\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                        "where (\"ehr\".\"entry\".\"template_id\" = ? and (\"ehr\".\"entry\".\"entry\" #>> '{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1],0,/description[at0001],/items[at0002],0,/value,value}' IN ('Hepatitis A','Hepatitis B')))) as \"\"",
                true));

        // select with function
        testCases.add(new AqlTestCase(13,
                "select  count (d/description[at0001]/items[at0004]/value/magnitude) as count_magnitude from EHR e  contains COMPOSITION  contains ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                "select count(\"count_magnitude\") as \"count_magnitude\" " +
                        "from (select ((jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0004],0,/value,magnitude}'))::numeric as \"count_magnitude\" " +
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

        //WHERE clause from node predicate
        testCases.add(new AqlTestCase(15,
                "select e/ehr_status/other_details from EHR e[ehr_id/value='2a3b673f-d1b1-44c5-9e38-dcadf67ff2fc']",
                "select distinct on (\"/ehr_status/other_details\") \"\".\"/ehr_status/other_details\" " +
                        "from (" +
                        "select ehr.js_ehr_status(\"status_join\".\"ehr_id\")::json #>>'{other_details}' as \"/ehr_status/other_details\" " +
                        "from \"ehr\".\"entry\" " +
                        "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                        "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                        "join \"ehr\".\"status\" as \"status_join\" on \"status_join\".\"ehr_id\" = \"ehr_join\".\"id\" " +
                        "where (\"ehr_join\".\"id\"='2a3b673f-d1b1-44c5-9e38-dcadf67ff2fc')) as \"\"",
                true
        ));

//        where with parenthesis
        testCases.add(new AqlTestCase(16,
                "select a/description[at0001]/items[openEHR-EHR-CLUSTER.test_all_types.v1]/items[at0001]/items[at0002]/items[at0003]/value/value " +
                        "from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  " +
                        "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]" +
                        "WHERE a/description[at0001]/items[at0001]/items[at0002]/items[at0003]/value/value = true " +
                        "OR " +
                        "(" +
                        "a/description[at0001]/items[at0001]/items[at0002]/items[at0003]/value/value = true " +
                        "AND" +
                        " a/description[at0001]/items[at0001]/items[at0002]/items[at0003]/value/value = true" +
                        ")",
                "select " +
                        "(jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[openEHR-EHR-CLUSTER.test_all_types.v1],0,/items[at0001],0,/items[at0002],0,/items[at0003],0,/value,value}') as \"/description[at0001]/items[openEHR-EHR-CLUSTER.test_all_types.v1]/items[at0001]/items[at0002]/items[at0003]/value/value\" from \"ehr\".\"entry\" where (\"ehr\".\"entry\".\"template_id\" = ? and (\"ehr\".\"entry\".\"entry\" @@ '\"/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary'']\".\"/content[openEHR-EHR-ACTION.immunisation_procedure.v1]\".#.\"/description[at0001]\".\"/items[at0001]\".#.\"/items[at0002]\".#.\"/items[at0003]\".#.\"/value\".\"value\"=true '::jsquery " +
                        "OR " +
                        "(" +
                        "\"ehr\".\"entry\".\"entry\" @@ '\"/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary'']\".\"/content[openEHR-EHR-ACTION.immunisation_procedure.v1]\".#.\"/description[at0001]\".\"/items[at0001]\".#.\"/items[at0002]\".#.\"/items[at0003]\".#.\"/value\".\"value\"=true '::jsquery " +
                        "AND " +
                        "\"ehr\".\"entry\".\"entry\" @@ '\"/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary'']\".\"/content[openEHR-EHR-ACTION.immunisation_procedure.v1]\".#.\"/description[at0001]\".\"/items[at0001]\".#.\"/items[at0002]\".#.\"/items[at0003]\".#.\"/value\".\"value\"=true'::jsquery" +
                        ")" +
                        ")" +
                        ")",
                true
        ));

        // select where from parenthese
        testCases.add(new AqlTestCase(17,
                "select a from EHR e [ehr_id/value = '4a7c01cf-bb1c-4d3d-8385-4ae0674befb1']" +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  " +
                        "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                "select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{}') as \"a\" " +
                        "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                        "where (\"ehr\".\"entry\".\"template_id\" = ? " +
                        "and (\"ehr_join\".\"id\"='4a7c01cf-bb1c-4d3d-8385-4ae0674befb1'))",
                true));

        // select where from parenthese and where
        testCases.add(new AqlTestCase(18,
                "select a from EHR e [ehr_id/value = '4a7c01cf-bb1c-4d3d-8385-4ae0674befb1']" +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  " +
                        "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1] " +
                        "where c/template_id='openEHR-EHR-COMPOSITION.health_summary.v1'",
                "select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{}') as \"a\" " +
                        "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                        "where (\"ehr\".\"entry\".\"template_id\" = ? " +
                        "and (\"ehr\".\"entry\".\"template_id\"='openEHR-EHR-COMPOSITION.health_summary.v1' " +
                        "and \"ehr_join\".\"id\"='4a7c01cf-bb1c-4d3d-8385-4ae0674befb1'))",
                true));


        testCases.add(new AqlTestCase(19,
                "select c/category from EHR e [ehr_id/value = '4a7c01cf-bb1c-4d3d-8385-4ae0674befb1']" +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]",
                "select ehr.js_concept(\"ehr\".\"entry\".\"category\")::text as \"/category\" " +
                        "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\"" +
                        "where (\"ehr\".\"entry\".\"template_id\" = ? and (\"ehr_join\".\"id\"='4a7c01cf-bb1c-4d3d-8385-4ae0674befb1'))",
                true));

        testCases.add(new AqlTestCase(20,
                "select c/category/defining_code from EHR e [ehr_id/value = '4a7c01cf-bb1c-4d3d-8385-4ae0674befb1']" +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]",
                "select ehr.js_concept(\"ehr\".\"entry\".\"category\")::json #>>'{defining_code}' as \"/category/defining_code\" " +
                        "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\"" +
                        "where (\"ehr\".\"entry\".\"template_id\" = ? and (\"ehr_join\".\"id\"='4a7c01cf-bb1c-4d3d-8385-4ae0674befb1'))",
                true));


        testCases.add(new AqlTestCase(21,
                "select c/category/defining_code/terminology_id/value from EHR e [ehr_id/value = '4a7c01cf-bb1c-4d3d-8385-4ae0674befb1']" +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]",
                "select ehr.js_concept(\"ehr\".\"entry\".\"category\")::json #>>'{defining_code,terminology_id,value}' as \"/category/defining_code/terminology_id/value\" " +
                        "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\"" +
                        "where (\"ehr\".\"entry\".\"template_id\" = ? and (\"ehr_join\".\"id\"='4a7c01cf-bb1c-4d3d-8385-4ae0674befb1'))",
                false));


        testCases.add(new AqlTestCase(22,
                "select count(a/description[at0001]/items[openEHR-EHR-CLUSTER.test_all_types.v1]/items[at0001]/items[at0002]/items[at0003]/value/value," +
                        "a/description[at0001]/items[openEHR-EHR-CLUSTER.test_all_types.v1]/items[at0001]/items[at0002]/items[at0004]/value/value)" +
                        "from EHR e " +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]  " +
                        "contains ACTION a[openEHR-EHR-ACTION.immunisation_procedure.v1]",
                "select count(\"_FCT_ARG_0\",\"_FCT_ARG_1\") as \"count\" from (select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[openEHR-EHR-CLUSTER.test_all_types.v1],0,/items[at0001],0,/items[at0002],0,/items[at0004],0,/value,value}') as \"_FCT_ARG_1\", (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[openEHR-EHR-CLUSTER.test_all_types.v1],0,/items[at0001],0,/items[at0002],0,/items[at0003],0,/value,value}') as \"_FCT_ARG_0\" from \"ehr\".\"entry\" where \"ehr\".\"entry\".\"template_id\" = ?) as \"\"",
                true
        ));

        testCases.add(new AqlTestCase(23,
                "SELECT count(e/time_created) FROM EHR e",
                "select count(DISTINCT \"_FCT_ARG_0\") as \"count\" from (select ehr.js_dv_date_time(\"ehr_join\".\"date_created\",\"ehr_join\".\"date_created_tzid\")::text as \"_FCT_ARG_0\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\") as \"\"",
                true
        ));

        //EXISTS
        testCases.add(new AqlTestCase(24,
                "select c\n" +
                        "from EHR e\n" +
                        "contains COMPOSITION c[openEHR-EHR-COMPOSITION.health_summary.v1]\n" +
                        "WHERE NOT EXISTS c/content[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]",
                "select ehr.js_composition(composition_join.id,'local')::text as \"c\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" where (\"ehr\".\"entry\".\"template_id\" = ? and (\"ehr\".\"entry\".\"entry\" #>> '{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]}'IS NULL))",
                true
        ));

        //NOT EXISTS
//        testCases.add(new AqlTestCase(25,
//                "select c\n" +
//                        "from EHR e\n" +
//                        "contains COMPOSITION c\n" +
//                        "WHERE NOT EXISTS u[openEHR-EHR-ADMIN_ENTRY.hospitalization.v0]",
//                "select count(DISTINCT \"_FCT_ARG_0\") as \"count\" from (select ehr.js_dv_date_time(\"ehr_join\".\"date_created\",\"ehr_join\".\"date_created_tzid\")::text as \"_FCT_ARG_0\" from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\") as \"\"",
//                true
//        ));

        return testCases;
    }


//    private static AqlExpression parseAql(String query) {
//
//        AqlExpression queryParser = new AqlExpression(query).parse();
//        queryParser.pass1();
//        queryParser.pass2();
//        return queryParser;
//    }

    /**
     * mocks the sql query such that there simulate a table  ehr.containment as
     * comp_id    |   label                                                                               |   path
     * ?          | openEHR_EHR_COMPOSITION_health_summary_v1                                             |  /composition[openEHR-EHR-COMPOSITION.health_summary.v1]
     * ?          | openEHR_EHR_COMPOSITION_health_summary_v1.openEHR_EHR_ACTION_immunisation_procedure_v1| /content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testBuildAqlSelectQuery() throws Exception {
        IntrospectService introspectCache = KnowledgeCacheHelper.buildKnowledgeCache(testFolder, cacheRule);

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