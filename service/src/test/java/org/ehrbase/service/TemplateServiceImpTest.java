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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.cache.CacheProperties;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.cache.CacheProviderImp;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.repository.TemplateStoreRepository;
import org.ehrbase.test.fixtures.TemplateFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

class TemplateServiceImpTest {

    private final TemplateStoreRepository mockTemplateStoreRepository = mock();
    private SimpleCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        Mockito.reset(mockTemplateStoreRepository);

        cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(List.of(
                new ConcurrentMapCache(CacheProvider.TEMPLATE_ID_UUID_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_UUID_ID_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_OPT_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_CACHE.name()),
                new ConcurrentMapCache(CacheProvider.TEMPLATE_LIST_CACHE.name())));
        cacheManager.initializeCaches();
    }

    private TemplateServiceImp service() {

        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setTemplateInitOnStartup("false");
        return new TemplateServiceImp(
                mockTemplateStoreRepository, new CacheProviderImp(cacheManager), cacheProperties, false);
    }

    @Test
    void addOperationalTemplate() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);
        String templateId = service().addOperationalTemplate(testTemplate.metaData(), false, false, false);

        verify(mockTemplateStoreRepository, times(1)).store(testTemplate.metaData());
        assertThat(templateId).isNotNull().isEqualTo(testTemplate.templateId());
    }

    @Test
    void addOperationalTemplateCanNotBeOverwritten() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findUuidByTemplateId(testTemplate.templateId()))
                .thenReturn(Optional.of(UUID.randomUUID()));

        TemplateServiceImp service = service();
        TemplateServiceImp.TemplateWithDetails templateData = testTemplate.metaData();
        assertThatThrownBy(() -> service.addOperationalTemplate(templateData, false, false, false))
                .isInstanceOf(StateConflictException.class)
                .hasMessage("Operational template with this template ID already exists: %s"
                        .formatted(testTemplate.templateId()));

        verify(mockTemplateStoreRepository, times(1)).findUuidByTemplateId(anyString());
    }

    @Test
    void addOperationalTemplateAllowOverwrite() {
        TemplateFixture.TestTemplate testTemplate = parseAndMock(OperationalTemplateTestData.MINIMAL_ACTION);

        Mockito.when(mockTemplateStoreRepository.findByTemplateIds(testTemplate.templateId()))
                .thenReturn(List.of(testTemplate.metaData()));

        TemplateServiceImp service = spy(service());
        String templateId = service.addOperationalTemplate(testTemplate.metaData(), true, true, false);

        assertThat(templateId).isNotNull().isEqualTo(testTemplate.templateId());
    }

    private TemplateFixture.TestTemplate parseAndMock(OperationalTemplateTestData operationalTemplateTestData) {
        TemplateFixture.TestTemplate testTemplate = TemplateFixture.fixtureTemplate(operationalTemplateTestData);
        TemplateServiceImp.TemplateWithDetails data = testTemplate.metaData();
        Mockito.when(mockTemplateStoreRepository.store(data)).thenReturn(data);
        Mockito.when(mockTemplateStoreRepository.update(data)).thenReturn(data);
        return testTemplate;
    }
}
