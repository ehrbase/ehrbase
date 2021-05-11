/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.queryimpl;

import org.apache.commons.io.IOUtils;
import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.ehr.knowledge.TemplateTestData;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectSelectStep;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MultiFieldJsonbEntryQueryTest extends TestAqlBase {

    JsonbEntryQuery cut;

    @Before
    public void setUp() throws IOException {

        //add test template with non unique paths to identified node
        knowledge.addOperationalTemplate(IOUtils.toByteArray(TemplateTestData.NON_UNIQUE_AQL_PATH.getStream()));

        String query =
                "select\n" +
                        "a/description[at0001]/items[at0002]/name/value\n" +
                        "from EHR e\n" +
                        "contains COMPOSITION c" +
                        "  CONTAINS ACTION a";

        //initialize containment resolver for path resolution
        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        PathResolver pathResolver = new PathResolver(knowledge, contains.getIdentifierMapper());

        cut = new JsonbEntryQuery(this.testDomainAccess, knowledge, pathResolver);
    }


    @Test
    public void testMakeMultiField() throws Exception {

        Field<?> actual = cut.makeField("non_unique_aql_paths", "a", I_VariableDefinitionHelper.build("description[at0001]/items[at0002]/name/value", "test", "a", false, false, false), IQueryImpl.Clause.SELECT).getFields().get(0).getSQLField();

        SelectSelectStep<? extends Record1<?>> selectQuery = DSL.select(actual);
        assertThat(selectQuery.getQuery().toString()).isEqualToIgnoringWhitespace("select ("+ QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION+"((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0002],0,/value,value}') \"test\"");
        assertThat(actual.toString()).hasToString("\"test\"");
    }
}