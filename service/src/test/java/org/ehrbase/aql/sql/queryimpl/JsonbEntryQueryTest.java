/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH)and Hannover Medical School.
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

package org.ehrbase.aql.sql.queryimpl;

import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class JsonbEntryQueryTest extends TestAqlBase {



    @Test
    public void testMakeField() {
        DSLContext context = DSLContextHelper.buildContext();

        String query =
                "select\n" +
                        "a, d\n" +
                        "from EHR e\n" +
                        "contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]" +
                        "  CONTAINS ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]";

        AqlExpression aqlExpression = new AqlExpression().parse(query);
        Contains contains = new Contains(aqlExpression.getParseTree(), knowledge).process();

        PathResolver pathResolver = new PathResolver(knowledge, contains.getIdentifierMapper());
         String entryRoot = "/composition[openEHR-EHR-COMPOSITION.health_summary.v1]";
        JsonbEntryQuery cut = new JsonbEntryQuery(testDomainAccess, knowledge, pathResolver);

        //CCH 191016: EHR-163 required trailing '/value' as now the query allows canonical json return
        Field<?> actual = cut.makeField("IDCR - Immunisation summary.v0", "d", I_VariableDefinitionHelper.build("description[at0001]/items[at0002]/value/value", "test", "d", false, false, false), IQueryImpl.Clause.SELECT);

        SelectSelectStep<? extends Record1<?>> selectQuery = DSL.select(actual);
        assertThat(selectQuery.getQuery().toString()).hasToString("select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0002],0,/value,value}') \"test\"");
        assertThat(actual.toString()).hasToString("\"test\"");
    }

}