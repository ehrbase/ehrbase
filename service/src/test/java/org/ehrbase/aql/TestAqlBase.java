/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql;

import java.io.FileInputStream;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.ehrbase.service.CacheRule;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.ehrbase.service.KnowledgeCacheService;
import org.ehrbase.service.TerminologyServiceImp;
import org.jooq.DSLContext;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

public class TestAqlBase {

    protected static I_DomainAccess testDomainAccess;
    protected static KnowledgeCacheService knowledge;

    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();

    @ClassRule
    public static CacheRule cacheRule = new CacheRule();

    @BeforeClass
    public static void beforeClass() throws Exception {

        DSLContext context = DSLContextHelper.buildContext();
        knowledge = KnowledgeCacheHelper.buildKnowledgeCache(testFolder, cacheRule);
        try {
            testDomainAccess = new DummyDataAccess(context, knowledge, null, KnowledgeCacheHelper.buildServerConfig());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // add template to knowledgeCache
        var opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/Patientenaufenthalt.opt");
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/LabResults1.opt");
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream(
                "./src/test/resources/knowledge/operational_templates/Stationärer Versorgungsfall.opt");
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/nested.en.v1.opt");
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/Virologischer_Befund.opt");
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/opt/Vital Signs Encounter (Composition).opt");
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/opt/minimal_instruction.opt");
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/opt/ehrbase_blood_pressure_simple.de.v0.opt");
        knowledge.addOperationalTemplate(opt);
        // tests require a terminology service
        new TerminologyServiceImp().init(); // this sets the instance variable
    }
}
