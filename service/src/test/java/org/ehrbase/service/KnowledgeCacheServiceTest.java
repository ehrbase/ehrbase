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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.ehr.knowledge.TemplateTestData;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.openehr.sdk.webtemplate.parser.NodeId;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

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
        KnowledgeCacheService cut = buildKnowledgeCache(testFolder, cacheRule);
        cut.addOperationalTemplate(TemplateTestData.IMMUNISATION_SUMMARY.getStream());
        List<TemplateMetaData> templateMetaData = cut.listAllOperationalTemplates();
        assertThat(templateMetaData).size().isEqualTo(1);
    }

    @Test
    public void testRetrieveVisitorByTemplateId() throws Exception {
        KnowledgeCacheService knowledge = buildKnowledgeCache(testFolder, cacheRule);
        knowledge.addOperationalTemplate(TemplateTestData.IMMUNISATION_SUMMARY.getStream());

        assertThat(knowledge.getQueryOptMetaData("IDCR - Immunisation summary.v0"))
                .isNotNull();
    }

    @Test
    public void testNonUniqueAqlPathsTemplateId() throws Exception {
        KnowledgeCacheService knowledge = buildKnowledgeCache(testFolder, cacheRule);
        knowledge.addOperationalTemplate(TemplateTestData.NON_UNIQUE_AQL_PATH.getStream());
        // a node with two paths
        NodeId nodeId = new NodeId("ACTION", "openEHR-EHR-ACTION.procedure.v1");
        List<NodeId> nodeIds = new ArrayList<>();
        nodeIds.add(nodeId);

        // resolve
        JsonPathQueryResult jsonPathQueryResult = knowledge.resolveForTemplate("non_unique_aql_paths", nodeIds);
        assertThat(jsonPathQueryResult).isNotNull();
    }

    @Test
    public void testQueryType() throws Exception {
        KnowledgeCacheService knowledge = buildKnowledgeCache(testFolder, cacheRule);
        knowledge.addOperationalTemplate(OperationalTemplateTestData.IDCR_PROBLEM_LIST.getStream());

        assertThat(knowledge
                        .getInfo(
                                OperationalTemplateTestData.IDCR_PROBLEM_LIST.getTemplateId(),
                                "/content[openEHR-EHR-SECTION.problems_issues_rcp.v1]/items[openEHR-EHR-EVALUATION.problem_diagnosis.v1]/data[at0001]/items[at0012]")
                        .getItemType())
                .isEqualTo("DV_TEXT");
    }

    @Test
    public void testQueryType2() throws Exception {
        KnowledgeCacheService knowledge = buildKnowledgeCache(testFolder, cacheRule);
        knowledge.addOperationalTemplate(OperationalTemplateTestData.BLOOD_PRESSURE_SIMPLE.getStream());

        assertThat(knowledge
                        .getInfo(
                                OperationalTemplateTestData.BLOOD_PRESSURE_SIMPLE.getTemplateId(),
                                "/content[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0004]")
                        .getItemType())
                .isEqualTo("DV_QUANTITY");
    }

    @Test
    public void unsupportedTemplate() throws Exception {
        var knowledgeCacheService = buildKnowledgeCache(testFolder, cacheRule);
        var content = TemplateTestData.CLINICAL_CONTENT_VALIDATION.getStream();

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> knowledgeCacheService.addOperationalTemplate(content));
    }

    public static KnowledgeCacheService buildKnowledgeCache(TemporaryFolder folder, CacheRule cacheRule)
            throws Exception {

        File operationalTemplatesemplates = folder.newFolder("operational_templates");

        TemplateFileStorageService templateFileStorageService = new TemplateFileStorageService();
        templateFileStorageService.setOptPath(operationalTemplatesemplates.getPath());

        TenantService tenantService = Mockito.mock(TenantService.class);
        Mockito.when(tenantService.getCurrentSysTenant()).thenReturn(TenantAuthentication.DEFAULT_SYS_TENANT);

        return new KnowledgeCacheService(
                templateFileStorageService, new ConcurrentMapCacheManager(), new CacheOptions(), tenantService);
    }
}
