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

import java.util.concurrent.Callable;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class CacheProviderImp implements CacheProvider {

    private final CacheManager cacheManager;

    public CacheProviderImp(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public <V, K> V get(EhrBaseCache<K, V> cache, K key, Callable<V> valueLoader) {
        return cacheManager.getCache(cache.name()).get(key, valueLoader);
    }

    @Override
    public <V, K> void evict(EhrBaseCache<K, V> cache, K key) {
        cacheManager.getCache(cache.name()).evict(key);
    }
}
