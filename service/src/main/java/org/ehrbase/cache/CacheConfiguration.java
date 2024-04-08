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
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return cm -> {
            cm.registerCustomCache(
                    CacheProvider.INTROSPECT_CACHE.name(), Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheProvider.TEMPLATE_UUID_ID_CACHE.name(),
                    Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheProvider.TEMPLATE_ID_UUID_CACHE.name(),
                    Caffeine.newBuilder().build());

            cm.registerCustomCache(
                    CacheProvider.USER_ID_CACHE.name(),
                    configureCache(Caffeine.newBuilder(), cacheProperties.getUserIdCacheConfig())
                            .build());
            cm.registerCustomCache(
                    CacheProvider.EXTERNAL_FHIR_TERMINOLOGY_CACHE.name(),
                    configureCache(Caffeine.newBuilder(), cacheProperties.getExternalFhirTerminologyCacheConfig())
                            .build());
        };
    }

    private static Caffeine<Object, Object> configureCache(
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
}
