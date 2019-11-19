/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.configuration;

import org.ehrbase.opt.query.I_QueryOptMetaData;
import org.ehrbase.validation.Validator;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.net.URISyntaxException;
import java.util.UUID;

@Configuration
public class CacheConfiguration {

    public static final String INTROSPECT_CACHE = "introspectCache";
    public static final String OPERATIONAL_TEMPLATE_CACHE = "operationaltemplateCache";
    public static final String VALIDATOR_CACHE = "validatorCache";
    @Value("${cache.config}")
    private String configPath;
    @Value("${cache.enabled}")
    private boolean enabled;


    @Bean
    public CacheManager cacheManagerCustomizer() throws URISyntaxException {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        final CacheManager cacheManager;
        if (enabled) {
            cacheManager = cachingProvider.getCacheManager(getClass().getResource(configPath).toURI(),
                    getClass().getClassLoader());
        } else {
            cacheManager = cachingProvider.getCacheManager();
        }
        buildCache(INTROSPECT_CACHE, UUID.class, I_QueryOptMetaData.class, cacheManager, enabled);
        buildCache(OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class, cacheManager, enabled);
        buildCache(VALIDATOR_CACHE, UUID.class, Validator.class, cacheManager, enabled);
        return cacheManager;
    }


    public static <K, V> void buildCache(String cacheName, Class<K> keyClass, Class<V> valueClass, CacheManager cacheManager, boolean enabled) {
        MutableConfiguration<K, V> config
                = new MutableConfiguration<>();
        config.setTypes(keyClass, valueClass);
        config.setStoreByValue(false);
        //disable Cache
        if (!enabled) {
            config.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ZERO));
        }
        cacheManager.createCache(cacheName, config);
    }
}
