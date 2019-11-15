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

import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryImpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryImpl.JsonbEntryQuery;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.ehrbase.service.CacheRule;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhereBinderTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public CacheRule cacheRule = new CacheRule();

    @Test
    public void testBind() throws Exception {
        DSLContext context = DSLContextHelper.buildContext();
        IntrospectService introspectCache = KnowledgeCacheHelper.buildKnowledgeCache(testFolder, cacheRule);
        //represents contains COMPOSITION [openEHR-EHR-COMPOSITION.health_summary.v1]
        String entryRoot = "/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value='Immunisation summary']";

        //  where clause with one condition
        {
            PathResolver pathResolver = mock(PathResolver.class);
            //represents contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]
            when(pathResolver.pathOf("a")).thenReturn("/composition[openEHR-EHR-COMPOSITION.health_summary.v1]");
            when(pathResolver.classNameOf("a")).thenReturn("COMPOSITION");
            IdentifierMapper identifierMapper = mock(IdentifierMapper.class);
            when(identifierMapper.getClassName("a")).thenReturn("COMPOSITION");

            JsonbEntryQuery jsonbEntryQuery = new JsonbEntryQuery(context, introspectCache, pathResolver, entryRoot);
            CompositionAttributeQuery compositionAttributeQuery = new CompositionAttributeQuery(context, pathResolver, "local", "entry_root", introspectCache);

            //represents where a/composer/name =  'Tony Stark'
            List where = Arrays.asList(I_VariableDefinitionHelper.build("composer/name", null, "a", false, false, false), "=", "'Tony Stark'");

            WhereBinder cut = new WhereBinder(jsonbEntryQuery, compositionAttributeQuery, where, identifierMapper);

            Condition actual = cut.bind("IDCR - Immunisation summary.v0", UUID.randomUUID());
            assertThat(actual.toString()).isEqualTo("(\"composer_ref\".\"name\"='Tony Stark')");
        }

        //   where clause with boolean operator
        {

            //represents contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1] contains ACTION d[openEHR-EHR-ACTION.immunisation_procedure.v1]
            PathResolver pathResolver = mock(PathResolver.class);
            when(pathResolver.pathOf("a")).thenReturn("/composition[openEHR-EHR-COMPOSITION.health_summary.v1]");
            when(pathResolver.classNameOf("a")).thenReturn("COMPOSITION");
            when(pathResolver.pathOf("d")).thenReturn("/content[openEHR-EHR-ACTION.immunisation_procedure.v1 and name/value='Immunisation procedure']");
            when(pathResolver.classNameOf("d")).thenReturn("ACTION");
            IdentifierMapper identifierMapper = mock(IdentifierMapper.class);
            when(identifierMapper.getClassName("a")).thenReturn("COMPOSITION");
            when(identifierMapper.getClassName("d")).thenReturn("ACTION");

            JsonbEntryQuery jsonbEntryQuery = new JsonbEntryQuery(context, introspectCache, pathResolver, entryRoot);
            CompositionAttributeQuery compositionAttributeQuery = new CompositionAttributeQuery(context, pathResolver, "local", "entry_root", introspectCache);

            //represents where a/composer/name =  'Tony Stark' and  d/description[at0001]/items[at0002]/value = 'Hepatitis A'
            //CCH 191016: EHR-163 required trailing '/value' as now the query allows canonical json return
            List where = Arrays.asList(I_VariableDefinitionHelper.build("composer/name", null, "a", false, false, false), "=", "'Tony Stark'", "and", I_VariableDefinitionHelper.build("description[at0001]/items[at0002]/value/value", null, "d", false, false, false), "=", "'Hepatitis A'");

            WhereBinder cut = new WhereBinder(jsonbEntryQuery, compositionAttributeQuery, where, identifierMapper);

            Condition actual = cut.bind("IDCR - Immunisation summary.v0", UUID.randomUUID());
            assertThat(actual.toString()).isEqualTo("(\n" +
                    "  (\"composer_ref\".\"name\"='Tony Stark')\n" +
                    "  and (\"ehr\".\"entry\".\"entry\" @@ '\"/composition[openEHR-EHR-COMPOSITION.health_summary.v1 and name/value=''Immunisation summary'']\".\"/content[openEHR-EHR-ACTION.immunisation_procedure.v1]\".#.\"/description[at0001]\".\"/items[at0002]\".#.\"/value\".\"value\"=\"Hepatitis A\" '::jsquery)\n" +
                    ")");
        }
    }

}