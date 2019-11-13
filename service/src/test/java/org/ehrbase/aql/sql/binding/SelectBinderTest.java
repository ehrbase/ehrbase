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

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.ehrbase.service.CacheRule;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SelectBinderTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public CacheRule cacheRule = new CacheRule();

    @Test
    public void testBind() throws Exception {
        DSLContext context = DSLContextHelper.buildContext();
        IntrospectService introspectCache = KnowledgeCacheHelper.buildKnowledgeCache(testFolder, cacheRule);
        List where = Collections.emptyList();

        //represents contain contains COMPOSITION [openEHR-EHR-COMPOSITION.health_summary.v1]
        String entryRoot = "/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value='Immunisation summary']";

        // select from LOCATABLE
        {
            PathResolver pathResolver = mock(PathResolver.class);

            //represents ... contains ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]
            when(pathResolver.pathOf("d")).thenReturn("/content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']");
            when(pathResolver.classNameOf("d")).thenReturn("ACTION");

            //represents SELECT d/description[at0001]/items[at0002]/value
            List<I_VariableDefinition> variableDefinitions = Arrays.asList(I_VariableDefinitionHelper.build("description[at0001]/items[at0002]/value", null, "d", false, false, false));


            SelectBinder cut = new SelectBinder(context, introspectCache, pathResolver, variableDefinitions, where, "local", entryRoot);

            SelectQuery<?> selectQuery = cut.bind("IDCR - Immunisation summary.v0", UUID.randomUUID());

            //CCH 191016: EHR-163 removed trailing ',value' as now the query allows canonical json return
            assertThat(selectQuery.getSQL()).isEqualTo("select (jsonb_array_elements((\"ehr\".\"entry\".\"entry\"#>>'{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}')::jsonb)#>>'{/description[at0001],/items[at0002],0,/value}') as \"/description[at0001]/items[at0002]/value\"");
        }

        // select from EHR
        {
            //represents FROM EHR e
            PathResolver pathResolver = mock(PathResolver.class);
            when(pathResolver.classNameOf("e")).thenReturn("EHR");

            //represents SELECT e/ehr_id/value
            List<I_VariableDefinition> variableDefinitions = Arrays.asList(I_VariableDefinitionHelper.build("ehr_id/value", null, "e", false, false, false));


            SelectBinder cut = new SelectBinder(context, introspectCache, pathResolver, variableDefinitions, where, "local", entryRoot);

            SelectQuery<?> selectQuery = cut.bind("IDCR - Immunisation summary.v0", UUID.randomUUID());
            assertThat(selectQuery.getSQL()).isEqualTo("select \"ehr_join\".\"id\" as \"/ehr_id/value\"");
        }

        // select from Composition
        {
            PathResolver pathResolver = mock(PathResolver.class);
            //represents  contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]
            when(pathResolver.pathOf("a")).thenReturn("/composition[openEHR-EHR-COMPOSITION.health_summary.v1]");
            when(pathResolver.classNameOf("a")).thenReturn("COMPOSITION");

            //represents SELECT a/composer/name, a/content[openEHR-EHR-ACTION.immunisation_procedure.v1]
            List<I_VariableDefinition> variableDefinitions = Arrays.asList(I_VariableDefinitionHelper.build("composer/name", null, "a", false, false, false), I_VariableDefinitionHelper.build("content[openEHR-EHR-ACTION.immunisation_procedure.v1]", null, "a", false, false, false));


            SelectBinder cut = new SelectBinder(context, introspectCache, pathResolver, variableDefinitions, where, "local", entryRoot);

            SelectQuery<?> selectQuery = cut.bind("IDCR - Immunisation summary.v0", UUID.randomUUID());
            assertThat(selectQuery.getSQL()).isEqualTo("select \"composer_ref\".\"name\" as \"/composer/name\", \"ehr\".\"entry\".\"entry\" #>> '{/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary''],/content[openEHR-EHR-ACTION.immunisation_procedure.v1]}' as \"/content[openEHR-EHR-ACTION.immunisation_procedure.v1]\"");
        }
    }

}