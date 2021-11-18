/*
 * Copyright 2021 vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.application.config.cache;

import io.micrometer.core.lang.NonNull;
import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.aql.containment.TemplateIdAqlTuple;
import org.ehrbase.aql.containment.TemplateIdQueryTuple;
import org.ehrbase.aql.sql.queryimpl.ItemInfo;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.validation.Validator;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * {@link Configuration} for EhCache using JCache.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
public class CacheConfiguration implements BeanClassLoaderAware {

    private final CacheProperties properties;

    private ClassLoader beanClassLoader;

    public CacheConfiguration(CacheProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CacheOptions cacheOptions() {
        var options = new CacheOptions();
        options.setPreBuildQueries(properties.isPreBuildQueries());
        options.setPreBuildQueriesDepth(properties.getPreBuildQueriesDepth());
        return options;
    }

    @Bean
    public CacheManager jCacheCacheManager() throws IOException {
        var cachingProvider = Caching.getCachingProvider();

        CacheManager cacheManager;
        if (properties.isEnabled() && properties.getConfig() != null) {
            cacheManager = cachingProvider.getCacheManager(properties.getConfig().getURI(), beanClassLoader);
        } else {
            cacheManager = cachingProvider.getCacheManager(null, beanClassLoader);
        }

        createCache(CacheOptions.INTROSPECT_CACHE, UUID.class, WebTemplate.class, cacheManager);
        createCache(CacheOptions.OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class, cacheManager);
        createCache(CacheOptions.VALIDATOR_CACHE, UUID.class, Validator.class, cacheManager);
        createCache(CacheOptions.QUERY_CACHE, TemplateIdQueryTuple.class, JsonPathQueryResult.class, cacheManager);
        createCache(CacheOptions.FIELDS_CACHE, TemplateIdAqlTuple.class, ItemInfo.class, cacheManager);
        createCache(CacheOptions.MULTI_VALUE_CACHE, String.class, List.class, cacheManager);

        return cacheManager;
    }

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    private <K, V> void createCache(String cacheName, Class<K> keyType, Class<V> valueType, CacheManager cacheManager) {
        MutableConfiguration<K, V> configuration = new MutableConfiguration<>();
        configuration.setTypes(keyType, valueType);
        configuration.setStoreByValue(false);
        if (!properties.isEnabled()) {
            configuration.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ZERO));
        }
        cacheManager.createCache(cacheName, configuration);
    }
}
