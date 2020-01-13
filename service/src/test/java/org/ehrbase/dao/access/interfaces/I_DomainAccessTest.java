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

package org.ehrbase.dao.access.interfaces;

import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.CacheRule;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.jooq.DSLContext;
import org.junit.rules.TemporaryFolder;

public class I_DomainAccessTest {

    public static I_DomainAccess buildDomainAccess(DSLContext context, TemporaryFolder temporaryFolder, CacheRule cacheRule) throws Exception {
        IntrospectService introspectCache = KnowledgeCacheHelper.buildKnowledgeCache(temporaryFolder, cacheRule);
        I_KnowledgeCache knowledge = introspectCache.getKnowledge();
        return new DataAccess(context, knowledge, introspectCache, KnowledgeCacheHelper.buildServerConfig()) {
            @Override
            public DataAccess getDataAccess() {
                return this;
            }
        };

    }
}