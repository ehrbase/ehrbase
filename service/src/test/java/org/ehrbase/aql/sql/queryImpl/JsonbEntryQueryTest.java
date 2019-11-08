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

package org.ehrbase.aql.sql.queryImpl;

import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.ehrbase.service.CacheRule;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonbEntryQueryTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public CacheRule cacheRule = new CacheRule();

    @Test
    public void testMakeField() throws Exception {
        DSLContext context = DSLContextHelper.buildContext();
        IntrospectService introspectCache = KnowledgeCacheHelper.buildKnowledgeCache(testFolder, cacheRule);

        PathResolver pathResolver = mock(PathResolver.class);
        when(pathResolver.pathOf("a")).thenReturn("/composition[openEHR-EHR-COMPOSITION.health_summary.v1]");
        when(pathResolver.pathOf("d")).thenReturn("/content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']");
        String entryRoot = "/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value='Immunisation summary']";
        JsonbEntryQuery cut = new JsonbEntryQuery(context, introspectCache, pathResolver, entryRoot);

        //CCH 191016: EHR-163 required trailing '/value' as now the query allows canonical json return
        Field<?> actual = cut.makeField("IDCR - Immunisation summary.v0", UUID.randomUUID(), "d", I_VariableDefinitionHelper.build("description[at0001]/items[at0002]/value/value", "test", "d", false, false, false), I_QueryImpl.Clause.SELECT);

//        assertThat(actual.toString()).isEqualTo("(jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0002],0,/value,value}')");
        assertThat(actual.toString()).isEqualTo("\"test\"");
    }

}