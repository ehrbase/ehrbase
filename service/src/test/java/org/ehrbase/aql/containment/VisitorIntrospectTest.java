package org.ehrbase.aql.containment;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.jooq.FolderAccessHistoryMockDataProvider;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.dao.jooq.impl.DSLContextHelper;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.*;
import org.ehrbase.terminology.openehr.TerminologyService;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.io.FileInputStream;
import java.util.Optional;

import static org.junit.Assert.*;

public class VisitorIntrospectTest {

    protected I_DomainAccess testDomainAccess;
    protected I_KnowledgeCache knowledge;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public CacheRule cacheRule = new CacheRule();

    @Before
    public  void beforeClass() throws Exception {
        DSLContext context = DSLContextHelper.buildContext();
        I_KnowledgeCache knowledgeCache = KnowledgeCacheHelper.buildKnowledgeCache(testFolder, cacheRule);
        try {
            testDomainAccess = new DummyDataAccess(context, knowledgeCache, null, KnowledgeCacheHelper.buildServerConfig());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //add template to knowledgeCache
        byte[] opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/Patientenaufenthalt.opt").readAllBytes();
        knowledgeCache.addOperationalTemplate(opt);
        opt = new FileInputStream("./src/test/resources/knowledge/operational_templates/Stationärer Versorgungsfall.opt").readAllBytes();
        knowledgeCache.addOperationalTemplate(opt);

        //tests require a terminology service
        new TerminologyServiceImp().init(); //this sets the instance variable

    }

    @Test
    public void testRepresent()  {
        VisitorIntrospect visitorIntrospect = new VisitorIntrospect(testDomainAccess.getKnowledgeManager());

        String json = visitorIntrospect.representAsString("Stationärer Versorgungsfall");

        assertNotNull(json);

        assertTrue(visitorIntrospect.jsonPathEval(json, "[*]"));

    }


}