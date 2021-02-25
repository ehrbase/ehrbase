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

import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.aql.containment.TemplateIdAqlTuple;
import org.ehrbase.aql.containment.TemplateIdQueryTuple;
import org.ehrbase.aql.sql.queryimpl.ItemInfo;
import org.ehrbase.validation.Validator;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheConfiguration {

    public static final String INTROSPECT_CACHE = "introspectCache";
    public static final String OPERATIONAL_TEMPLATE_CACHE = "operationaltemplateCache";
    public static final String VALIDATOR_CACHE = "validatorCache";
    public static final String QUERY_CACHE = "queryCache";
    public static final String FIELDS_CACHE = "fieldsCache";
    public static final String MULTI_VALUE_CACHE = "multivaluedCache";


    private String configPath;
    private boolean enabled;
    private boolean preBuildQueries;
    private int preBuildQueriesDepth;

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPreBuildQueries() {
        return preBuildQueries;
    }

    public void setPreBuildQueries(boolean preBuildQueries) {
        this.preBuildQueries = preBuildQueries;
    }

    public int getPreBuildQueriesDepth() {
        return preBuildQueriesDepth;
    }

    public void setPreBuildQueriesDepth(int preBuildQueriesDepth) {
        this.preBuildQueriesDepth = preBuildQueriesDepth;
    }

    @Bean
    public static CacheManager cacheManagerCustomizer(CacheConfiguration cacheProperties) throws URISyntaxException {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        final CacheManager cacheManager;
        if (cacheProperties.isEnabled()) {
            cacheManager = cachingProvider.getCacheManager(CacheConfiguration.class.getResource(cacheProperties.getConfigPath()).toURI(),
                    CacheConfiguration.class.getClassLoader());
        } else {
            cacheManager = cachingProvider.getCacheManager();
        }
        buildCache(INTROSPECT_CACHE, UUID.class, WebTemplate.class, cacheManager, cacheProperties.isEnabled());
        buildCache(OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class, cacheManager, cacheProperties.isEnabled());
        buildCache(VALIDATOR_CACHE, UUID.class, Validator.class, cacheManager, cacheProperties.isEnabled());
        buildCache(QUERY_CACHE, TemplateIdQueryTuple.class, JsonPathQueryResult.class, cacheManager, cacheProperties.isEnabled());
        buildCache(FIELDS_CACHE, TemplateIdAqlTuple.class, ItemInfo.class, cacheManager, cacheProperties.isEnabled());
        buildCache(MULTI_VALUE_CACHE, String.class, List.class, cacheManager, cacheProperties.isEnabled());
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
