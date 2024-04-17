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

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * {@link Configuration} for EhCache using JCache.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
public class CacheConfiguration {

    @Bean
    @ConditionalOnExpression(
            "T(org.springframework.boot.autoconfigure.cache.CacheType).CAFFEINE.name().equalsIgnoreCase(\"${spring.cache.type}\")")
    public CacheManagerCustomizer<CaffeineCacheManager> cacheManagerCustomizer(CacheProperties cacheProperties) {
        return cm -> configureCaffeineCacheManager(cm, cacheProperties, CacheProvider.EhrBaseCache::name);
    }

    protected void configureCaffeineCacheManager(
            CaffeineCacheManager cacheManager,
            CacheProperties cacheProperties,
            Function<CacheProvider.EhrBaseCache<?, ?>, String> createCacheName) {
        cacheManager.registerCustomCache(
                createCacheName.apply(CacheProvider.INTROSPECT_CACHE),
                Caffeine.newBuilder().build());
        cacheManager.registerCustomCache(
                createCacheName.apply(CacheProvider.TEMPLATE_UUID_ID_CACHE),
                Caffeine.newBuilder().build());
        cacheManager.registerCustomCache(
                createCacheName.apply(CacheProvider.TEMPLATE_ID_UUID_CACHE),
                Caffeine.newBuilder().build());
        cacheManager.registerCustomCache(
                createCacheName.apply(CacheProvider.USER_ID_CACHE),
                configureCache(Caffeine.newBuilder(), cacheProperties.getUserIdCacheConfig())
                        .build());
        cacheManager.registerCustomCache(
                createCacheName.apply(CacheProvider.EXTERNAL_FHIR_TERMINOLOGY_CACHE),
                configureCache(Caffeine.newBuilder(), cacheProperties.getExternalFhirTerminologyCacheConfig())
                        .build());
        cacheManager.registerCustomCache(
                createCacheName.apply(CacheProvider.STORED_QUERY_CACHE),
                Caffeine.newBuilder().build());
    }

    protected static Caffeine<Object, Object> configureCache(
            Caffeine<Object, Object> caffeine, CacheProperties.CacheConfig cacheConfig) {

        if (cacheConfig.getExpireAfterWrite() != null) {
            caffeine.expireAfterWrite(
                    cacheConfig.getExpireAfterWrite().getDuration(),
                    cacheConfig.getExpireAfterWrite().getUnit());
        }

        if (cacheConfig.getExpireAfterAccess() != null) {
            caffeine.expireAfterAccess(
                    cacheConfig.getExpireAfterAccess().getDuration(),
                    cacheConfig.getExpireAfterAccess().getUnit());
        }

        return caffeine;
    }

    @Bean
    public BeanPostProcessor cacheManagerTxProxyBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(final Object bean, final String beanName) {
                if (bean instanceof CacheManager cm) {
                    //TODO CDR-1259 breaks template delete, because the template caches are not managed properly
                    return new CustomTxAwareCacheManagerProxy(cm);
                }
                return bean;
            }
        };
    }

    public static class CustomTxAwareCacheManagerProxy implements CacheManager {
        private CacheManager targetCacheManager;

        /**
         * Create a new TransactionAwareCacheManagerProxy for the given target CacheManager.
         * @param targetCacheManager the target CacheManager to proxy
         */
        public CustomTxAwareCacheManagerProxy(CacheManager targetCacheManager) {

            if (targetCacheManager == null) {
                throw new IllegalArgumentException("Property 'targetCacheManager' is required");
            }
            this.targetCacheManager = targetCacheManager;
        }


        @Override
        @Nullable
        public Cache getCache(String name) {
            Cache targetCache = this.targetCacheManager.getCache(name);
            return (targetCache != null ? new CustomTxAwareCacheDecorator(targetCache) : null);
        }

        @Override
        public Collection<String> getCacheNames() {
            return this.targetCacheManager.getCacheNames();
        }
    }

    private static class CustomTxAwareCacheDecorator extends TransactionAwareCacheDecorator {

        public CustomTxAwareCacheDecorator(final Cache targetCache) {
            super(targetCache);
        }

        @Override
        public <T> T get(final Object key, final Callable<T> valueLoader) {
            //TODO CDR-1259: make value available from calls to this method during the transaction? What if this is called multiple times with the same key?
            return (T) Optional.of(key).map(super::get).map(ValueWrapper::get).orElseGet(()-> {
                T val;
                try {
                    val = valueLoader.call();
                } catch (Exception e) {
                    throw new ValueRetrievalException(key,valueLoader,e);
                }
                super.put(key, val);
                return val;
            });
        }

        @Override
        public <T> CompletableFuture<T> retrieve(final Object key, final Supplier<CompletableFuture<T>> valueLoader) {
            throw new UnsupportedOperationException("Cache::retrieve operation is not supported during a transaction");
        }

        @Override
        public ValueWrapper putIfAbsent(final Object key, final Object value) {
            ValueWrapper valInCache = get(key);
            if(valInCache == null){
                put(key, value);
            }
            return valInCache;
        }
    }
}
