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

import org.ehrbase.test_data.operationaltemplate.OperationalTemplateTestData;
import org.junit.Test;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by christian on 5/7/2018.
 */

public class QueryOptMetaDataTest {



    @Test
    public void testQueryUpperUnbounded() throws Exception {
        Optional<OPERATIONALTEMPLATE> operationaltemplate = Optional.ofNullable(TemplateDocument.Factory.parse(OperationalTemplateTestData.IDCR_PROBLEM_LIST.getStream()).getTemplate());
        List result = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new)).upperNotBounded();

        assertNotNull(result);

        assertEquals(3, result.size());
    }

    @Test
    public void testQueryUpperUnbounded2() throws Exception {
        Optional<OPERATIONALTEMPLATE> operationaltemplate = Optional.ofNullable(TemplateDocument.Factory.parse(OperationalTemplateTestData.IDCR_LABORATORY_TEST.getStream()).getTemplate());

        List result = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new)).upperNotBounded();

        assertNotNull(result);

        assertEquals(15, result.size());
    }

    @Test
    public void testQueryType() throws Exception {

        Optional<OPERATIONALTEMPLATE> operationaltemplate = Optional.ofNullable(TemplateDocument.Factory.parse(OperationalTemplateTestData.IDCR_PROBLEM_LIST.getStream()).getTemplate());

        QueryOptMetaData queryOptMetaData = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new));

        String result = queryOptMetaData.type("/content[openEHR-EHR-SECTION.problems_issues_rcp.v1]/items[openEHR-EHR-EVALUATION.problem_diagnosis.v1]/data[at0001]/items[at0012]");

        assertEquals("DV_TEXT", result);
    }

    @Test
    public void testQueryByFieldValue() throws Exception {

        Optional<OPERATIONALTEMPLATE> operationaltemplate = Optional.ofNullable(TemplateDocument.Factory.parse(OperationalTemplateTestData.IDCR_PROBLEM_LIST.getStream()).getTemplate());

        QueryOptMetaData queryOptMetaData = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new));

        List result = queryOptMetaData.nodeByFieldValue("name", "Problem/Diagnosis name");

        assertEquals(1, result.size());
    }

    @Test
    public void testQueryByFieldRegexp() throws Exception {
        Optional<OPERATIONALTEMPLATE> operationaltemplate = Optional.ofNullable(TemplateDocument.Factory.parse(OperationalTemplateTestData.IDCR_PROBLEM_LIST.getStream()).getTemplate());

        QueryOptMetaData queryOptMetaData = QueryOptMetaData.initialize(operationaltemplate.orElseThrow(Exception::new));

        List result = queryOptMetaData.nodeFieldRegexp("name", "/PROBLEM.*/i");

        assertEquals(3, result.size());
    }

}