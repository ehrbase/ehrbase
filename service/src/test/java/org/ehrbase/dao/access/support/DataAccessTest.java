/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.junit.Assert.assertTrue;

/**
 * Created by christian on 11/2/2016.
 */
@Ignore
public class DataAccessTest {

    protected I_DomainAccess testDomainAccess;
    protected DSLContext context;
    protected I_KnowledgeCache knowledge;

    @Test
    @Ignore
    public void setupDBCP2Access() throws Exception {
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
        properties.put(I_DomainAccess.KEY_CONNECTION_MODE, I_DomainAccess.DBCP2_POOL);
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

        //try a query with this pool
        Result result = context.select(EHR_.ID).from(EHR_).fetch();

        // a new database does not contain any data so empty result is perfectly fine
        assertTrue(result.isNotEmpty());
    }

}