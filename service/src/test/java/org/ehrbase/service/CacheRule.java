/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.service;

import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.aql.containment.TemplateIdAqlTuple;
import org.ehrbase.aql.containment.TemplateIdQueryTuple;
import org.ehrbase.aql.sql.queryimpl.ItemInfo;
import org.ehrbase.cache.CacheOptions;
import org.ehrbase.validation.Validator;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.util.List;
import java.util.UUID;

public class CacheRule extends TestWatcher {
    public CacheManager cacheManager;

    @Override
    protected void starting(Description description) {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        cacheManager = cachingProvider.getCacheManager();
        buildCache(CacheOptions.INTROSPECT_CACHE, UUID.class, WebTemplate.class, cacheManager, true);
        buildCache(CacheOptions.OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class, cacheManager, true);
        buildCache(CacheOptions.VALIDATOR_CACHE, UUID.class, Validator.class, cacheManager, true);
        buildCache(CacheOptions.QUERY_CACHE, TemplateIdQueryTuple.class, JsonPathQueryResult.class, cacheManager, true);
        buildCache(CacheOptions.FIELDS_CACHE, TemplateIdAqlTuple.class, ItemInfo.class, cacheManager, true);
        buildCache(CacheOptions.MULTI_VALUE_CACHE, String.class, List.class, cacheManager, false);
    }

    public <K, V> void buildCache(String cacheName, Class<K> keyClass, Class<V> valueClass, CacheManager cacheManager, boolean enabled) {
        MutableConfiguration<K, V> config = new MutableConfiguration<>();
        config.setTypes(keyClass, valueClass);
        config.setStoreByValue(false);
        //disable Cache
        if (!enabled) {
            config.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ZERO));
        }

        cacheManager.createCache(cacheName, config);
    }

    @Override
    protected void finished(Description description) {
        cacheManager.close();
    }
}

