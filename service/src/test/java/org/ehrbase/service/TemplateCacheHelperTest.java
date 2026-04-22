/*
 * Copyright (c) 2026 vitasystems GmbH.
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
import static org.assertj.core.api.Fail.fail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.cache.CacheProviderImp;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

class TemplateCacheHelperTest {

    private SimpleCacheManager cacheManager;
    private CacheProvider cacheProvider;
    private TemplateCacheHelper helper;

    // Convenience handles to the underlying Spring caches for direct assertions
    private ConcurrentMapCache templateCache;
    private ConcurrentMapCache templateOptCache;
    private ConcurrentMapCache templateIdUuidCache;
    private ConcurrentMapCache templateUuidIdCache;
    private ConcurrentMapCache templateListCache;

    @BeforeEach
    void setUp() {
        templateCache = new ConcurrentMapCache(CacheProvider.TEMPLATE_CACHE.name());
        templateOptCache = new ConcurrentMapCache(CacheProvider.TEMPLATE_OPT_CACHE.name());
        templateIdUuidCache = new ConcurrentMapCache(CacheProvider.TEMPLATE_ID_UUID_CACHE.name());
        templateUuidIdCache = new ConcurrentMapCache(CacheProvider.TEMPLATE_UUID_ID_CACHE.name());
        templateListCache = new ConcurrentMapCache(CacheProvider.TEMPLATE_LIST_CACHE.name());

        cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(
                List.of(templateCache, templateOptCache, templateIdUuidCache, templateUuidIdCache, templateListCache));
        cacheManager.initializeCaches();

        cacheProvider = new CacheProviderImp(cacheManager);
        helper = new TemplateCacheHelper(cacheProvider);
    }

    @Test
    void addToCache_populatesAllThreeCaches() {
        UUID id = UUID.randomUUID();
        String templateId = "test-template";
        WebTemplate webTemplate = new WebTemplate();

        helper.addToCache(id, templateId, webTemplate, false);

        // TEMPLATE_CACHE: templateId -> WebTemplate (identity check)
        assertThat(templateCache.get(templateId))
                .extracting(Cache.ValueWrapper::get)
                .isSameAs(webTemplate);

        // TEMPLATE_UUID_ID_CACHE: UUID -> templateId
        assertThat(templateUuidIdCache.get(id))
                .extracting(Cache.ValueWrapper::get)
                .isEqualTo(templateId);

        // TEMPLATE_ID_UUID_CACHE: templateId -> UUID
        assertThat(templateIdUuidCache.get(templateId))
                .extracting(Cache.ValueWrapper::get)
                .isEqualTo(id);

        // OPT cache must remain untouched
        assertThat(templateOptCache.get(templateId)).isNull();
    }

    @Test
    void addToCache_inbound_clearsTemplateListCache() {
        templateListCache.put(Boolean.TRUE, List.of());
        helper.addToCache(UUID.randomUUID(), "tpl", new WebTemplate(), true);
        assertThat(templateListCache.get(Boolean.TRUE)).isNull();
    }

    @Test
    void addToCache_notInbound_doesNotClearTemplateListCache() {
        templateListCache.put(Boolean.TRUE, List.of());
        helper.addToCache(UUID.randomUUID(), "tpl", new WebTemplate(), false);
        assertThat(templateListCache.get(Boolean.TRUE)).isNotNull();
    }

    @Test
    void invalidateCaches_evictsOnlyRelatedEntries() {
        UUID id = UUID.randomUUID();
        String templateId = "evict-me";
        UUID otherId = UUID.randomUUID();
        String otherTemplateId = "keep-me";
        WebTemplate otherWebTemplate = new WebTemplate();

        templateCache.put(templateId, new WebTemplate());
        templateOptCache.put(templateId, "<template/>");
        templateIdUuidCache.put(templateId, id);
        templateUuidIdCache.put(id, templateId);
        templateListCache.put(Boolean.TRUE, List.of());

        templateCache.put(otherTemplateId, otherWebTemplate);
        templateIdUuidCache.put(otherTemplateId, otherId);
        templateUuidIdCache.put(otherId, otherTemplateId);

        helper.invalidateCaches(templateId, id);

        // targeted entries must be gone
        assertThat(templateCache.get(templateId)).isNull();
        assertThat(templateOptCache.get(templateId)).isNull();
        assertThat(templateIdUuidCache.get(templateId)).isNull();
        assertThat(templateUuidIdCache.get(id)).isNull();
        assertThat(templateListCache.get(Boolean.TRUE)).isNull();

        // unrelated entries must survive
        assertThat(templateCache.get(otherTemplateId))
                .extracting(Cache.ValueWrapper::get)
                .isSameAs(otherWebTemplate);
        assertThat(templateIdUuidCache.get(otherTemplateId)).isNotNull();
        assertThat(templateUuidIdCache.get(otherId)).isNotNull();
    }

    @Test
    void clearCaches_removesAllEntriesFromAllCaches() {
        UUID id = UUID.randomUUID();
        String templateId = "clear-me";

        templateCache.put(templateId, new WebTemplate());
        templateOptCache.put(templateId, "<template/>");
        templateIdUuidCache.put(templateId, id);
        templateUuidIdCache.put(id, templateId);
        templateListCache.put(Boolean.TRUE, List.of());

        helper.clearCaches();

        assertThat(templateCache.get(templateId)).isNull();
        assertThat(templateOptCache.get(templateId)).isNull();
        assertThat(templateIdUuidCache.get(templateId)).isNull();
        assertThat(templateUuidIdCache.get(id)).isNull();
        assertThat(templateListCache.get(Boolean.TRUE)).isNull();
    }

    @Test
    void findTemplateIdByUuid_callsLoaderAndPopulatesReverseCacheOnMiss() {
        UUID id = UUID.randomUUID();
        String templateId = "loaded-tid";

        String result = helper.findTemplateIdByUuid(id, u -> Optional.of(templateId));

        assertThat(result).isEqualTo(templateId);

        // forward cache populated (UUID -> templateId)
        assertThat(templateUuidIdCache.get(id))
                .extracting(Cache.ValueWrapper::get)
                .isEqualTo(templateId);

        // reverse cache populated (templateId -> UUID)
        assertThat(templateIdUuidCache.get(templateId))
                .extracting(Cache.ValueWrapper::get)
                .isEqualTo(id);
    }

    @Test
    void findUuidByTemplateId_callsLoaderAndPopulatesReverseCacheOnMiss() {
        String templateId = "loaded-tpl";
        UUID id = UUID.randomUUID();

        UUID result = helper.findUuidByTemplateId(templateId, tid -> Optional.of(id));

        assertThat(result).isEqualTo(id);

        // forward cache populated (UUID -> templateId)
        assertThat(templateUuidIdCache.get(id))
                .extracting(Cache.ValueWrapper::get)
                .isEqualTo(templateId);

        // reverse cache populated (templateId -> UUID)
        assertThat(templateIdUuidCache.get(templateId))
                .extracting(Cache.ValueWrapper::get)
                .isEqualTo(id);
    }

    @Test
    void addToCache_thenGetInternalTemplate_returnsAddedTemplate_withoutCallingLoader() {
        UUID id = UUID.randomUUID();
        String templateId = "round-trip";
        WebTemplate webTemplate = new WebTemplate();

        helper.addToCache(id, templateId, webTemplate, false);

        assertThat(helper.getInternalTemplate(templateId, tid -> {
                    fail("Loader called");
                    return null;
                }))
                .isSameAs(webTemplate);

        assertThat(helper.findTemplateIdByUuid(id, u -> {
                    fail("Loader called");
                    return null;
                }))
                .isEqualTo(templateId);

        assertThat(helper.findUuidByTemplateId(templateId, tid -> {
                    fail("Loader called");
                    return null;
                }))
                .isEqualTo(id);
    }

    // -------------------------------------------------------------------------
    // handleCacheMismatch: RuntimeException propagation
    // -------------------------------------------------------------------------

    @Test
    void findTemplateIdByUuid_propagatesRuntimeException_fromLoader() {
        RuntimeException cause = new IllegalStateException("db error");
        assertThatThrownBy(() -> helper.findTemplateIdByUuid(UUID.randomUUID(), u -> {
                    throw cause;
                }))
                .isSameAs(cause);
    }

    @Test
    void findUuidByTemplateId_propagatesRuntimeException_fromLoader() {
        RuntimeException cause = new IllegalStateException("db error");
        assertThatThrownBy(() -> helper.findUuidByTemplateId("error-tpl", tid -> {
                    throw cause;
                }))
                .isSameAs(cause);
    }
}
