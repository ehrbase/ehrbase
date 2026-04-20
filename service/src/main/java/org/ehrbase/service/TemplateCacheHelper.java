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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.springframework.cache.Cache;

final class TemplateCacheHelper {
    private final CacheProvider cacheProvider;

    public TemplateCacheHelper(CacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    public void addToCache(
            UUID internalId, String templateId, WebTemplate tpl, OffsetDateTime createdOn, boolean inbound) {
        CacheProvider.TEMPLATE_CACHE.put(cacheProvider, templateId, Pair.of(tpl, createdOn));
        CacheProvider.TEMPLATE_UUID_ID_CACHE.put(cacheProvider, internalId, templateId);
        CacheProvider.TEMPLATE_ID_UUID_CACHE.put(cacheProvider, templateId, internalId);
        if (inbound) {
            CacheProvider.TEMPLATE_LIST_CACHE.clear(cacheProvider);
        }
    }

    public void invalidateCaches(String templateId, UUID internalId) {
        CacheProvider.TEMPLATE_CACHE.evict(cacheProvider, templateId);
        CacheProvider.TEMPLATE_OPT_CACHE.evict(cacheProvider, templateId);
        CacheProvider.TEMPLATE_ID_UUID_CACHE.evict(cacheProvider, templateId);
        CacheProvider.TEMPLATE_UUID_ID_CACHE.evict(cacheProvider, internalId);
        CacheProvider.TEMPLATE_LIST_CACHE.clear(cacheProvider);
    }

    public void clearCaches() {
        Stream.of(
                        CacheProvider.TEMPLATE_CACHE,
                        CacheProvider.TEMPLATE_OPT_CACHE,
                        CacheProvider.TEMPLATE_ID_UUID_CACHE,
                        CacheProvider.TEMPLATE_UUID_ID_CACHE,
                        CacheProvider.TEMPLATE_LIST_CACHE)
                .forEach(c -> c.clear(cacheProvider));
    }

    public String retrieveOperationalTemplate(String templateId, Function<String, String> loader) {
        return find(CacheProvider.TEMPLATE_OPT_CACHE, templateId, loader);
    }

    private static <T> T handleCacheMismatch(Cache.ValueRetrievalException ex) {
        Throwable cause = ex.getCause();
        return switch (cause) {
            // No template with that UUID exists
            case NoSuchElementException _ -> null;
            case RuntimeException re -> throw re;
            default -> throw ex;
        };
    }

    public String findTemplateIdByUuid(UUID uuid, Function<UUID, Optional<String>> loader) {
        try {
            return find(CacheProvider.TEMPLATE_UUID_ID_CACHE, uuid, u -> {
                String tid = loader.apply(u).orElseThrow();
                // reverse cache
                CacheProvider.TEMPLATE_ID_UUID_CACHE.put(cacheProvider, tid, u);
                return tid;
            });
        } catch (Cache.ValueRetrievalException ex) {
            return handleCacheMismatch(ex);
        }
    }

    public UUID findUuidByTemplateId(String templateId, Function<String, Optional<UUID>> loader) {
        try {
            return find(CacheProvider.TEMPLATE_ID_UUID_CACHE, templateId, tid -> {
                UUID u = loader.apply(tid).orElseThrow();
                // reverse cache
                CacheProvider.TEMPLATE_UUID_ID_CACHE.put(cacheProvider, u, tid);
                return u;
            });
        } catch (Cache.ValueRetrievalException ex) {
            return handleCacheMismatch(ex);
        }
    }

    public Pair<WebTemplate, OffsetDateTime> getInternalTemplate(
            String templateId, Function<String, Pair<WebTemplate, OffsetDateTime>> loader) {
        return find(CacheProvider.TEMPLATE_CACHE, templateId, loader);
    }

    private <K, V> V find(CacheProvider.EhrBaseCache<K, V> cache, K key, Function<K, V> loader) {
        return cache.get(cacheProvider, key, () -> loader.apply(key));
    }

    public List<TemplateService.TemplateDetails> findAllTemplates(
            Supplier<List<TemplateService.TemplateDetails>> loader) {
        return find(CacheProvider.TEMPLATE_LIST_CACHE, Boolean.TRUE, _ -> loader.get());
    }
}
