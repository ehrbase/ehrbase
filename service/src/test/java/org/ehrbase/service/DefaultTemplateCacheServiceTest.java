/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.knowledge.TemplateCacheService;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.cache.CacheProviderImp;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.repository.TemplateStoreRepository;
import org.ehrbase.test.fixtures.TemplateFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

class DefaultTemplateCacheServiceTest {

    private final TemplateStoreRepository mockTemplateStoreRepository = mock();
    private final TemplateDBStorageService mockTemplateStore = mock();
    private SimpleCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        Mockito.reset(mockTemplateStoreRepository);
        Mockito.reset(mockTemplateStore);

        cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new ConcurrentMapCache(CacheProvider.TEMPLATE_ID_UUID_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_UUID_ID_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_OPT_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_CACHE.name()) // For WebTemplate representation
                ));
        cacheManager.initializeCaches();
    }

    private TemplateCacheService service() {

        return new DefaultTemplateCacheService(
                mockTemplateStoreRepository, mockTemplateStore, new CacheProviderImp(cacheManager));
    }

    @Test
    void addOperationalTemplate() {

        TemplateFixture.TestTemplate testTemplate = parse(OperationalTemplateTestData.MINIMAL_ACTION);
        String templateId = service().addOperationalTemplate(testTemplate.operationaltemplate());

        assertThat(templateId).isNotNull().isEqualTo(testTemplate.templateId());
    }

    @Test
    void addOperationalTemplateCanNotBeOverwritten() {

        TemplateFixture.TestTemplate testTemplate = parse(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.of(UUID.randomUUID()));

        TemplateCacheService service = service();
        OPERATIONALTEMPLATE operationaltemplate = testTemplate.operationaltemplate();
        assertThatThrownBy(() -> service.addOperationalTemplate(operationaltemplate))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Operational template with this template ID already exists: %s"
                        .formatted(testTemplate.templateId()));
    }

    @Test
    void addOperationalTemplateAllowOverwrite() {
        TemplateFixture.TestTemplate testTemplate = parse(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));
        Mockito.when(mockTemplateStore.allowTemplateOverwrite()).thenReturn(true);

        TemplateCacheService service = spy(service());
        String templateId = service.addOperationalTemplate(testTemplate.operationaltemplate());

        assertThat(templateId).isNotNull().isEqualTo(testTemplate.templateId());
    }

    private TemplateFixture.TestTemplate parse(OperationalTemplateTestData operationalTemplateTestData) {

        TemplateFixture.TestTemplate testTemplate = TemplateFixture.fixtureTemplate(operationalTemplateTestData);

        doReturn(testTemplate.metaData()).when(mockTemplateStore).storeTemplate(testTemplate.operationaltemplate());
        return testTemplate;
    }
}
