/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.service;

import org.apache.commons.io.IOUtils;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.opt.query.TemplateTestData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by christian on 5/10/2018.
 */

public class KnowledgeCacheServiceTest {


    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public CacheRule cacheRule = new CacheRule();

    @Test
    public void testListAllOperationalTemplates() throws Exception {
        KnowledgeCacheService cut = buildKnowledgeCache(testFolder,cacheRule);
        cut.addOperationalTemplate(IOUtils.toByteArray(TemplateTestData.IMMUNISATION_SUMMARY.getStream()));
        List<TemplateMetaData> templateMetaData = cut.listAllOperationalTemplates();
        assertThat(templateMetaData).size().isEqualTo(1);
    }

    @Test
    public void testRetrieveVisitorByTemplateId() throws Exception {
        KnowledgeCacheService knowledge = buildKnowledgeCache(testFolder, cacheRule);
        knowledge.addOperationalTemplate(IOUtils.toByteArray(TemplateTestData.IMMUNISATION_SUMMARY.getStream()));


        assertThat(knowledge.getQueryOptMetaData("IDCR - Immunisation summary.v0")).isNotNull();
    }


    public static KnowledgeCacheService buildKnowledgeCache(TemporaryFolder folder, CacheRule cacheRule) throws Exception {


        File operationalTemplatesemplates = folder.newFolder("operational_templates");

        TemplateFileStorageService templateFileStorageService = new TemplateFileStorageService();
        templateFileStorageService.setOptPath(operationalTemplatesemplates.getPath());

        return new KnowledgeCacheService(templateFileStorageService, cacheRule.cacheManager);
    }


}