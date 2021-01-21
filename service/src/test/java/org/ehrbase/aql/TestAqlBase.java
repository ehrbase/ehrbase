package org.ehrbase.aql;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.*;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;

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

        //add template to knowledgeCache
        byte[] opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/Patientenaufenthalt.opt").readAllBytes();
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/LabResults1.opt").readAllBytes();
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/Stationärer Versorgungsfall.opt").readAllBytes();
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/nested.en.v1.opt").readAllBytes();
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/Virologischer_Befund.opt").readAllBytes();
        knowledge.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/opt/Vital Signs Encounter (Composition).opt").readAllBytes();
        knowledge.addOperationalTemplate(opt);

        //tests require a terminology service
        new TerminologyServiceImp().init(); //this sets the instance variable

    }
}
