/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.application.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.ehrbase.cache.CacheOptions;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Configuration} for EhCache using JCache.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
public class CacheConfiguration {

    @Bean
    public CacheOptions cacheOptions(CacheProperties properties) {
        var options = new CacheOptions();
        options.setPreBuildQueries(properties.isPreBuildQueries());
        options.setPreBuildQueriesDepth(properties.getPreBuildQueriesDepth());
        options.setPreInitialize(properties.isInitOnStartup());
        return options;
    }

    @Bean
    @ConditionalOnExpression(
            "T(org.springframework.boot.autoconfigure.cache.CacheType).CAFFEINE.name().equalsIgnoreCase(\"${spring.cache.type}\")")
    public CacheManagerCustomizer<CaffeineCacheManager> cacheManagerCustomizer() {
        return cm -> {
            cm.registerCustomCache(
                    CacheOptions.INTROSPECT_CACHE, Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheOptions.QUERY_CACHE, Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheOptions.FIELDS_CACHE, Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheOptions.MULTI_VALUE_CACHE, Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheOptions.CONCEPT_CACHE_ID, Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheOptions.CONCEPT_CACHE_CONCEPT_ID, Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheOptions.CONCEPT_CACHE_DESCRIPTION,
                    Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheOptions.TERRITORY_CACHE, Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheOptions.LANGUAGE_CACHE, Caffeine.newBuilder().build());
            cm.registerCustomCache(
                    CacheOptions.USER_ID_CACHE,
                    Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build());
            cm.registerCustomCache(
                    CacheOptions.SYS_TENANT, Caffeine.newBuilder().build());
        };
    }
}
