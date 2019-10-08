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

package org.ehrbase.dao.access.support;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.KnowledgeCacheService;
import junit.framework.TestCase;
import org.jooq.DSLContext;
import org.junit.Ignore;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created by Christian Chevalley on 4/25/2015.
 */
@Ignore
public abstract class AccessTestCase extends TestCase {

    protected I_DomainAccess testDomainAccess;
    protected DSLContext context;
    protected I_KnowledgeCache knowledge;
//    protected I_IntrospectCache introspectCache;

    protected void setupDomainAccess() throws Exception {
        Properties props = new Properties();
        props.put("knowledge.path.archetype", "src/test/resources/knowledge/archetypes");
        props.put("knowledge.path.template", "src/test/resources/knowledge/templates");
        props.put("knowledge.path.opt", "src/test/resources/knowledge/operational_templates");
        props.put("knowledge.cachelocatable", "true");
        props.put("knowledge.forcecache", "true");

        knowledge = new KnowledgeCacheService(props);

        Pattern include = Pattern.compile(".*");

        knowledge.retrieveFileMap(include, null);

        Map<String, Object> properties = new HashMap<>();
        properties.put(I_DomainAccess.KEY_DIALECT, "POSTGRES");
        properties.put(I_DomainAccess.KEY_URL, "jdbc:postgresql://" + System.getProperty("test.db.host") + ":" + System.getProperty("test.db.port") + "/" + System.getProperty("test.db.name"));
        properties.put(I_DomainAccess.KEY_LOGIN, System.getProperty("test.db.user"));
        properties.put(I_DomainAccess.KEY_PASSWORD, System.getProperty("test.db.password"));

        properties.put(I_DomainAccess.KEY_KNOWLEDGE, knowledge);

        try {
            testDomainAccess = new DummyDataAccess(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }

        context = testDomainAccess.getContext();
        //     introspectCache = testDomainAccess.getIntrospectService().load().synchronize();
    }

}
