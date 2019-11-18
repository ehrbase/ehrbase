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

package org.ehrbase.dao.access.jooq;

import org.ehrbase.aql.sql.AqlResult;
import org.ehrbase.dao.access.interfaces.I_DomainAccessTest;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.ehrbase.service.CacheRule;
import org.jooq.tools.jdbc.MockResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;


public class AqlQueryHandlerTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public CacheRule cacheRule = new CacheRule();

    @Test
    public void process() throws Exception {
        AqlQueryHandler cut = new AqlQueryHandler(I_DomainAccessTest.buildDomainAccess(DSLContextHelper.buildContext(ctx -> {
            MockResult[] mock = new MockResult[1];
            mock[0] = new MockResult(0, null);
            return mock;
        }), testFolder, cacheRule), true);
        AqlResult aqlResult = cut.process("select e/ehr_id/value from EHR e LIMIT 10 OFFSET 5");
        assertThat(aqlResult.getExplain().get(0)).hasSize(3).contains("10", "5");
        assertThat(aqlResult.getExplain().get(0).get(0)).isEqualToIgnoringWhitespace("select \"ehr_join\".\"id\" as \"/ehr_id/value\" " +
                "from \"ehr\".\"entry\" right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                "right outer join \"ehr\".\"ehr\" as \"ehr_join\" on \"ehr_join\".\"id\" = \"composition_join\".\"ehr_id\" " +
                "limit ? offset ?");
    }
}