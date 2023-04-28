/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import java.io.File;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.opt.query.TemplateTestData;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * Created by christian on 5/10/2018.
 */
public class KnowledgeCacheHelper {

    public static KnowledgeCacheService buildKnowledgeCache(TemporaryFolder folder, CacheRule cacheRule)
            throws Exception {

        File operationalTemplates = folder.newFolder("operational_templates");

        TemplateFileStorageService templateFileStorageService = new TemplateFileStorageService();
        templateFileStorageService.setOptPath(operationalTemplates.getPath());

        TenantService tenantService = Mockito.mock(TenantService.class);
        Mockito.when(tenantService.getCurrentSysTenant()).thenReturn(TenantAuthentication.DEFAULT_SYS_TENANT);

        KnowledgeCacheService knowledgeCacheService = new KnowledgeCacheService(
                templateFileStorageService, cacheRule.cacheManager, new CacheOptions(), tenantService);
        knowledgeCacheService.addOperationalTemplate(TemplateTestData.IMMUNISATION_SUMMARY.getStream());
        return knowledgeCacheService;
    }

    public static ServerConfig buildServerConfig() {
        return new ServerConfig() {

            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public void setPort(int port) {}

            @Override
            public String getNodename() {
                return "local.ehrbase.org";
            }

            @Override
            public void setNodename(String nodename) {}

            @Override
            public String getAqlIterationSkipList() {
                return "/events,/activities,/content";
            }

            @Override
            public Integer getAqlDepth() {
                return 1;
            }

            @Override
            public boolean isDisableStrictValidation() {
                return false;
            }
        };
    }
}
