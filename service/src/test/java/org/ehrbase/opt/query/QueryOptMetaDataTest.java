/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.opt.query;

import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.KnowledgeCacheService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by christian on 5/7/2018.
 */
@Ignore
public class QueryOptMetaDataTest {

    I_KnowledgeCache knowledge;

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.put("knowledge.path.archetype", "src/test/resources/knowledge");
        props.put("knowledge.path.template", "src/test/resources/knowledge");
        props.put("knowledge.path.opt", "src/test/resources/knowledge");
        props.put("knowledge.cachelocatable", "true");
        props.put("knowledge.forcecache", "true");
        knowledge = new KnowledgeCacheService(props);

        Pattern include = Pattern.compile(".*");

        knowledge.retrieveFileMap(include, null);
    }

    @Test
    public void testQueryUpperUnbounded() throws Exception {
        Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledge.retrieveOperationalTemplate("IDCR Problem List.v1");
        List result = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new)).upperNotBounded();

        assertNotNull(result);

        assertEquals(3, result.size());
    }

    @Test
    public void testQueryUpperUnbounded2() throws Exception {
        Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledge.retrieveOperationalTemplate("IDCR - Laboratory Test Report.v0");
        List result = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new)).upperNotBounded();

        assertNotNull(result);

        assertEquals(15, result.size());
    }

    @Test
    public void testQueryType() throws Exception {
        Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledge.retrieveOperationalTemplate("IDCR Problem List.v1");
        QueryOptMetaData queryOptMetaData = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new));

        String result = queryOptMetaData.type("/content[openEHR-EHR-SECTION.problems_issues_rcp.v1]/items[openEHR-EHR-EVALUATION.problem_diagnosis.v1]/data[at0001]/items[at0012]");

        assertEquals("DV_TEXT", result);
    }

    @Test
    public void testQueryByFieldValue() throws Exception {
        Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledge.retrieveOperationalTemplate("IDCR Problem List.v1");
        QueryOptMetaData queryOptMetaData = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new));

        List result = queryOptMetaData.nodeByFieldValue("name", "Problem/Diagnosis name");

        assertEquals(1, result.size());
    }

    @Test
    public void testQueryByFieldRegexp() throws Exception {
        Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledge.retrieveOperationalTemplate("IDCR Problem List.v1");
        QueryOptMetaData queryOptMetaData = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new));

        List result = queryOptMetaData.nodeFieldRegexp("name", "/PROBLEM.*/i");

        assertEquals(3, result.size());
    }

}