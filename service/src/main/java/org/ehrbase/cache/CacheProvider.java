/*
 * Copyright (c) 2024 vitasystems GmbH.
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
package org.ehrbase.cache;

import com.jayway.jsonpath.DocumentContext;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.springframework.cache.Cache;

public interface CacheProvider {
    EhrBaseCache<String, WebTemplate> INTROSPECT_CACHE = new EhrBaseCache<>("introspectCache");
    EhrBaseCache<String, UUID> TEMPLATE_ID_UUID_CACHE = new EhrBaseCache<>("TemplateIdUuidCache");
    EhrBaseCache<UUID, String> TEMPLATE_UUID_ID_CACHE = new EhrBaseCache<>("TemplateUuidIdCache");
    EhrBaseCache<String, UUID> USER_ID_CACHE = new EhrBaseCache<>("userIdCache");
    EhrBaseCache<String, DocumentContext> EXTERNAL_FHIR_TERMINOLOGY_CACHE =
            new EhrBaseCache<>("externalFhirTerminologyCache");
    EhrBaseCache<String, QueryDefinitionResultDto> STORED_QUERY_CACHE = new EhrBaseCache<>("StoredQueryCache");

    static Supplier<InternalServerException> getExceptionSupplier(EhrBaseCache<?, ?> cache) {
        return () -> new InternalServerException("Non existing cache : %s".formatted(cache.name()));
    }

    record EhrBaseCache<K, V>(String name) {
        public V get(CacheProvider cacheProvider, K key, Callable<V> valueLoader) {
            return cacheProvider.getCache(this).get(key, valueLoader);
        }

        public void evict(CacheProvider cacheProvider, K key) {
            cacheProvider.getCache(this).evict(key);
        }

        public void put(CacheProvider cacheProvider, K key, V value) {
            cacheProvider.getCache(this).put(key, value);
        }

        public void clear(CacheProvider cacheProvider) {
            cacheProvider.getCache(this).clear();
        }
    }

    Cache getCache(EhrBaseCache<?, ?> cache);
}
